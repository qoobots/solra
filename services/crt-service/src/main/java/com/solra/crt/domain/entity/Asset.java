package com.solra.crt.domain.entity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 资产领域实体。
 * 表示空间创作中的 3D 模型、纹理、材质、动画等数字资产。
 */
public class Asset {

    private String assetId;
    private String spaceId;
    private String ownerId;
    private AssetType type;
    private String name;
    private String description;
    private AssetMeta meta;
    private AssetStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private List<String> tags;

    public enum AssetType {
        MODEL_3D, TEXTURE, MATERIAL, ANIMATION, AUDIO, SCRIPT, PREFAB, SCENE
    }

    public enum AssetStatus {
        DRAFT, PUBLISHED, ARCHIVED, REVIEWING
    }

    public Asset(String assetId, String spaceId, String ownerId, AssetType type,
                 String name, String description, AssetStatus status) {
        this.assetId = assetId;
        this.spaceId = spaceId;
        this.ownerId = ownerId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : AssetStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = 1;
    }

    public void publish() {
        if (this.status == AssetStatus.DRAFT) {
            this.status = AssetStatus.PUBLISHED;
            this.version++;
            this.updatedAt = Instant.now();
        }
    }

    public void archive() {
        this.status = AssetStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public void updateMeta(AssetMeta meta) {
        this.meta = meta;
        this.updatedAt = Instant.now();
        this.version++;
    }

    // Getters and setters
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AssetMeta getMeta() { return meta; }
    public void setMeta(AssetMeta meta) { this.meta = meta; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
