package com.solra.mon.interfaces.rest;

import com.solra.mon.domain.service.ReconciliationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对账结算 REST 控制器 — MON-007。
 *
 * <p>端点一览（管理后台）：
 * <ul>
 *   <li>GET  /api/v1/admin/reconciliation/daily?date=2026-06-30  — 日对账</li>
 *   <li>POST /api/v1/admin/reconciliation/settle-monthly             — 月结算</li>
 *   <li>GET  /api/v1/admin/reconciliation/brand/{brandId}            — 品牌结算</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    /** GET /api/v1/admin/reconciliation/daily?date=2026-06-30 */
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> dailyReconciliation(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().toString()}") String date) {
        ReconciliationService.DailyReconciliationReport report =
                reconciliationService.reconcileDaily(date);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", report.getDate());
        result.put("total_orders", report.getTotalOrders());
        result.put("matched_orders", report.getMatchedOrders());
        result.put("discrepancies", report.getDiscrepancies());
        result.put("error_rate", String.format("%.4f%%", report.getErrorRate()));
        result.put("within_tolerance", report.isWithinTolerance());

        List<Map<String, Object>> details = report.getDetails().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("order_id", d.getOrderId());
            m.put("type", d.getType().name());
            m.put("description", d.getDescription());
            m.put("order_amount", d.getOrderAmount());
            m.put("txn_amount", d.getTxnAmount());
            m.put("diff", d.getDiff());
            return m;
        }).collect(Collectors.toList());
        result.put("details", details);

        return ResponseEntity.ok(result);
    }

    /** POST /api/v1/admin/reconciliation/settle-monthly */
    @PostMapping("/settle-monthly")
    public ResponseEntity<Map<String, Object>> settleMonthly(
            @RequestBody(required = false) Map<String, String> request) {
        String period = request != null ? request.getOrDefault("period",
                java.time.YearMonth.now().toString()) : java.time.YearMonth.now().toString();

        ReconciliationService.MonthlySettlementReport report =
                reconciliationService.settleMonthly(period);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", report.getPeriod());
        result.put("settled_count", report.getSettledCount());
        result.put("total_sales", report.getTotalSales());
        result.put("total_commission", report.getTotalCommission());
        result.put("total_creator_share", report.getTotalCreatorShare());

        Map<String, Long> byCreator = new LinkedHashMap<>();
        report.getByCreator().forEach((k, v) -> byCreator.put(k, v));
        result.put("by_creator", byCreator);

        return ResponseEntity.ok(result);
    }

    /** GET /api/v1/admin/reconciliation/brand/{brandId}?period=2026-06 */
    @GetMapping("/brand/{brandId}")
    public ResponseEntity<Map<String, Object>> brandSettlement(
            @PathVariable String brandId,
            @RequestParam(defaultValue = "#{T(java.time.YearMonth).now().toString()}") String period) {
        ReconciliationService.BrandSettlementSummary summary =
                reconciliationService.getBrandSettlement(brandId, period);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("brand_id", summary.getBrandId());
        result.put("period", summary.getPeriod());
        result.put("annual_fee", summary.getAnnualFee());
        result.put("ad_revenue", summary.getAdRevenue());
        result.put("total_settlement", summary.getTotalSettlement());

        return ResponseEntity.ok(result);
    }
}
