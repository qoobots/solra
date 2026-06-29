package com.solra.crt.application.dto;

import com.solra.crt.domain.entity.Template;
import java.time.Instant;
import java.util.List;

/**
 * 模板数据传输对象。
 */
public class TemplateDTO {

    private String templateId;
    private String name;
    private String description;
    private String authorId;
    private String category;
    private String thumbnailUrl;
    private List<String> tags;
    private int usageCount;
    private float rating;
    private boolean isOfficial;
    private Instant createdAt;

    public static TemplateDTO from(Template template) {
        TemplateDTO dto = new TemplateDTO();
        dto.templateId = template.getTemplateId();
        dto.name = template.getName();
        dto.description = template.getDescription();
        dto.authorId = template.getAuthorId();
        dto.category = template.getCategory().name();
        dto.thumbnailUrl = template.getThumbnailUrl();
        dto.tags = template.getTags();
        dto.usageCount = template.getUsageCount();
        dto.rating = template.getRating();
        dto.isOfficial = template.isOfficial();
        dto.createdAt = template.getCreatedAt();
        return dto;
    }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public int getUsageCount() { return usageCount; }
    public float getRating() { return rating; }
    public boolean isOfficial() { return isOfficial; }
    public void setOfficial(boolean official) { isOfficial = official; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
