package com.solra.crt.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目模板领域实体。
 * 预定义的创作模板，包含默认场景配置。
 */
public class Template {

    private String templateId;
    private String name;
    private String description;
    private String authorId;
    private TemplateCategory category;
    private String thumbnailUrl;
    private ProjectConfig defaultConfig;
    private List<String> tags;
    private int usageCount;
    private float rating;
    private boolean isOfficial;
    private Instant createdAt;

    public enum TemplateCategory {
        SPACE, GALLERY, AVATAR, GAME
    }

    public Template(String templateId, String name, String description,
                    String authorId, TemplateCategory category) {
        this.templateId = templateId;
        this.name = name;
        this.description = description;
        this.authorId = authorId;
        this.category = category != null ? category : TemplateCategory.SPACE;
        this.tags = new ArrayList<>();
        this.defaultConfig = new ProjectConfig();
        this.usageCount = 0;
        this.rating = 0.0f;
        this.isOfficial = false;
        this.createdAt = Instant.now();
    }

    public void incrementUsage() {
        this.usageCount++;
    }

    public void updateRating(float rating) {
        this.rating = Math.max(0.0f, Math.min(5.0f, rating));
    }

    // Getters and setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public TemplateCategory getCategory() { return category; }
    public void setCategory(TemplateCategory category) { this.category = category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public ProjectConfig getDefaultConfig() { return defaultConfig; }
    public void setDefaultConfig(ProjectConfig defaultConfig) { this.defaultConfig = defaultConfig; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public int getUsageCount() { return usageCount; }
    public float getRating() { return rating; }
    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
