package com.solra.not.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id @Column(name = "notification_id", length = 64)
    private String notificationId;
    @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "type", length = 30)
    private String type;
    @Column(name = "priority", length = 10)
    private String priority;
    @Column(name = "title", length = 200)
    private String title;
    @Column(name = "body", length = 2000)
    private String body;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(name = "deep_link", length = 500)
    private String deepLink;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "read_at")
    private Instant readAt;
    @Column(name = "expires_at")
    private Instant expiresAt;

    public NotificationEntity() {}

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
