package com.solra.spc.domain.model;

/**
 * SpaceMeta 值对象 — 空间元信息。
 */
public class SpaceMeta {

    private String title;
    private String description;
    private SpaceCategory category = SpaceCategory.PERSONAL;
    private String thumbnailUrl;
    private Double latitude;
    private Double longitude;
    private SpacePrivacy privacy = SpacePrivacy.PUBLIC;
    private String languageCode = "zh-CN";

    public SpaceMeta() {}

    public SpaceMeta(String title, String description, SpaceCategory category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public boolean isPublic() { return privacy == SpacePrivacy.PUBLIC; }

    // ---- getters / setters ----
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SpaceCategory getCategory() { return category; }
    public void setCategory(SpaceCategory category) { this.category = category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public SpacePrivacy getPrivacy() { return privacy; }
    public void setPrivacy(SpacePrivacy privacy) { this.privacy = privacy; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
}
