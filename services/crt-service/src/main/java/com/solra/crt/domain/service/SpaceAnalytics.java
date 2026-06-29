package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.InteractionEvent;
import com.solra.crt.domain.entity.VisitRecord;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 空间数据分析面板领域服务 (CRT-004)。
 * 提供访客数、停留分布、互动热力图等 T+1 数据分析。
 *
 * 验收标准：
 * - 访客数/停留分布/互动热力图
 * - T+1 更新
 * - 按时间维度（日/周/月）聚合
 */
public class SpaceAnalytics {

    // 访问记录
    private final Map<String, VisitRecord> visits = new ConcurrentHashMap<>();
    // 交互事件
    private final Map<String, InteractionEvent> interactions = new ConcurrentHashMap<>();

    /**
     * 记录访问开始。
     */
    public VisitRecord recordVisitStart(String visitId, String spaceId, String projectId,
                                         String visitorId, String sessionId,
                                         String entrySource, String deviceType, String region) {
        VisitRecord visit = new VisitRecord();
        visit.setVisitId(visitId);
        visit.setSpaceId(spaceId);
        visit.setProjectId(projectId);
        visit.setVisitorId(visitorId);
        visit.setSessionId(sessionId);
        visit.setEntrySource(entrySource);
        visit.setDeviceType(deviceType);
        visit.setRegion(region);
        visit.setEnteredAt(Instant.now());

        visits.put(visitId, visit);
        return visit;
    }

    /**
     * 记录访问结束。
     */
    public VisitRecord recordVisitEnd(String visitId) {
        VisitRecord visit = visits.get(visitId);
        if (visit == null) throw new IllegalArgumentException("Visit not found: " + visitId);
        visit.markExit(Instant.now());
        return visit;
    }

    /**
     * 记录交互事件。
     */
    public InteractionEvent recordInteraction(String eventId, String visitId, String spaceId,
                                                String projectId, String visitorId,
                                                InteractionEvent.EventType eventType,
                                                String targetNodeId,
                                                float posX, float posY, float posZ) {
        InteractionEvent event = new InteractionEvent();
        event.setEventId(eventId);
        event.setVisitId(visitId);
        event.setSpaceId(spaceId);
        event.setProjectId(projectId);
        event.setVisitorId(visitorId);
        event.setEventType(eventType);
        event.setTargetNodeId(targetNodeId);
        event.setPositionX(posX);
        event.setPositionY(posY);
        event.setPositionZ(posZ);
        event.setTimestamp(Instant.now());

        interactions.put(eventId, event);
        return event;
    }

    // ── 数据分析查询 ──

    /**
     * 获取项目概览仪表盘数据。
     */
    public Map<String, Object> getDashboard(String projectId, String period) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("projectId", projectId);
        dashboard.put("period", period);
        dashboard.put("generatedAt", Instant.now());

        // 计算时间窗口
        Instant since = getPeriodStart(period);

        // 过滤数据
        List<VisitRecord> filteredVisits = visits.values().stream()
                .filter(v -> projectId.equals(v.getProjectId()) && v.getEnteredAt().isAfter(since))
                .toList();
        List<InteractionEvent> filteredInteractions = interactions.values().stream()
                .filter(e -> projectId.equals(e.getProjectId()) && e.getTimestamp().isAfter(since))
                .toList();

        // 访客统计
        dashboard.put("visitorStats", getVisitorStats(filteredVisits));
        // 停留分布
        dashboard.put("durationDistribution", getDurationDistribution(filteredVisits));
        // 互动统计
        dashboard.put("interactionStats", getInteractionStats(filteredInteractions));
        // 来源分析
        dashboard.put("sourceAnalysis", getSourceAnalysis(filteredVisits));
        // 设备分析
        dashboard.put("deviceAnalysis", getDeviceAnalysis(filteredVisits));
        // 地域分布
        dashboard.put("regionDistribution", getRegionDistribution(filteredVisits));

