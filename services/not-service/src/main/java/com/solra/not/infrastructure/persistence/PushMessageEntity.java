package com.solra.not.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "push_messages")
public class PushMessageEntity {
    @Id @Column(name = "push_id", length = 64)
    private String pushId;
    @Column(name = "notification_id", length = 64)
    private String notificationId;
    @Column(name = "device_token", length = 512)
    private String deviceToken;
    @Column(name = "platform", length = 10)
    private String platform;
    @Column(name = "push_provider", length = 10)
    private String pushProvider;
    @Column(name = "provider_message_id", length = 256)
    private String providerMessageId;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "failure_reason")
    private String failureReason;

    public PushMessageEntity() {}

    public String getPushId() { return pushId; }
    public void setPushId(String pushId) { this.pushId = pushId; }
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getPushProvider() { return pushProvider; }
    public void setPushProvider(String pushProvider) { this.pushProvider = pushProvider; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
