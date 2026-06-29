package com.solra.common.cost;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CostReportTest {

    @Test
    void shouldCreateEmptyReport() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(86400);

        CostReport report = new CostReport("rpt-001",
                CostReport.ReportPeriod.DAILY, start, end);

        assertEquals("rpt-001", report.getReportId());
        assertEquals(CostReport.ReportPeriod.DAILY, report.getPeriod());
        assertEquals(0.0, report.getTotalCost());
        assertEquals(0.0, report.getTotalCostYuan());
        assertTrue(report.getCostByType().isEmpty());
        assertTrue(report.getCostByFunction().isEmpty());
    }

    @Test
    void shouldAggregateEntries() {
        CostReport report = new CostReport("rpt-002",
                CostReport.ReportPeriod.DAILY,
                Instant.now().minusSeconds(86400), Instant.now());

        // 添加3条成本记录
        CostEntry e1 = CostEntry.builder()
                .entryId("e1").resourceType(CostResourceType.COMPUTE)
                .resourceId("i-1").amount(10000)
                .dimensions(Map.of(CostDimension.FUNCTION, "avt-service",
                        CostDimension.USER, "user-a",
                        CostDimension.SPACE, "space-1"))
                .build();
        CostEntry e2 = CostEntry.builder()
                .entryId("e2").resourceType(CostResourceType.API)
                .resourceId("llm").amount(5000)
                .dimensions(Map.of(CostDimension.FUNCTION, "avt-service",
                        CostDimension.USER, "user-b"))
                .build();
        CostEntry e3 = CostEntry.builder()
                .entryId("e3").resourceType(CostResourceType.STORAGE)
                .resourceId("s3").amount(3000)
                .dimensions(Map.of(CostDimension.FUNCTION, "spc-service",
                        CostDimension.SPACE, "space-2"))
                .build();

        report.addEntry(e1);
        report.addEntry(e2);
        report.addEntry(e3);

        assertEquals(18000, report.getTotalCost());
        assertEquals(180.0, report.getTotalCostYuan());

        // 按资源类型验证
        Map<CostResourceType, Double> byType = report.getCostByType();
        assertEquals(3, byType.size());
        assertEquals(10000.0, byType.get(CostResourceType.COMPUTE));
        assertEquals(5000.0, byType.get(CostResourceType.API));
        assertEquals(3000.0, byType.get(CostResourceType.STORAGE));
    }

    @Test
    void shouldCalculateCostChangePercent() {
        CostReport report = new CostReport("rpt-003",
                CostReport.ReportPeriod.MONTHLY,
                Instant.now().minusSeconds(86400 * 30), Instant.now());

        report.setPreviousPeriodCost(80000); // 上期 ¥800
        // totalCost = 0, so change is -100%
        assertEquals(-100.0, report.getCostChangePercent(), 0.01);
    }

    @Test
    void shouldCalculatePositiveChange() {
        CostReport report = new CostReport("rpt-004",
                CostReport.ReportPeriod.MONTHLY,
                Instant.now().minusSeconds(86400 * 30), Instant.now());

        CostEntry e1 = CostEntry.builder()
                .entryId("e1").resourceType(CostResourceType.COMPUTE)
                .resourceId("i-1").amount(12000)
                .dimensions(Map.of(CostDimension.FUNCTION, "avt-service"))
                .build();
        report.addEntry(e1);
        report.setPreviousPeriodCost(10000); // 上期 ¥100, 本期 ¥120

        assertEquals(20.0, report.getCostChangePercent(), 0.01);
    }

    @Test
    void shouldGetTopCostTypes() {
        CostReport report = new CostReport("rpt-005",
                CostReport.ReportPeriod.WEEKLY,
                Instant.now().minusSeconds(86400 * 7), Instant.now());

        report.addEntry(CostEntry.builder().entryId("e1")
                .resourceType(CostResourceType.COMPUTE).resourceId("i-1")
                .amount(50000).build());
        report.addEntry(CostEntry.builder().entryId("e2")
                .resourceType(CostResourceType.API).resourceId("llm")
                .amount(30000).build());
        report.addEntry(CostEntry.builder().entryId("e3")
                .resourceType(CostResourceType.STORAGE).resourceId("s3")
                .amount(10000).build());

        var top = report.getTopCostTypes(2);
        assertEquals(2, top.size());
        assertEquals(CostResourceType.COMPUTE, top.get(0).getKey());
        assertEquals(50000.0, top.get(0).getValue());
        assertEquals(CostResourceType.API, top.get(1).getKey());
    }

    @Test
    void shouldGetTopFunctions() {
        CostReport report = new CostReport("rpt-006",
                CostReport.ReportPeriod.DAILY,
                Instant.now().minusSeconds(86400), Instant.now());

        report.addEntry(CostEntry.builder().entryId("e1")
                .resourceType(CostResourceType.COMPUTE).resourceId("i-1").amount(10000)
                .dimensions(Map.of(CostDimension.FUNCTION, "avt-service")).build());
        report.addEntry(CostEntry.builder().entryId("e2")
                .resourceType(CostResourceType.API).resourceId("llm").amount(8000)
                .dimensions(Map.of(CostDimension.FUNCTION, "avt-service")).build());
        report.addEntry(CostEntry.builder().entryId("e3")
                .resourceType(CostResourceType.STORAGE).resourceId("s3").amount(5000)
                .dimensions(Map.of(CostDimension.FUNCTION, "spc-service")).build());

        var top = report.getTopFunctions(2);
        assertEquals(2, top.size());
        assertEquals("avt-service", top.get(0).getKey());
        assertEquals(18000.0, top.get(0).getValue());
        assertEquals("spc-service", top.get(1).getKey());
        assertEquals(5000.0, top.get(1).getValue());
    }

    @Test
    void shouldAddRecommendations() {
        CostReport report = new CostReport("rpt-007",
                CostReport.ReportPeriod.MONTHLY,
                Instant.now().minusSeconds(86400 * 30), Instant.now());

        var rec = new CostReport.FinOpsRecommendation(
                "FIN-001", "Test", "Description", 100.0,
                CostReport.FinOpsRecommendation.Priority.HIGH, "spot_instance");
        report.addRecommendation(rec);

        assertEquals(1, report.getRecommendations().size());
        assertEquals("FIN-001", report.getRecommendations().get(0).id());
        assertEquals(100.0, report.getRecommendations().get(0).estimatedMonthlySavings());
    }

    @Test
    void shouldHandleEmptyReportTopQueries() {
        CostReport report = new CostReport("rpt-008",
                CostReport.ReportPeriod.DAILY,
                Instant.now().minusSeconds(86400), Instant.now());

        assertTrue(report.getTopCostTypes(10).isEmpty());
        assertTrue(report.getTopFunctions(10).isEmpty());
        assertTrue(report.getTopUsers(10).isEmpty());
        assertTrue(report.getTopSpaces(10).isEmpty());
    }

    @Test
    void shouldGetTopUsers() {
        CostReport report = new CostReport("rpt-009",
                CostReport.ReportPeriod.DAILY,
                Instant.now().minusSeconds(86400), Instant.now());

        report.addEntry(CostEntry.builder().entryId("e1")
                .resourceType(CostResourceType.API).resourceId("llm").amount(3000)
                .dimensions(Map.of(CostDimension.USER, "vip-user-1")).build());
        report.addEntry(CostEntry.builder().entryId("e2")
                .resourceType(CostResourceType.API).resourceId("llm").amount(1000)
                .dimensions(Map.of(CostDimension.USER, "normal-user")).build());

        var top = report.getTopUsers(5);
        assertEquals(2, top.size());
        assertEquals("vip-user-1", top.get(0).getKey());
        assertEquals(3000.0, top.get(0).getValue());
    }

    @Test
    void shouldGetTopSpaces() {
        CostReport report = new CostReport("rpt-010",
                CostReport.ReportPeriod.WEEKLY,
                Instant.now().minusSeconds(86400 * 7), Instant.now());

        report.addEntry(CostEntry.builder().entryId("e1")
                .resourceType(CostResourceType.COMPUTE).resourceId("gpu").amount(20000)
                .dimensions(Map.of(CostDimension.SPACE, "popular-space")).build());
        report.addEntry(CostEntry.builder().entryId("e2")
                .resourceType(CostResourceType.COMPUTE).resourceId("gpu").amount(5000)
                .dimensions(Map.of(CostDimension.SPACE, "normal-space")).build());

        var top = report.getTopSpaces(3);
        assertEquals(2, top.size());
        assertEquals("popular-space", top.get(0).getKey());
        assertEquals(20000.0, top.get(0).getValue());
    }
}
