package com.solra.spc.domain.model;

import java.time.Instant;

/**
 * SPC-008: 空间排行榜条目 — 值对象。
 * 记录一个空间在特定排行榜周期中的排名及各项指标。
 */
public class LeaderboardEntry {

    private String spaceId;
    private String title;
    private String thumbnailUrl;
    private SpaceCategory category;
    private int rank;
    private long hotScore;          // 综合热度分
    private long viewCount;
    private long likeCount;
    private long shareCount;
    private long visitorCount;
    private float rating;
    private int rankChange;         // 排名变化（正数=上升，负数=下降，0=不变）
    private LeaderboardPeriod period;
    private Instant snapshotAt;     // 快照时间（T+1）

    public LeaderboardEntry() {}

    /**
     * 综合热度分计算公式：
     *   hotScore = viewCount×1 + likeCount×3 + shareCount×5 + visitorCount×2 + conversationCount×2
     * 权重反映不同行为的用户参与深度。
     */
    public static long calculateHotScore(SpaceStats stats) {
        if (stats == null) return 0;
        return stats.getViewCount() * 1L
             + stats.getLikeCount() * 3L
             + stats.getShareCount() * 5L
             + stats.getVisitorCount() * 2L
             + stats.getConversationCount() * 2L;
    }

    // ---- getters / setters ----

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public SpaceCategory getCategory() { return category; }
    public void setCategory(SpaceCategory category) { this.category = category; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public long getHotScore() { return hotScore; }
    public void setHotScore(long hotScore) { this.hotScore = hotScore; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }

    public long getShareCount() { return shareCount; }
    public void setShareCount(long shareCount) { this.shareCount = shareCount; }

    public long getVisitorCount() { return visitorCount; }
    public void setVisitorCount(long visitorCount) { this.visitorCount = visitorCount; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public int getRankChange() { return rankChange; }
    public void setRankChange(int rankChange) { this.rankChange = rankChange; }

    public LeaderboardPeriod getPeriod() { return period; }
    public void setPeriod(LeaderboardPeriod period) { this.period = period; }

    public Instant getSnapshotAt() { return snapshotAt; }
    public void setSnapshotAt(Instant snapshotAt) { this.snapshotAt = snapshotAt; }
}
