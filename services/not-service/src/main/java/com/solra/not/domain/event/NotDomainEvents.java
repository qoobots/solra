package com.solra.not.domain.event;

/**
 * NotDomainEvents — 通知领域事件集合。
 */
public final class NotDomainEvents {

    private NotDomainEvents() {}

    public record NotificationSent(String notificationId, String userId) {}
    public record NotificationDelivered(String notificationId, String userId) {}
    public record NotificationRead(String notificationId, String userId) {}
    public record DeviceRegistered(String userId, String platform, String registrationId) {}
    public record DeviceUnregistered(String userId, String platform, String registrationId) {}
    public record PushDelivered(String pushId, String providerMessageId) {}
    public record PushFailed(String pushId, String provider, String reason) {}

    /** 收件箱消息已发送 */
    public record InboxMessageSent(String messageId, String senderId, String recipientId) {}
    /** 收件箱消息已读 */
    public record InboxMessageRead(String messageId, String recipientId) {}
}
