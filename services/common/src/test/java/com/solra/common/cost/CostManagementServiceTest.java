package com.solra.common.cost;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostManagementServiceTest {

    private CostManagementService service;

    @BeforeEach
    void setUp() {
        service = new CostManagementService();
    }

    // ---- 成本记录 ----

    @Test
    void shouldRecordCost() {
        CostEntry entry = service.recordCost("e1", CostResourceType.COMPUTE,
                "i-123", 10000.0,
                Map.of(CostDimension.FUNCTION, "avt-service",
                        CostDimension.USER, "user-1"),
                "GPU instance");

        assertEquals("e1", entry.getEntryId());
        assertEquals(CostResourceType.COMPUTE, entry.getResourceType());
        assertEquals(10000, entry.getAmount());
        assertEquals(1, service.getCostEntryCount());
    }

    @Test
    void shouldRecordCostBatch() {
        List<CostEntry> entries = List.of(
                CostEntry.builder().entryId("e1").resourceType(CostResourceType.COMPUTE)
                        .resourceId("i-1").amount(1000).build(),
                CostEntry.builder().entryId("e2").resourceType(CostResourceType.API)
                        .resourceId("llm").amount(500).build(),
                CostEntry.builder().entryId("e3").resourceType(CostResourceType.STORAGE)
                        .resourceId("s3").amount(300).build());

        int count = service.recordCostBatch(entries);
        assertEquals(3, count);
        assertEquals(3, service.getCostEntryCount());
    }

    // ---- 成本查询 ----

    @Test
    void shouldQueryCostsByTimeRange() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant twoHoursAgo = now.minusSeconds(7200);

        // 这条在范围内
        recordCost("e1", CostResourceType.COMPUTE, "i-1", 1000,
                Map.of(), "", oneHourAgo);
        // 这条不在范围内
        recordCost("e2", CostResourceType.API, "llm", 500,
                Map.of(), "", twoHoursAgo);

        List<CostEntry> result = service.queryCosts(oneHourAgo.minusSeconds(1), now);
        assertEquals(1, result.size());
        assertEquals("e1", result.get(0).getEntryId());
    }

    @Test
    void shouldGetCostByDimension() {
        Instant now = Instant.now();
        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 10000.0,
                Map.of(CostDimension.FUNCTION, "avt-service"), "");
        service.recordCost("e2", CostResourceType.API, "llm", 5000.0,
                Map.of(CostDimension.FUNCTION, "avt-service"), "");
        service.recordCost("e3", CostResourceType.STORAGE, "s3", 3000.0,
                Map.of(CostDimension.FUNCTION, "spc-service"), "");

        Map<String, Double> result = service.getCostByDimension(
                CostDimension.FUNCTION,
                now.minusSeconds(1), now.plusSeconds(1));

        assertEquals(2, result.size());
        assertEquals(15000.0, result.get("avt-service"));
        assertEquals(3000.0, result.get("spc-service"));
    }

    @Test
    void shouldGetCostByResourceType() {
        Instant now = Instant.now();
        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 20000.0, Map.of(), "");
        service.recordCost("e2", CostResourceType.API, "llm", 8000.0, Map.of(), "");
        service.recordCost("e3", CostResourceType.COMPUTE, "i-2", 10000.0, Map.of(), "");

        Map<CostResourceType, Double> result = service.getCostByResourceType(
                now.minusSeconds(1), now.plusSeconds(1));

        assertEquals(2, result.size());
        assertEquals(30000.0, result.get(CostResourceType.COMPUTE));
        assertEquals(8000.0, result.get(CostResourceType.API));
    }

    @Test
    void shouldGetTotalCost() {
        Instant now = Instant.now();
        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 10000.0, Map.of(), "");
        service.recordCost("e2", CostResourceType.API, "llm", 5000.0, Map.of(), "");

        double total = service.getTotalCost(now.minusSeconds(1), now.plusSeconds(1));
        assertEquals(15000.0, total);
    }

    // ---- 预算管理 ----

    @Test
    void shouldInitializeDefaultBudgets() {
        var budgets = service.getAllBudgets();
        assertEquals(7, budgets.size());
        assertTrue(service.getBudget("solra-monthly-total").isPresent());
        assertTrue(service.getBudget("solra-compute-budget").isPresent());
    }

    @Test
    void shouldRegisterAndRemoveBudget() {
        Budget budget = new Budget("custom-budget", 5000, "CNY",
                CostResourceType.NETWORK,
                List.of(50, 75, 100), List.of("email"));

        service.registerBudget(budget);
        assertTrue(service.getBudget("custom-budget").isPresent());

        service.removeBudget("custom-budget");
        assertFalse(service.getBudget("custom-budget").isPresent());
    }

    // ---- 预算告警 ----

    @Test
    void shouldCheckBudgetAlertsWhenUnderBudget() {
        Instant now = Instant.now();
        // 仅记录少量成本，不应触发告警
        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 50000.0, Map.of(), ""); // ¥500

        List<BudgetAlert> alerts = service.checkBudgetAlerts(
                now.minusSeconds(1), now.plusSeconds(1));

        // 所有预算都远低于阈值，不应有告警
        assertTrue(alerts.isEmpty());
    }

    @Test
    void shouldCheckBudgetAlertsWhenOverThreshold() {
        Instant now = Instant.now();
        // 记录超过月预算50%的成本
        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 1200000.0, Map.of(), ""); // ¥12,000 → 60%

        List<BudgetAlert> alerts = service.checkBudgetAlerts(
                now.minusSeconds(1), now.plusSeconds(1));

        assertFalse(alerts.isEmpty());
        // 应该至少触发计算预算的50%告警
        assertTrue(alerts.stream().anyMatch(a -> a.getBudgetName().contains("compute")));
    }

    // ---- 成本报告 ----

    @Test
    void shouldGenerateDailyReport() {
        Instant now = Instant.now();
        Instant dayStart = now.minusSeconds(3600); // 1小时前作为日初

        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 100000.0,
                Map.of(CostDimension.FUNCTION, "avt-service"), "");
        service.recordCost("e2", CostResourceType.API, "llm", 50000.0,
                Map.of(CostDimension.FUNCTION, "avt-service"), "");

        CostReport report = service.generateReport(CostReport.ReportPeriod.DAILY, dayStart);

        assertEquals(1500.0, report.getTotalCostYuan(), 0.01);
        assertNotNull(report.getReportId());
        assertEquals(CostReport.ReportPeriod.DAILY, report.getPeriod());
    }

    @Test
    void shouldGenerateMonthlyReportWithRecommendations() {
        Instant now = Instant.now();
        Instant monthStart = now.minusSeconds(3600);

        // 模拟高计算成本（触发Spot实例建议）
        service.recordCost("e1", CostResourceType.COMPUTE, "i-1", 4000000.0, // ¥40,000 (>60%)
                Map.of(CostDimension.FUNCTION, "avt-service"), "");

        CostReport report = service.generateReport(CostReport.ReportPeriod.MONTHLY, monthStart);

        assertFalse(report.getRecommendations().isEmpty());
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.id().equals("FIN-001"))); // Spot实例建议
        assertTrue(report.getRecommendations().stream()
                .anyMatch(r -> r.id().equals("FIN-006"))); // 预算上限警告
    }

    @Test
    void shouldCacheAndRetrieveReport() {
        Instant dayStart = Instant.now().minusSeconds(3600);
        CostReport report = service.generateReport(CostReport.ReportPeriod.DAILY, dayStart);

        var cached = service.getCachedReport(report.getReportId());
        assertTrue(cached.isPresent());
        assertEquals(report.getReportId(), cached.get().getReportId());
    }

    @Test
    void shouldReturnEmptyForMissingReport() {
        assertTrue(service.getCachedReport("non-existent").isEmpty());
    }

    @Test
    void shouldPurgeOldCostEntries() {
        Instant now = Instant.now();
        Instant oneDayAgo = now.minusSeconds(86400);
        Instant twoDaysAgo = now.minusSeconds(172800);

        recordCost("e1", CostResourceType.COMPUTE, "i-1", 1000.0, Map.of(), "", oneDayAgo);
        recordCost("e2", CostResourceType.API, "llm", 500.0, Map.of(), "", twoDaysAgo);

        assertEquals(2, service.getCostEntryCount());

        int removed = service.purgeCostsBefore(now.minusSeconds(86400 + 3600));
        assertEquals(1, removed);
        assertEquals(1, service.getCostEntryCount());
    }

    // ========== 辅助方法 ==========

    private void recordCost(String entryId, CostResourceType type, String resourceId,
                            double amount, Map<CostDimension, String> dims,
                            String description, Instant timestamp) {
        CostEntry entry = CostEntry.builder()
                .entryId(entryId)
                .resourceType(type)
                .resourceId(resourceId)
                .amount(amount)
                .dimensions(dims != null ? dims : Map.of())
                .description(description != null ? description : "")
                .timestamp(timestamp)
                .build();
        service.recordCostBatch(List.of(entry));
    }
}
