package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.CreatorEarning;
import com.solra.mon.domain.entity.VirtualItem;
import com.solra.mon.domain.repository.VirtualItemRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 创作者交易市场领域服务 — MON-003。
 *
 * <p>职责：
 * <ul>
 *   <li>创作者商品上架/下架</li>
 *   <li>收益记录：每笔销售自动计算平台抽成（30%）</li>
 *   <li>月结统计：按创作者汇总待结算收益</li>
 *   <li>提现申请：最低 ¥100</li>
 * </ul>
 */
public class CreatorMarketplaceService {

    private final VirtualItemRepository itemRepository;
    private final Map<String, List<CreatorEarning>> earningsByCreator = new ConcurrentHashMap<>();
    private final Map<String, CreatorEarning> earningsById = new ConcurrentHashMap<>();

    public CreatorMarketplaceService(VirtualItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    /** 创作者上架商品。 */
    public VirtualItem publishCreatorItem(String creatorId, VirtualItem item) {
        item.setCreatorId(creatorId);
        item.publish();
        return itemRepository.save(item);
    }

    /** 创作者下架商品。 */
    public VirtualItem suspendCreatorItem(String creatorId, String itemId) {
        VirtualItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        if (!creatorId.equals(item.getCreatorId())) {
            throw new IllegalStateException("Only the creator can suspend their item");
        }
        item.suspend();
        return itemRepository.save(item);
    }

    /** 获取创作者的所有商品。 */
    public List<VirtualItem> getCreatorItems(String creatorId) {
        return itemRepository.findByType(null, 0, Integer.MAX_VALUE).stream()
                .filter(i -> creatorId.equals(i.getCreatorId()))
                .collect(Collectors.toList());
    }

    /** 记录创作者收益（购买时调用）。 */
    public CreatorEarning recordEarning(String creatorId, String itemId,
                                         String orderId, long saleAmount) {
        CreatorEarning earning = new CreatorEarning(creatorId, itemId, orderId, saleAmount);
        earningsById.put(earning.getEarningId(), earning);
        earningsByCreator.computeIfAbsent(creatorId, k -> new ArrayList<>()).add(earning);
        return earning;
    }

    /** 获取创作者收益列表。 */
    public List<CreatorEarning> getCreatorEarnings(String creatorId) {
        return earningsByCreator.getOrDefault(creatorId, List.of());
    }

    /** 获取创作者待结算总金额。 */
    public long getPendingBalance(String creatorId) {
        return earningsByCreator.getOrDefault(creatorId, List.of()).stream()
                .filter(e -> e.getStatus() == CreatorEarning.EarningStatus.PENDING
                        || e.getStatus() == CreatorEarning.EarningStatus.SETTLED)
                .mapToLong(CreatorEarning::getCreatorShare)
                .sum();
    }

    /** 月结：将指定月份所有 PENDING 收益结算。 */
    public List<CreatorEarning> settleMonthly(String period) {
        List<CreatorEarning> settled = new ArrayList<>();
        for (CreatorEarning earning : earningsById.values()) {
            if (earning.getStatus() == CreatorEarning.EarningStatus.PENDING) {
                earning.settle(period);
                settled.add(earning);
            }
        }
        return settled;
    }

    /** 创作者提现（需满足最低 ¥100）。 */
    public CreatorEarning withdraw(String earningId) {
        CreatorEarning earning = earningsById.get(earningId);
        if (earning == null) {
            throw new IllegalArgumentException("Earning not found: " + earningId);
        }
        if (earning.getCreatorShare() < CreatorEarning.MIN_WITHDRAWAL_AMOUNT) {
            throw new IllegalStateException(String.format(
                    "Minimum withdrawal is ¥100 (10000分), current balance: %d分",
                    earning.getCreatorShare()));
        }
        earning.withdraw();
        return earning;
    }

    /** 获取所有创作者收益汇总。 */
    public Map<String, CreatorBalanceSummary> getAllCreatorBalances() {
        Map<String, CreatorBalanceSummary> result = new LinkedHashMap<>();
        for (var entry : earningsByCreator.entrySet()) {
            String creatorId = entry.getKey();
            long pending = entry.getValue().stream()
                    .filter(e -> e.getStatus() == CreatorEarning.EarningStatus.PENDING)
                    .mapToLong(CreatorEarning::getCreatorShare).sum();
            long settled = entry.getValue().stream()
                    .filter(e -> e.getStatus() == CreatorEarning.EarningStatus.SETTLED)
                    .mapToLong(CreatorEarning::getCreatorShare).sum();
            long withdrawn = entry.getValue().stream()
                    .filter(e -> e.getStatus() == CreatorEarning.EarningStatus.WITHDRAWN)
                    .mapToLong(CreatorEarning::getCreatorShare).sum();
            long totalCommission = entry.getValue().stream()
                    .mapToLong(CreatorEarning::getCommission).sum();

            result.put(creatorId, new CreatorBalanceSummary(
                    creatorId, pending, settled, withdrawn, totalCommission));
        }
        return result;
    }

    // ── 值对象 ──

    public static class CreatorBalanceSummary {
        private final String creatorId;
        private final long pendingBalance;
        private final long settledBalance;
        private final long withdrawnTotal;
        private final long totalCommission;

        public CreatorBalanceSummary(String creatorId, long pendingBalance, long settledBalance,
                                      long withdrawnTotal, long totalCommission) {
            this.creatorId = creatorId;
            this.pendingBalance = pendingBalance;
            this.settledBalance = settledBalance;
            this.withdrawnTotal = withdrawnTotal;
            this.totalCommission = totalCommission;
        }

        public String getCreatorId() { return creatorId; }
        public long getPendingBalance() { return pendingBalance; }
        public long getSettledBalance() { return settledBalance; }
        public long getWithdrawnTotal() { return withdrawnTotal; }
        public long getTotalCommission() { return totalCommission; }
        /** 可提现总额。 */
        public long getAvailableBalance() { return pendingBalance + settledBalance; }
    }
}
