package com.solra.spc.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * PreviewCard 值对象 — 空间预览卡片（对应抖音"刷视频"的卡片）。
 * 支持 SPC-003：3D 动态渲染预览。
 */
public class PreviewCard {
    private String spaceId;
    private SpaceMeta meta;
    private List<String> previewImages;
    private String previewVideoUrl;
    private SpaceStats stats;
    private List<String> tags;

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public SpaceMeta getMeta() { return meta; }
    public void setMeta(SpaceMeta meta) { this.meta = meta; }
    public List<String> getPreviewImages() { return previewImages; }
    public void setPreviewImages(List<String> previewImages) { this.previewImages = previewImages; }
    public String getPreviewVideoUrl() { return previewVideoUrl; }
    public void setPreviewVideoUrl(String previewVideoUrl) { this.previewVideoUrl = previewVideoUrl; }
    public SpaceStats getStats() { return stats; }
    public void setStats(SpaceStats stats) { this.stats = stats; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