        return dashboard;
    }

    /**
     * 生成互动热力图数据。
     * 按网格聚合交互事件位置，生成热力数据。
     */
    public Map<String, Object> generateHeatmap(String projectId, String period) {
        Instant since = getPeriodStart(period);

        List<InteractionEvent> events = interactions.values().stream()
                .filter(e -> projectId.equals(e.getProjectId()) && e.getTimestamp().isAfter(since))
                .toList();

        // 按节点聚合
        Map<String, Long> byNode = events.stream()
                .filter(e -> e.getTargetNodeId() != null)
                .collect(Collectors.groupingBy(
                        InteractionEvent::getTargetNodeId,
                        Collectors.counting()));

        // 按事件类型聚合
        Map<String, Long> byType = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventType().name(),
                        Collectors.counting()));

        // 网格热力点（简化：按位置聚类）
        List<Map<String, Object>> heatPoints = new ArrayList<>();
        Map<String, List<InteractionEvent>> grid = events.stream()
                .collect(Collectors.groupingBy(e ->
                        String.format("%.1f:%.1f:%.1f",
                                Math.floor(e.getPositionX() * 2) / 2,
                                Math.floor(e.getPositionY() * 2) / 2,
                                Math.floor(e.getPositionZ() * 2) / 2)));

        for (Map.Entry<String, List<InteractionEvent>> entry : grid.entrySet()) {
            String[] coords = entry.getKey().split(":");
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("x", Float.parseFloat(coords[0]));
            point.put("y", Float.parseFloat(coords[1]));
            point.put("z", Float.parseFloat(coords[2]));
            point.put("intensity", entry.getValue().size());
            point.put("eventTypes", entry.getValue().stream()
                    .map(e -> e.getEventType().name())
                    .distinct()
                    .toList());
            heatPoints.add(point);
        }

        Map<String, Object> heatmap = new LinkedHashMap<>();
        heatmap.put("projectId", projectId);
        heatmap.put("period", period);
        heatmap.put("totalEvents", events.size());
        heatmap.put("byNode", byNode);
        heatmap.put("byType", byType);
        heatmap.put("heatPoints", heatPoints);
        heatmap.put("generatedAt", Instant.now());

        return heatmap;
    }

    /**
     * 获取时间序列趋势数据（按天）。
     */
    public Map<String, Object> getDailyTrend(String projectId, int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);

        List<VisitRecord> filteredVisits = visits.values().stream()
                .filter(v -> projectId.equals(v.getProjectId()) && v.getEnteredAt().isAfter(since))
                .toList();

        List<InteractionEvent> filteredInteractions = interactions.values().stream()
                .filter(e -> projectId.equals(e.getProjectId()) && e.getTimestamp().isAfter(since))
                .toList();

        // 按天聚合
        Map<LocalDate, Long> visitsByDay = filteredVisits.stream()
                .collect(Collectors.groupingBy(
                        v -> LocalDate.ofInstant(v.getEnteredAt(), ZoneOffset.UTC),
                        Collectors.counting()));

        Map<LocalDate, Long> interactionsByDay = filteredInteractions.stream()
                .collect(Collectors.groupingBy(
                        e -> LocalDate.ofInstant(e.getTimestamp(), ZoneOffset.UTC),
                        Collectors.counting()));

        // 构建趋势数据
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("visits", visitsByDay.getOrDefault(date, 0L));
            day.put("interactions", interactionsByDay.getOrDefault(date, 0L));
            trend.add(day);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("days", days);
        result.put("trend", trend);
        result.put("generatedAt", Instant.now());
        return result;
    }

    /**
     * 获取跳出率分析。
     */
    public Map<String, Object> getBounceRateAnalysis(String projectId, String period) {
        Instant since = getPeriodStart(period);

        List<VisitRecord> filteredVisits = visits.values().stream()
                .filter(v -> projectId.equals(v.getProjectId())
                        && v.getEnteredAt().isAfter(since)
                        && v.getLeftAt() != null)
                .toList();

        long total = filteredVisits.size();
        long bounced = filteredVisits.stream().filter(VisitRecord::isBounced).count();

        // 按来源分组跳出率
        Map<String, Map<String, Object>> bounceBySource = filteredVisits.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getEntrySource() != null ? v.getEntrySource() : "unknown"))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            long groupTotal = e.getValue().size();
                            long groupBounced = e.getValue().stream().filter(VisitRecord::isBounced).count();
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("total", groupTotal);
                            m.put("bounced", groupBounced);
                            m.put("rate", groupTotal > 0 ? (float) groupBounced / groupTotal : 0);
                            return m;
                        },
                        (a, b) -> b,
                        LinkedHashMap::new));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("period", period);
        result.put("totalVisits", total);
        result.put("bouncedVisits", bounced);
        result.put("bounceRate", total > 0 ? (float) bounced / total : 0);
        result.put("bounceBySource", bounceBySource);
        result.put("generatedAt", Instant.now());
        return result;
    }

    /**
     * 获取全部数据摘要（用于全局排行）。
     */
    public List<Map<String, Object>> getProjectRanking(String metric, int limit) {
        return visits.values().stream()
                .collect(Collectors.groupingBy(VisitRecord::getProjectId))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("projectId", e.getKey());
                    m.put("totalVisits", e.getValue().size());
                    m.put("uniqueVisitors", e.getValue().stream()
                            .map(VisitRecord::getVisitorId).distinct().count());
                    m.put("avgDurationMs", e.getValue().stream()
                            .filter(v -> v.getLeftAt() != null)
                            .mapToLong(VisitRecord::getDurationMs)
                            .average().orElse(0));
                    return m;
                })
                .sorted((a, b) -> Long.compare(
                        (Long) b.getOrDefault(metric, 0L),
                        (Long) a.getOrDefault(metric, 0L)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ── 私有辅助 ──

    private Instant getPeriodStart(String period) {
        return switch (period.toLowerCase()) {
            case "day", "daily" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "week", "weekly" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "month", "monthly" -> Instant.now().minus(30, ChronoUnit.DAYS);
            case "quarter" -> Instant.now().minus(90, ChronoUnit.DAYS);
            case "year" -> Instant.now().minus(365, ChronoUnit.DAYS);
            default -> Instant.now().minus(7, ChronoUnit.DAYS);
        };
    }

    private Map<String, Object> getVisitorStats(List<VisitRecord> visits) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalVisits", visits.size());
        stats.put("uniqueVisitors", visits.stream()
                .map(VisitRecord::getVisitorId).distinct().count());
        stats.put("uniqueSessions", visits.stream()
                .map(VisitRecord::getSessionId).distinct().count());

        // 平均停留时长
        double avgDuration = visits.stream()
                .filter(v -> v.getLeftAt() != null)
                .mapToLong(VisitRecord::getDurationMs)
                .average().orElse(0);
        stats.put("avgDurationMs", avgDuration);

        // 跳出率
        long bounced = visits.stream().filter(VisitRecord::isBounced).count();
        stats.put("bounceRate", visits.size() > 0 ? (float) bounced / visits.size() : 0);

        return stats;
    }

    private Map<String, Object> getDurationDistribution(List<VisitRecord> visits) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("<10s", 0L);
        distribution.put("10s-30s", 0L);
        distribution.put("30s-1min", 0L);
        distribution.put("1min-5min", 0L);
        distribution.put("5min-15min", 0L);
        distribution.put("15min-30min", 0L);
        distribution.put(">30min", 0L);

        for (VisitRecord v : visits) {
            if (v.getLeftAt() == null) continue;
            long sec = v.getDurationMs() / 1000;
            if (sec < 10) distribution.merge("<10s", 1L, Long::sum);
            else if (sec < 30) distribution.merge("10s-30s", 1L, Long::sum);
            else if (sec < 60) distribution.merge("30s-1min", 1L, Long::sum);
            else if (sec < 300) distribution.merge("1min-5min", 1L, Long::sum);
            else if (sec < 900) distribution.merge("5min-15min", 1L, Long::sum);
            else if (sec < 1800) distribution.merge("15min-30min", 1L, Long::sum);
            else distribution.merge(">30min", 1L, Long::sum);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("distribution", distribution);
        result.put("totalWithDuration", visits.stream().filter(v -> v.getLeftAt() != null).count());
        return result;
    }

    private Map<String, Object> getInteractionStats(List<InteractionEvent> events) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalInteractions", events.size());

        Map<String, Long> byType = events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEventType().name(),
                        Collectors.counting()));
        stats.put("byType", byType);

        // 互动最多的节点 Top 10
        List<Map<String, Object>> topNodes = events.stream()
                .filter(e -> e.getTargetNodeId() != null)
                .collect(Collectors.groupingBy(InteractionEvent::getTargetNodeId, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("nodeId", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();
        stats.put("topNodes", topNodes);

        return stats;
    }

    private Map<String, Object> getSourceAnalysis(List<VisitRecord> visits) {
        Map<String, Long> bySource = visits.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getEntrySource() != null ? v.getEntrySource() : "unknown",
                        Collectors.counting()));
        Map<String, Object> analysis = new LinkedHashMap<>(bySource);
        analysis.put("total", visits.size());
        return analysis;
    }

    private Map<String, Object> getDeviceAnalysis(List<VisitRecord> visits) {
        Map<String, Long> byDevice = visits.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getDeviceType() != null ? v.getDeviceType() : "unknown",
                        Collectors.counting()));
        Map<String, Object> analysis = new LinkedHashMap<>(byDevice);
        analysis.put("total", visits.size());
        return analysis;
    }

    private Map<String, Object> getRegionDistribution(List<VisitRecord> visits) {
        Map<String, Long> byRegion = visits.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getRegion() != null ? v.getRegion() : "unknown",
                        Collectors.counting()));
        Map<String, Object> distribution = new LinkedHashMap<>(byRegion);
        distribution.put("total", visits.size());
        return distribution;
    }
}
