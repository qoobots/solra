package com.solra.mon.interfaces.rest;

import com.solra.mon.application.dto.VirtualItemDTO;
import com.solra.mon.domain.entity.CreatorEarning;
import com.solra.mon.domain.entity.VirtualItem;
import com.solra.mon.domain.service.CreatorMarketplaceService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 创作者交易市场 REST 控制器 — MON-003。
 *
 * <p>端点一览：
 * <ul>
 *   <li>GET  /api/v1/creator/{creatorId}/items          — 创作者的物品列表</li>
 *   <li>POST /api/v1/creator/{creatorId}/items           — 上架商品</li>
 *   <li>POST /api/v1/creator/{creatorId}/items/{itemId}/suspend — 下架商品</li>
 *   <li>GET  /api/v1/creator/{creatorId}/earnings        — 创作者收益</li>
 *   <li>GET  /api/v1/creator/{creatorId}/balance          — 创作者余额</li>
 *   <li>POST /api/v1/creator/earnings/{earningId}/withdraw — 提现</li>
 *   <li>POST /api/v1/admin/creator/settle                 — 月结（管理员）</li>
 *   <li>GET  /api/v1/admin/creator/balances               — 全部创作者余额（管理员）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/creator")
public class CreatorMarketplaceController {

    private final CreatorMarketplaceService marketplaceService;

    public CreatorMarketplaceController(CreatorMarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    /** GET /api/v1/creator/{creatorId}/items */
    @GetMapping("/{creatorId}/items")
    public ResponseEntity<List<VirtualItemDTO>> getCreatorItems(@PathVariable String creatorId) {
        List<VirtualItem> items = marketplaceService.getCreatorItems(creatorId);
        return ResponseEntity.ok(items.stream().map(VirtualItemDTO::from).collect(Collectors.toList()));
    }

    /** POST /api/v1/creator/{creatorId}/items */
    @PostMapping("/{creatorId}/items")
    public ResponseEntity<VirtualItemDTO> publishItem(
            @PathVariable String creatorId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String type = (String) request.get("type");
        long price = ((Number) request.get("price")).longValue();
        String description = (String) request.getOrDefault("description", "");

        VirtualItem item = new VirtualItem(
                "CR-" + UUID.randomUUID().toString().substring(0, 8),
                name,
                VirtualItem.VirtualItemType.valueOf(type),
                price,
                com.solra.mon.domain.entity.VirtualWallet.CurrencyType.DIAMOND);
        item.setDescription(description);

        VirtualItem published = marketplaceService.publishCreatorItem(creatorId, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(VirtualItemDTO.from(published));
    }

    /** POST /api/v1/creator/{creatorId}/items/{itemId}/suspend */
    @PostMapping("/{creatorId}/items/{itemId}/suspend")
    public ResponseEntity<Map<String, String>> suspendItem(
            @PathVariable String creatorId,
            @PathVariable String itemId) {
        marketplaceService.suspendCreatorItem(creatorId, itemId);
        return ResponseEntity.ok(Map.of("status", "suspended", "item_id", itemId));
    }

    /** GET /api/v1/creator/{creatorId}/earnings */
    @GetMapping("/{creatorId}/earnings")
    public ResponseEntity<List<Map<String, Object>>> getEarnings(@PathVariable String creatorId) {
        List<CreatorEarning> earnings = marketplaceService.getCreatorEarnings(creatorId);
        List<Map<String, Object>> result = earnings.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("earning_id", e.getEarningId());
            m.put("item_id", e.getItemId());
            m.put("order_id", e.getOrderId());
            m.put("sale_amount", e.getSaleAmount());
            m.put("commission", e.getCommission());
            m.put("creator_share", e.getCreatorShare());
            m.put("status", e.getStatus().name());
            m.put("settlement_period", e.getSettlementPeriod());
            m.put("created_at", e.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /api/v1/creator/{creatorId}/balance */
    @GetMapping("/{creatorId}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String creatorId) {
        long balance = marketplaceService.getPendingBalance(creatorId);
        boolean canWithdraw = balance >= CreatorEarning.MIN_WITHDRAWAL_AMOUNT;
        return ResponseEntity.ok(Map.of(
                "creator_id", creatorId,
                "pending_balance", balance,
                "can_withdraw", canWithdraw,
                "min_withdrawal", CreatorEarning.MIN_WITHDRAWAL_AMOUNT
        ));
    }

    /** POST /api/v1/creator/earnings/{earningId}/withdraw */
    @PostMapping("/earnings/{earningId}/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@PathVariable String earningId) {
        CreatorEarning earning = marketplaceService.withdraw(earningId);
        return ResponseEntity.ok(Map.of(
                "earning_id", earning.getEarningId(),
                "status", earning.getStatus().name(),
                "amount", earning.getCreatorShare()
        ));
    }

    /** POST /api/v1/admin/creator/settle — 管理员月结 */
    @PostMapping("/admin/creator/settle")
    public ResponseEntity<Map<String, Object>> adminSettle(@RequestBody Map<String, String> request) {
        String period = request.getOrDefault("period",
                java.time.YearMonth.now().toString());
        List<CreatorEarning> settled = marketplaceService.settleMonthly(period);
        return ResponseEntity.ok(Map.of(
                "period", period,
                "settled_count", settled.size(),
                "total_amount", settled.stream().mapToLong(CreatorEarning::getCreatorShare).sum()
        ));
    }

    /** GET /api/v1/admin/creator/balances — 管理员查看全部余额 */
    @GetMapping("/admin/creator/balances")
    public ResponseEntity<List<Map<String, Object>>> adminBalances() {
        var balances = marketplaceService.getAllCreatorBalances();
        List<Map<String, Object>> result = balances.values().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("creator_id", b.getCreatorId());
            m.put("pending_balance", b.getPendingBalance());
            m.put("settled_balance", b.getSettledBalance());
            m.put("withdrawn_total", b.getWithdrawnTotal());
            m.put("available_balance", b.getAvailableBalance());
            m.put("total_commission", b.getTotalCommission());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
