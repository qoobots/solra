package com.solra.not.domain.model;

import java.time.Instant;

/**
 * PushMessage 实体 — 推送消息记录。
 * 记录每次实际推送的状态、平台和提供商标识。
 */
public class PushMessage {

    private String pushId;
    private String notificationId;
    private String deviceToken;
    private Platform platform;
    private PushProvider pushProvider;
    private String providerMessageId;
    private PushStatus status;
    private Instant sentAt;
    private Instant deliveredAt;
    private String failureReason;

    public PushMessage() {}

    public PushMessage(String pushId, String notificationId, String deviceToken,
                       Platform platform, PushProvider pushProvider) {
        this.pushId = pushId;
        this.notificationId = notificationId;
        this.deviceToken = deviceToken;
        this.platform = platform;
        this.pushProvider = pushProvider;
        this.status = PushStatus.PENDING;
    }

    public void sent(String providerMsgId) { this.providerMessageId = providerMsgId; this.sentAt = Instant.now(); this.status = PushStatus.SENT; }
    public void delivered() { this.deliveredAt = Instant.now(); this.status = PushStatus.DELIVERED; }
    public void failed(String reason) { this.failureReason = reason; this.status = PushStatus.FAILED; }

    public String getPushId() { return pushId; }
    public void setPushId(String pushId) { this.pushId = pushId; }
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public PushProvider getPushProvider() { return pushProvider; }
    public void setPushProvider(PushProvider pushProvider) { this.pushProvider = pushProvider; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public PushStatus getStatus() { return status; }
    public void setStatus(PushStatus status) { this.status = status; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
