package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.InteractionEvent;
import com.solra.crt.domain.entity.VisitRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpaceAnalytics 单元测试 (CRT-004)。
 */
@DisplayName("SpaceAnalytics 空间数据分析")
class SpaceAnalyticsTest {

    private SpaceAnalytics analytics;
    private static final String PROJECT_ID = "proj-001";
    private static final String SPACE_ID = "space-001";

    @BeforeEach
    void setUp() {
        analytics = new SpaceAnalytics();
        seedTestData();
    }

    private void seedTestData() {
        // 模拟10个访问记录
        String[] sources = {"direct", "share", "recommend", "search", "direct"};
        String[] devices = {"desktop", "mobile", "mobile", "desktop", "tablet"};
        String[] regions = {"CN", "CN", "US", "JP", "CN"};

        for (int i = 0; i < 10; i++) {
            String visitId = "visit-" + i;
            String visitorId = "user-" + (i % 5);
            String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);

            VisitRecord visit = analytics.recordVisitStart(
                    visitId, SPACE_ID, PROJECT_ID, visitorId, sessionId,
                    sources[i % sources.length], devices[i % devices.length],
                    regions[i % regions.length]);

            // 模拟停留一段时间
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            analytics.recordVisitEnd(visitId);

            // 模拟交互事件
            for (int j = 0; j < 3; j++) {
                String eventId = "event-" + i + "-" + j;
                InteractionEvent.EventType type = j == 0 ?
                        InteractionEvent.EventType.CLICK :
                        InteractionEvent.EventType.HOVER;
                analytics.recordInteraction(
                        eventId, visitId, SPACE_ID, PROJECT_ID, visitorId,
                        type,
                        "node-" + (j % 3),
                        i * 1.0f, j * 2.0f, 0.5f);
            }
        }
    }

    @Test
    @DisplayName("应生成仪表盘数据")
    void shouldGenerateDashboard() {
        Map<String, Object> dashboard = analytics.getDashboard(PROJECT_ID, "week");

        assertNotNull(dashboard);
        assertEquals(PROJECT_ID, dashboard.get("projectId"));

        @SuppressWarnings("unchecked")
        Map<String, Object> visitorStats = (Map<String, Object>) dashboard.get("visitorStats");
        assertNotNull(visitorStats);
        assertEquals(10L, visitorStats.get("totalVisits"));
        assertEquals(5L, visitorStats.get("uniqueVisitors"));

        assertNotNull(dashboard.get("durationDistribution"));
        assertNotNull(dashboard.get("interactionStats"));
        assertNotNull(dashboard.get("sourceAnalysis"));
        assertNotNull(dashboard.get("deviceAnalysis"));
        assertNotNull(dashboard.get("regionDistribution"));
    }

    @Test
    @DisplayName("应生成互动热力图")
    void shouldGenerateHeatmap() {
        Map<String, Object> heatmap = analytics.generateHeatmap(PROJECT_ID, "week");

        assertNotNull(heatmap);
        assertEquals(PROJECT_ID, heatmap.get("projectId"));
        assertTrue((int) heatmap.get("totalEvents") >= 30);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> heatPoints = (List<Map<String, Object>>) heatmap.get("heatPoints");
        assertNotNull(heatPoints);
        assertFalse(heatPoints.isEmpty());
        // 每个热力点应有位置和强度
        for (Map<String, Object> point : heatPoints) {
            assertNotNull(point.get("x"));
            assertNotNull(point.get("y"));
            assertNotNull(point.get("z"));
            assertTrue((int) point.get("intensity") > 0);
        }

        @SuppressWarnings("unchecked")
        Map<String, Long> byNode = (Map<String, Long>) heatmap.get("byNode");
        assertNotNull(byNode);
        assertFalse(byNode.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Long> byType = (Map<String, Long>) heatmap.get("byType");
        assertNotNull(byType);
        assertTrue(byType.containsKey("CLICK"));
        assertTrue(byType.containsKey("HOVER"));
    }

    @Test
    @DisplayName("应生成每日趋势")
    void shouldGenerateDailyTrend() {
        Map<String, Object> trend = analytics.getDailyTrend(PROJECT_ID, 7);

        assertNotNull(trend);
        assertEquals(PROJECT_ID, trend.get("projectId"));
        assertEquals(7, trend.get("days"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trendData = (List<Map<String, Object>>) trend.get("trend");
        assertNotNull(trendData);
        assertEquals(7, trendData.size());
    }

    @Test
    @DisplayName("应生成跳出率分析")
    void shouldGenerateBounceRateAnalysis() {
        Map<String, Object> bounce = analytics.getBounceRateAnalysis(PROJECT_ID, "week");

        assertNotNull(bounce);
        assertNotNull(bounce.get("bounceRate"));
        assertNotNull(bounce.get("bounceBySource"));
    }

    @Test
    @DisplayName("应生成项目排行")
    void shouldGenerateProjectRanking() {
        List<Map<String, Object>> ranking = analytics.getProjectRanking("totalVisits", 10);

        assertNotNull(ranking);
        assertFalse(ranking.isEmpty());
        assertTrue(ranking.size() <= 10);

        Map<String, Object> first = ranking.get(0);
        assertEquals(PROJECT_ID, first.get("projectId"));
        assertEquals(10L, first.get("totalVisits"));
    }

    @Test
    @DisplayName("visitRecord 应正确记录访问生命周期")
    void visitRecordShouldTrackLifecycle() {
        String visitId = "test-visit";
        VisitRecord visit = analytics.recordVisitStart(
                visitId, SPACE_ID, PROJECT_ID, "test-user", "test-session",
                "direct", "desktop", "CN");

        assertNotNull(visit);
        assertEquals("test-user", visit.getVisitorId());
        assertNull(visit.getLeftAt());

        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        VisitRecord ended = analytics.recordVisitEnd(visitId);

        assertNotNull(ended.getLeftAt());
        assertTrue(ended.getDurationMs() >= 10);
    }

    @Test
    @DisplayName("interactionEvent 应正确记录交互")
    void interactionEventShouldRecordCorrectly() {
        String eventId = "test-event";
        InteractionEvent event = analytics.recordInteraction(
                eventId, "visit-0", SPACE_ID, PROJECT_ID, "test-user",
                InteractionEvent.EventType.CLICK,
                "node-counter", 1.5f, 2.0f, 0.0f);

        assertNotNull(event);
        assertEquals("test-event", event.getEventId());
        assertEquals("node-counter", event.getTargetNodeId());
        assertEquals(1.5f, event.getPositionX());
        assertEquals(2.0f, event.getPositionY());
    }

    @Test
    @DisplayName("不同时间段的数据应被正确筛选")
    void differentPeriodsShouldBeFilteredCorrectly() {
        Map<String, Object> daily = analytics.getDashboard(PROJECT_ID, "day");
        Map<String, Object> monthly = analytics.getDashboard(PROJECT_ID, "month");

        assertNotNull(daily);
        assertNotNull(monthly);
        // 日/周/月报告都应包含相同结构
        assertEquals(daily.keySet(), monthly.keySet());
    }
}
