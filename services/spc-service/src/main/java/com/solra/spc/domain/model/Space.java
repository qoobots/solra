package com.solra.spc.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Space 聚合根 — 索拉 3D 空间。
 * 包含元信息、内容清单、统计数据。
 */
public class Space {

    private String spaceId;
    private SpaceMeta meta;
    private SpaceContent content;
    private SpaceStats stats;
    private String creatorId;
    private SpaceStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<String> tags;
    private Map<String, String> metadata;

    public Space() {}

    public Space(String spaceId, String creatorId) {
        this.spaceId = spaceId;
        this.creatorId = creatorId;
        this.status = SpaceStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.stats = new SpaceStats();
    }

    public void publish() {
        if (this.status == SpaceStatus.DRAFT || this.status == SpaceStatus.REVIEWING) {
            this.status = SpaceStatus.PUBLISHED;
            this.updatedAt = Instant.now();
        }
    }

    public void archive() { this.status = SpaceStatus.ARCHIVED; this.updatedAt = Instant.now(); }

    public void incrementViews() { if (stats != null) stats.incrementViews(); }
    public void incrementLikes() { if (stats != null) stats.incrementLikes(); }
    public void incrementShares() { if (stats != null) stats.incrementShares(); }
    public void incrementVisitors() { if (stats != null) stats.incrementVisitors(); }

    // ---- getters / setters ----
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public SpaceMeta getMeta() { return meta; }
    public void setMeta(SpaceMeta meta) { this.meta = meta; }
    public SpaceContent getContent() { return content; }
    public void setContent(SpaceContent content) { this.content = content; }
    public SpaceStats getStats() { return stats; }
    public void setStats(SpaceStats stats) { this.stats = stats; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public SpaceStatus getStatus() { return status; }
    public void setStatus(SpaceStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
