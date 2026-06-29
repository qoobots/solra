package com.solra.spc.domain.model;

/**
 * SpaceStats 值对象 — 空间统计指标。
 */
public class SpaceStats {
    private long viewCount;
    private long likeCount;
    private long shareCount;
    private long visitorCount;
    private long conversationCount;
    private float rating;

    public void incrementViews() { this.viewCount++; }
    public void incrementLikes() { this.likeCount++; }
    public void incrementShares() { this.shareCount++; }
    public void incrementVisitors() { this.visitorCount++; }
    public void incrementConversations() { this.conversationCount++; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public long getShareCount() { return shareCount; }
    public void setShareCount(long shareCount) { this.shareCount = shareCount; }
    public long getVisitorCount() { return visitorCount; }
    public void setVisitorCount(long visitorCount) { this.visitorCount = visitorCount; }
    public long getConversationCount() { return conversationCount; }
    public void setConversationCount(long conversationCount) { this.conversationCount = conversationCount; }
    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}
