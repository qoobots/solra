package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SPC-008: 空间排行榜领域服务。
 * 按日/周/月周期生成空间排行榜，T+1 更新。
 *
 * 热度分公式：
 *   hotScore = viewCount×1 + likeCount×3 + shareCount×5 + visitorCount×2 + conversationCount×2
 */
public class LeaderboardService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardService.class);

    private final SpaceRepository spaceRepository;

    // 内存缓存：按周期缓存排行榜快照
    private final Map<LeaderboardPeriod, CachedLeaderboard> cache = new ConcurrentHashMap<>();

    // 默认排行榜条数
    private static final int DEFAULT_TOP_N = 100;

    public LeaderboardService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    /**
     * 获取指定周期的排行榜。
     * 如果缓存已过期（T+1 规则），则从数据库重新计算。
     */
    public List<LeaderboardEntry> getLeaderboard(LeaderboardPeriod period, int topN) {
        int n = topN > 0 ? Math.min(topN, DEFAULT_TOP_N) : DEFAULT_TOP_N;

        CachedLeaderboard cached = cache.get(period);
        Instant now = Instant.now();

        if (cached != null && !isCacheExpired(cached, period, now)) {
            log.debug("Leaderboard cache hit: period={}", period);
            return cached.entries.size() <= n
                    ? cached.entries
                    : cached.entries.subList(0, n);
        }

        log.info("Leaderboard cache miss / expired, rebuilding: period={}", period);
        List<LeaderboardEntry> entries = buildLeaderboard(period, n);
        cache.put(period, new CachedLeaderboard(entries, now));
        return entries;
    }

    /**
     * 按分类过滤获取排行榜。
     */
    public List<LeaderboardEntry> getLeaderboardByCategory(LeaderboardPeriod period,
                                                            List<SpaceCategory> categories, int topN) {
        List<LeaderboardEntry> all = getLeaderboard(period, DEFAULT_TOP_N);
        if (categories == null || categories.isEmpty()) {
            return all.size() > topN ? all.subList(0, topN) : all;
        }

        int n = topN > 0 ? topN : 20;
        return all.stream()
                .filter(e -> categories.contains(e.getCategory()))
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * 强制刷新所有周期的排行榜缓存。
     */
    public void refreshAll() {
        log.info("Force-refreshing all leaderboard caches");
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            List<LeaderboardEntry> entries = buildLeaderboard(period, DEFAULT_TOP_N);
            cache.put(period, new CachedLeaderboard(entries, Instant.now()));
        }
    }

    /**
     * 获取排行榜快照时间（用于客户端判断是否需要刷新）。
     */
    public Map<LeaderboardPeriod, Instant> getSnapshotTimes() {
        Map<LeaderboardPeriod, Instant> times = new LinkedHashMap<>();
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            CachedLeaderboard c = cache.get(period);
            times.put(period, c != null ? c.snapshotAt : Instant.EPOCH);
        }
        return times;
    }

    // ========== 内部方法 ==========

    /**
     * 从数据库构建排行榜。
     * 获取所有已发布空间，按热度分排序，取 topN。
     */
    private List<LeaderboardEntry> buildLeaderboard(LeaderboardPeriod period, int topN) {
        // 获取所有已发布空间（拉取较多数据以保证排序质量）
        List<Space> allSpaces = spaceRepository.findPublished(0, 500, List.of(), "popular");
        if (allSpaces.isEmpty()) {
            return List.of();
        }

        // 计算热度分并排序
        List<LeaderboardEntry> entries = new ArrayList<>(allSpaces.size());
        for (Space space : allSpaces) {
            LeaderboardEntry entry = buildEntry(space, period);
            entries.add(entry);
        }

        // 按热度分降序排序
        entries.sort((a, b) -> Long.compare(b.getHotScore(), a.getHotScore()));

        // 截取 topN 并设置排名
        List<LeaderboardEntry> result = new ArrayList<>(Math.min(topN, entries.size()));
        for (int i = 0; i < Math.min(topN, entries.size()); i++) {
            LeaderboardEntry entry = entries.get(i);
            entry.setRank(i + 1);
            entry.setRankChange(calculateRankChange(entry.getSpaceId(), period, i + 1));
            result.add(entry);
        }

        return result;
    }

    private LeaderboardEntry buildEntry(Space space, LeaderboardPeriod period) {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setSpaceId(space.getSpaceId());
        entry.setPeriod(period);
        entry.setSnapshotAt(Instant.now());

        SpaceMeta meta = space.getMeta();
        if (meta != null) {
            entry.setTitle(meta.getTitle());
            entry.setThumbnailUrl(meta.getThumbnailUrl());
            entry.setCategory(meta.getCategory());
        }

        SpaceStats stats = space.getStats();
        if (stats != null) {
            entry.setViewCount(stats.getViewCount());
            entry.setLikeCount(stats.getLikeCount());
            entry.setShareCount(stats.getShareCount());
            entry.setVisitorCount(stats.getVisitorCount());
            entry.setRating(stats.getRating());
            entry.setHotScore(LeaderboardEntry.calculateHotScore(stats));
        }

        return entry;
    }

    /**
     * 计算排名变化。
     * 对比上一周期缓存的排名，正数=上升，负数=下降。
     */
    private int calculateRankChange(String spaceId, LeaderboardPeriod period, int currentRank) {
        CachedLeaderboard previous = cache.get(period);
        if (previous == null || previous.entries == null) return 0;

        for (int i = 0; i < previous.entries.size(); i++) {
            if (spaceId.equals(previous.entries.get(i).getSpaceId())) {
                return (i + 1) - currentRank; // 上期排名 - 当前排名
            }
        }
        // 新上榜
        return currentRank;
    }

    /**
     * 判断缓存是否过期（T+1 规则）。
     * - DAILY: 缓存日期 != 今天 → 过期
     * - WEEKLY: 缓存所在周 != 本周 → 过期
     * - MONTHLY: 缓存所在月 != 本月 → 过期
     */
    private boolean isCacheExpired(CachedLeaderboard cached, LeaderboardPeriod period, Instant now) {
        ZoneOffset offset = ZoneOffset.UTC;
        ZonedDateTime cachedTime = cached.snapshotAt.atZone(offset);
        ZonedDateTime nowTime = now.atZone(offset);

        return switch (period) {
            case DAILY -> !cachedTime.toLocalDate().equals(nowTime.toLocalDate());
            case WEEKLY -> {
                // ISO week: Monday-based
                int cachedWeek = cachedTime.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                int cachedYear = cachedTime.get(java.time.temporal.WeekFields.ISO.weekBasedYear());
                int nowWeek = nowTime.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
                int nowYear = nowTime.get(java.time.temporal.WeekFields.ISO.weekBasedYear());
                yield cachedWeek != nowWeek || cachedYear != nowYear;
            }
            case MONTHLY -> cachedTime.getMonth() != nowTime.getMonth()
                         || cachedTime.getYear() != nowTime.getYear();
        };
    }

    // ========== 内部类 ==========

    private static class CachedLeaderboard {
        final List<LeaderboardEntry> entries;
        final Instant snapshotAt;

        CachedLeaderboard(List<LeaderboardEntry> entries, Instant snapshotAt) {
            this.entries = List.copyOf(entries);
            this.snapshotAt = snapshotAt;
        }
    }
}
