package com.solra.spc.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LeaderboardEntry 值对象单元测试 — SPC-008 空间排行榜
 */
@DisplayName("LeaderboardEntry 排行榜条目值对象")
class LeaderboardEntryTest {

    private SpaceStats stats;

    @BeforeEach
    void setUp() {
        stats = new SpaceStats();
    }

    @Test
    @DisplayName("空统计时热度分为0")
    void calculateHotScoreWithEmptyStats() {
        assertEquals(0, LeaderboardEntry.calculateHotScore(null));
        assertEquals(0, LeaderboardEntry.calculateHotScore(new SpaceStats()));
    }

    @Test
    @DisplayName("热度分 = viewCount×1 + likeCount×3 + shareCount×5 + visitorCount×2 + conversationCount×2")
    void calculateHotScoreWithAllDimensions() {
        stats.setViewCount(100);
        stats.setLikeCount(50);
        stats.setShareCount(20);
        stats.setVisitorCount(200);
        stats.setConversationCount(30);

        long expected = 100L * 1   // view
                      + 50L * 3    // like
                      + 20L * 5    // share
                      + 200L * 2   // visitor
                      + 30L * 2;   // conversation

        assertEquals(expected, LeaderboardEntry.calculateHotScore(stats));
    }

    @Test
    @DisplayName("仅浏览量贡献热度分")
    void calculateHotScoreViewOnly() {
        stats.setViewCount(1000);
        assertEquals(1000, LeaderboardEntry.calculateHotScore(stats));
    }

    @Test
    @DisplayName("分享权重最高(×5)")
    void shareHasHighestWeight() {
        stats.setShareCount(100);
        long shareScore = LeaderboardEntry.calculateHotScore(stats);

        stats.setShareCount(0);
        stats.setViewCount(100);
        long viewScore = LeaderboardEntry.calculateHotScore(stats);

        // 100 shares = 500 score, 100 views = 100 score
        assertTrue(shareScore > viewScore,
                "分享权重(×5)应大于浏览权重(×1): " + shareScore + " > " + viewScore);
    }

    @Test
    @DisplayName("排行榜条目字段读写")
    void leaderboardEntryFields() {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setSpaceId("spc-001");
        entry.setTitle("Test Space");
        entry.setThumbnailUrl("https://cdn.solra.io/thumbs/spc-001.jpg");
        entry.setCategory(SpaceCategory.ENTERTAINMENT);
        entry.setRank(1);
        entry.setHotScore(1500);
        entry.setViewCount(300);
        entry.setLikeCount(100);
        entry.setShareCount(50);
        entry.setVisitorCount(200);
        entry.setRating(4.5f);
        entry.setRankChange(3);
        entry.setPeriod(LeaderboardPeriod.WEEKLY);

        assertEquals("spc-001", entry.getSpaceId());
        assertEquals("Test Space", entry.getTitle());
        assertEquals(1, entry.getRank());
        assertEquals(1500, entry.getHotScore());
        assertEquals(300, entry.getViewCount());
        assertEquals(100, entry.getLikeCount());
        assertEquals(50, entry.getShareCount());
        assertEquals(200, entry.getVisitorCount());
        assertEquals(4.5f, entry.getRating(), 0.001);
        assertEquals(3, entry.getRankChange());
        assertEquals(LeaderboardPeriod.WEEKLY, entry.getPeriod());
    }

    @Test
    @DisplayName("RankChange 正数表示上升")
    void rankChangePositiveMeansRising() {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setRankChange(5);
        assertTrue(entry.getRankChange() > 0, "正数 rankChange 表示排名上升");
    }

    @Test
    @DisplayName("RankChange 负数表示下降")
    void rankChangeNegativeMeansFalling() {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setRankChange(-3);
        assertTrue(entry.getRankChange() < 0, "负数 rankChange 表示排名下降");
    }

    @Test
    @DisplayName("热度分不受null统计影响")
    void hotScoreHandlesNullStats() {
        assertEquals(0, LeaderboardEntry.calculateHotScore(null));
    }

    @Test
    @DisplayName("大数值热度分不溢出")
    void hotScoreNoOverflow() {
        stats.setViewCount(Long.MAX_VALUE / 2);
        stats.setLikeCount(1);
        long score = LeaderboardEntry.calculateHotScore(stats);
        assertTrue(score > 0, "大数值热度分不应溢出");
    }
}
