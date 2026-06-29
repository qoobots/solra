package com.solra.mon.domain.entity;

import java.util.UUID;

/**
 * 创作者收益领域实体 — MON-003。
 *
 * <p>业务规则：
 * <ul>
 *   <li>平台抽成 30%，创作者分得 70%</li>
 *   <li>月结：每月1日结算上月收益</li>
 *   <li>最低提现 ¥100（10000分）</li>
 * </ul>
 */
public class CreatorEarning {

    public enum EarningStatus {
        PENDING,      // 待结算
        SETTLED,      // 已结算
        WITHDRAWN,    // 已提现
        CANCELLED     // 已取消
    }

    public static final double PLATFORM_COMMISSION_RATE = 0.30;
    public static final long MIN_WITHDRAWAL_AMOUNT = 10000L; // ¥100 = 10000分

    private String earningId;
    private String creatorId;
    private String itemId;
    private String orderId;
    private long saleAmount;         // 售价（分）
    private long commission;         // 平台抽成（分）
    private long creatorShare;       // 创作者分成（分）
    private EarningStatus status;
    private String settlementPeriod; // 结算周期，如 "2026-06"
    private long createdAt;
    private long settledAt;
    private long withdrawnAt;

    public CreatorEarning(String creatorId, String itemId, String orderId, long saleAmount) {
        this.earningId = "ERN-" + UUID.randomUUID().toString().substring(0, 8);
        this.creatorId = creatorId;
        this.itemId = itemId;
        this.orderId = orderId;
        this.saleAmount = saleAmount;
        this.commission = (long) (saleAmount * PLATFORM_COMMISSION_RATE);
        this.creatorShare = saleAmount - commission;
        this.status = EarningStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    /** 结算收益。 */
    public void settle(String period) {
        if (status != EarningStatus.PENDING) {
            throw new IllegalStateException("Only pending earnings can be settled");
        }
        this.status = EarningStatus.SETTLED;
        this.settlementPeriod = period;
        this.settledAt = System.currentTimeMillis();
    }

    /** 提现。 */
    public void withdraw() {
        if (status != EarningStatus.SETTLED) {
            throw new IllegalStateException("Only settled earnings can be withdrawn");
        }
        if (creatorShare < MIN_WITHDRAWAL_AMOUNT) {
            throw new IllegalStateException(
                    "Minimum withdrawal is ¥100 (10000分), current: " + creatorShare + "分");
        }
        this.status = EarningStatus.WITHDRAWN;
        this.withdrawnAt = System.currentTimeMillis();
    }

    /** 取消收益记录。 */
    public void cancel() {
        if (status != EarningStatus.PENDING) {
            throw new IllegalStateException("Only pending earnings can be cancelled");
        }
        this.status = EarningStatus.CANCELLED;
    }

    // Getters
    public String getEarningId() { return earningId; }
    public String getCreatorId() { return creatorId; }
    public String getItemId() { return itemId; }
    public String getOrderId() { return orderId; }
    public long getSaleAmount() { return saleAmount; }
    public long getCommission() { return commission; }
    public long getCreatorShare() { return creatorShare; }
    public EarningStatus getStatus() { return status; }
    public String getSettlementPeriod() { return settlementPeriod; }
    public long getCreatedAt() { return createdAt; }
    public long getSettledAt() { return settledAt; }
    public long getWithdrawnAt() { return withdrawnAt; }
}
