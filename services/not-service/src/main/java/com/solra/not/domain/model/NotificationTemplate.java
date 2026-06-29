package com.solra.not.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * NotificationTemplate 聚合根 — 系统通知模板。
 * NOT-004: 系统通知模板管理。
 *
 * 支持变量替换、多语言、分类管理，覆盖 ≥20 种通知场景。
 */
public class NotificationTemplate {

    private String templateId;
    private String templateCode;          // 唯一模板编码，如 "space_invite"
    private String name;                  // 模板名称
    private NotificationType type;        // 通知类型
    private NotificationPriority defaultPriority; // 默认优先级
    private String titleTemplate;         // 标题模板，支持 {变量}
    private String bodyTemplate;          // 内容模板，支持 {变量}
    private String imageUrl;              // 默认图片
    private String deepLinkTemplate;      // 深链接模板
    private String category;              // 分类: SOCIAL, CREATION, SYSTEM, COMMERCIAL
    private boolean active;               // 是否启用
    private Map<String, String> localizedTitles;  // 多语言标题: {"zh-CN":"...", "en":"..."}
    private Map<String, String> localizedBodies;  // 多语言内容
    private int version;                  // 模板版本号
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public NotificationTemplate() {}

    public NotificationTemplate(String templateId, String templateCode, String name,
                                 NotificationType type, NotificationPriority defaultPriority,
                                 String titleTemplate, String bodyTemplate) {
        this.templateId = templateId;
        this.templateCode = templateCode;
        this.name = name;
        this.type = type;
        this.defaultPriority = defaultPriority;
        this.titleTemplate = titleTemplate;
        this.bodyTemplate = bodyTemplate;
        this.active = true;
        this.version = 1;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 激活模板 */
    public void activate() { this.active = true; this.updatedAt = Instant.now(); }

    /** 停用模板 */
    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }

    /** 升级版本 */
    public void bumpVersion() { this.version++; this.updatedAt = Instant.now(); }

    // ---- getters/setters ----

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public NotificationPriority getDefaultPriority() { return defaultPriority; }
    public void setDefaultPriority(NotificationPriority defaultPriority) { this.defaultPriority = defaultPriority; }
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
    public Map<String, String> getLocalizedTitles() { return localizedTitles; }
    public void setLocalizedTitles(Map<String, String> localizedTitles) { this.localizedTitles = localizedTitles; }
    public Map<String, String> getLocalizedBodies() { return localizedBodies; }
    public void setLocalizedBodies(Map<String, String> localizedBodies) { this.localizedBodies = localizedBodies; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
