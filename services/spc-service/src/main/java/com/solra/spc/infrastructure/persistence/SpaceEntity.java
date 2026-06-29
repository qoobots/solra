package com.solra.spc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "spc_spaces")
public class SpaceEntity {

    @Id @Column(name = "space_id")
    private String spaceId;
    private String title;
    @Column(length = 1024)
    private String description;
    private String category;
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;
    private Double latitude;
    private Double longitude;
    private String privacy;
    @Column(name = "language_code")
    private String languageCode;
    @Column(name = "scene_file_url")
    private String sceneFileUrl;
    @Column(name = "entry_point")
    private String entryPoint;
    @Column(name = "creator_id")
    private String creatorId;
    private String status;
    @Column(name = "view_count")
    private long viewCount;
    @Column(name = "like_count")
    private long likeCount;
    @Column(name = "share_count")
    private long shareCount;
    @Column(name = "visitor_count")
    private long visitorCount;
    @Column(name = "conversation_count")
    private long conversationCount;
    private float rating;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "spc_space_tags", joinColumns = @JoinColumn(name = "space_id"))
    @Column(name = "tag")
    private List<String> tags;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "spc_space_metadata", joinColumns = @JoinColumn(name = "space_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata;

    // getters/setters
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
    public String getSceneFileUrl() { return sceneFileUrl; }
    public void setSceneFileUrl(String sceneFileUrl) { this.sceneFileUrl = sceneFileUrl; }
    public String getEntryPoint() { return entryPoint; }
    public void setEntryPoint(String entryPoint) { this.entryPoint = entryPoint; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
