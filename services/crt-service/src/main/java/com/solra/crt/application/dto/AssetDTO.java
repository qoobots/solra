package com.solra.crt.application.dto;

import com.solra.crt.domain.entity.Asset;
import java.time.Instant;
import java.util.List;

/**
 * 资产数据传输对象。
 */
public class AssetDTO {

    private String assetId;
    private String spaceId;
    private String ownerId;
    private String type;
    private String name;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private List<String> tags;

    // AssetMeta fields
    private String fileFormat;
    private long fileSizeBytes;
    private String cdnUrl;
    private String thumbnailUrl;

    public static AssetDTO from(Asset asset) {
        AssetDTO dto = new AssetDTO();
        dto.assetId = asset.getAssetId();
        dto.spaceId = asset.getSpaceId();
        dto.ownerId = asset.getOwnerId();
        dto.type = asset.getType().name();
        dto.name = asset.getName();
        dto.description = asset.getDescription();
        dto.status = asset.getStatus().name();
        dto.createdAt = asset.getCreatedAt();
        dto.updatedAt = asset.getUpdatedAt();
        dto.version = asset.getVersion();
        dto.tags = asset.getTags();
        if (asset.getMeta() != null) {
            dto.fileFormat = asset.getMeta().getFileFormat();
            dto.fileSizeBytes = asset.getMeta().getFileSizeBytes();
            dto.cdnUrl = asset.getMeta().getCdnUrl();
            dto.thumbnailUrl = asset.getMeta().getThumbnailUrl();
        }
        return dto;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
