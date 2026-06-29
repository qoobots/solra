package com.solra.not.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * NotificationTemplateEntity — JPA 实体映射。
 * NOT-004: 通知模板持久化。
 */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplateEntity {

    @Id
    @Column(name = "template_id", length = 36)
    private String templateId;

    @Column(name = "template_code", length = 64, unique = true, nullable = false)
    private String templateCode;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "type", length = 32, nullable = false)
    private String type;

    @Column(name = "default_priority", length = 16)
    private String defaultPriority;

    @Column(name = "title_template", length = 256)
    private String titleTemplate;

    @Column(name = "body_template", length = 1024)
    private String bodyTemplate;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "deep_link_template", length = 512)
    private String deepLinkTemplate;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "active")
    private boolean active;

    @Column(name = "localized_titles", columnDefinition = "jsonb")
    private String localizedTitles;

    @Column(name = "localized_bodies", columnDefinition = "jsonb")
    private String localizedBodies;

    @Column(name = "version")
    private int version;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public NotificationTemplateEntity() {}

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDeepLinkTemplate() { return deepLinkTemplate; }
    public void setDeepLinkTemplate(String deepLinkTemplate) { this.deepLinkTemplate = deepLinkTemplate; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getLocalizedTitles() { return localizedTitles; }
    public void setLocalizedTitles(String localizedTitles) { this.localizedTitles = localizedTitles; }
    public String getLocalizedBodies() { return localizedBodies; }
    public void setLocalizedBodies(String localizedBodies) { this.localizedBodies = localizedBodies; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
