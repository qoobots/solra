package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * LeaderboardService 领域服务单元测试 — SPC-008 空间排行榜
 */
@DisplayName("LeaderboardService 空间排行榜领域服务")
class LeaderboardServiceTest {

    private SpaceRepository spaceRepo;
    private LeaderboardService service;

    @BeforeEach
    void setUp() {
        spaceRepo = mock(SpaceRepository.class);
        service = new LeaderboardService(spaceRepo);
    }

    @Test
    @DisplayName("获取日榜 — 按热度分降序排列")
    void getDailyLeaderboardSortedByHotScore() {
        List<Space> spaces = createSampleSpaces(10);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.DAILY, 5);

        assertEquals(5, entries.size());
        assertEquals(1, entries.get(0).getRank());
        assertEquals(2, entries.get(1).getRank());

        // 验证降序
        for (int i = 0; i < entries.size() - 1; i++) {
            assertTrue(entries.get(i).getHotScore() >= entries.get(i + 1).getHotScore(),
                    "排行榜应按热度分降序排列");
        }
    }

    @Test
    @DisplayName("获取周榜 — 默认topN=100")
    void getWeeklyLeaderboardDefaultTopN() {
        List<Space> spaces = createSampleSpaces(10);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.WEEKLY, 0);

        // 默认 topN = min(topN, 100) → 默认100，数据只有10条 → 返回10条
        assertEquals(10, entries.size());
    }

    @Test
    @DisplayName("获取月榜 — topN限制")
    void getMonthlyLeaderboardWithLimit() {
        List<Space> spaces = createSampleSpaces(10);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.MONTHLY, 3);

        assertEquals(3, entries.size());
    }

    @Test
    @DisplayName("按分类过滤排行榜")
    void getLeaderboardByCategory() {
        List<Space> spaces = createSampleSpaces(8);
        // 第1-3个为 ENTERTAINMENT，其余为其他分类
        spaces.get(0).getMeta().setCategory(SpaceCategory.ENTERTAINMENT);
        spaces.get(1).getMeta().setCategory(SpaceCategory.ENTERTAINMENT);
        spaces.get(2).getMeta().setCategory(SpaceCategory.ENTERTAINMENT);

        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboardByCategory(
                LeaderboardPeriod.DAILY,
                List.of(SpaceCategory.ENTERTAINMENT),
                10);

        assertEquals(3, entries.size());
        entries.forEach(e -> assertEquals(SpaceCategory.ENTERTAINMENT, e.getCategory()));
    }

    @Test
    @DisplayName("缓存命中 — 同一周期内返回缓存数据")
    void leaderboardCacheHit() {
        List<Space> spaces = createSampleSpaces(5);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        // 第一次调用 — 构建缓存
        List<LeaderboardEntry> first = service.getLeaderboard(LeaderboardPeriod.DAILY, 10);
        assertEquals(5, first.size());

        // 第二次调用 — 应命中缓存（不再查询 repo）
        List<LeaderboardEntry> second = service.getLeaderboard(LeaderboardPeriod.DAILY, 10);
        assertEquals(5, second.size());

        // 验证 repo 只被调用了一次
        verify(spaceRepo, times(1)).findPublished(0, 500, List.of(), "popular");
    }

    @Test
    @DisplayName("强制刷新清除所有缓存")
    void refreshAllInvalidatesCache() {
        List<Space> spaces = createSampleSpaces(5);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        service.getLeaderboard(LeaderboardPeriod.DAILY, 10);
        service.getLeaderboard(LeaderboardPeriod.WEEKLY, 10);
        service.getLeaderboard(LeaderboardPeriod.MONTHLY, 10);

        // 刷新所有缓存
        service.refreshAll();

        // 刷新后应重新查询（3个周期各查一次 = 3次，加之前3次 = 6次）
        service.getLeaderboard(LeaderboardPeriod.DAILY, 10);
        service.getLeaderboard(LeaderboardPeriod.WEEKLY, 10);
        service.getLeaderboard(LeaderboardPeriod.MONTHLY, 10);

        verify(spaceRepo, times(6)).findPublished(0, 500, List.of(), "popular");
    }

    @Test
    @DisplayName("空数据返回空列表")
    void emptyDataReturnsEmptyList() {
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(List.of());

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.DAILY, 10);
        assertTrue(entries.isEmpty());
    }

    @Test
    @DisplayName("排行榜条目包含正确排名")
    void leaderboardEntriesHaveCorrectRank() {
        List<Space> spaces = createSampleSpaces(3);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.DAILY, 10);

        assertEquals(1, entries.get(0).getRank());
        assertEquals(2, entries.get(1).getRank());
        assertEquals(3, entries.get(2).getRank());
    }

    @Test
    @DisplayName("排行榜条目包含统计信息")
    void leaderboardEntriesContainStats() {
        List<Space> spaces = createSampleSpaces(1);
        Space space = spaces.get(0);
        space.getStats().setViewCount(100);
        space.getStats().setLikeCount(50);
        space.getStats().setShareCount(20);
        space.getStats().setVisitorCount(200);

        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.DAILY, 10);

        LeaderboardEntry entry = entries.get(0);
        assertEquals(100, entry.getViewCount());
        assertEquals(50, entry.getLikeCount());
        assertEquals(20, entry.getShareCount());
        assertEquals(200, entry.getVisitorCount());
    }

    @Test
    @DisplayName("快照时间 — 三个周期均有记录")
    void snapshotTimesForAllPeriods() {
        List<Space> spaces = createSampleSpaces(3);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        service.getLeaderboard(LeaderboardPeriod.DAILY, 10);
        service.getLeaderboard(LeaderboardPeriod.WEEKLY, 10);
        service.getLeaderboard(LeaderboardPeriod.MONTHLY, 10);

        Map<LeaderboardPeriod, Instant> times = service.getSnapshotTimes();
        assertEquals(3, times.size());
        assertTrue(times.containsKey(LeaderboardPeriod.DAILY));
        assertTrue(times.containsKey(LeaderboardPeriod.WEEKLY));
        assertTrue(times.containsKey(LeaderboardPeriod.MONTHLY));
        assertNotEquals(Instant.EPOCH, times.get(LeaderboardPeriod.DAILY));
    }

    @Test
    @DisplayName("topN超过100时截断为100")
    void topNClampedTo100() {
        List<Space> spaces = createSampleSpaces(150);
        when(spaceRepo.findPublished(0, 500, List.of(), "popular")).thenReturn(spaces);

        List<LeaderboardEntry> entries = service.getLeaderboard(LeaderboardPeriod.DAILY, 200);

        assertTrue(entries.size() <= 100, "topN应被截断为100");
    }

    // ========== 测试数据工厂 ==========

    private List<Space> createSampleSpaces(int count) {
        List<Space> spaces = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Space space = new Space();
            space.setSpaceId("spc-" + String.format("%03d", i));
            space.setStatus(SpaceStatus.PUBLISHED);
            space.setCreatorId("user-" + i);

            SpaceMeta meta = new SpaceMeta("Space " + i, "Description " + i,
                    SpaceCategory.values()[i % SpaceCategory.values().length]);
            meta.setThumbnailUrl("https://cdn.solra.io/thumbs/spc-" + i + ".jpg");
            space.setMeta(meta);

            // 热度分从高到低：i越大越热门（用于验证降序）
            SpaceStats stats = new SpaceStats();
            stats.setViewCount(1000 - i * 10L);
            stats.setLikeCount(500 - i * 20L);
            stats.setShareCount(200 - i * 5L);
            stats.setVisitorCount(800 - i * 15L);
            stats.setConversationCount(100 - i * 3L);
            stats.setRating(4.5f - i * 0.1f);
            space.setStats(stats);

            space.setCreatedAt(Instant.now().minusSeconds(i * 86400L));
            space.setUpdatedAt(Instant.now());
            spaces.add(space);
        }
        return spaces;
    }
}
