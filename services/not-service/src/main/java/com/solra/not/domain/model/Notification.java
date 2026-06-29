package com.solra.not.domain.model;

import java.time.Instant;

/**
 * Notification 聚合根 — 通知实体。
 * 支持系统通知、社交通知、空间活动等多种类型，带优先级和过期管理。
 */
public class Notification {

    private String notificationId;
    private String userId;
    private NotificationType type;
    private NotificationPriority priority;
    private String title;
    private String body;
    private String imageUrl;
    private String deepLink;
    private NotificationStatus status;
    private String metadata;
    private Instant createdAt;
    private Instant readAt;
    private Instant expiresAt;

    public Notification() {}

    public Notification(String notificationId, String userId, NotificationType type,
                        NotificationPriority priority, String title, String body) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.priority = priority;
        this.title = title;
        this.body = body;
        this.status = NotificationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void sent() { if (this.status == NotificationStatus.PENDING) this.status = NotificationStatus.SENT; }
    public void delivered() { if (this.status == NotificationStatus.SENT) this.status = NotificationStatus.DELIVERED; }
    public void markRead() { if (this.status == NotificationStatus.DELIVERED) { this.status = NotificationStatus.READ; this.readAt = Instant.now(); } }
    public void dismiss() { this.status = NotificationStatus.DISMISSED; }
    public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }
    public boolean isUnread() { return status == NotificationStatus.PENDING || status == NotificationStatus.SENT || status == NotificationStatus.DELIVERED; }

    // getters / setters
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public NotificationPriority getPriority() { return priority; }
    public void setPriority(NotificationPriority priority) { this.priority = priority; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
