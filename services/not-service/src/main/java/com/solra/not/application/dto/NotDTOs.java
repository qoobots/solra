package com.solra.not.application.dto;

import com.solra.not.domain.model.Notification;
import com.solra.not.domain.model.NotificationStatus;
import com.solra.not.domain.model.NotificationType;

import java.time.Instant;
import java.util.List;

/** 发送通知命令 */
public class SendNotificationCommand {
    private List<String> targetUserIds;
    private String type;
    private String title;
    private String body;
    private String imageUrl;
    private String deepLink;
    private String priority;
    private int expiresInHours;

    public List<String> getTargetUserIds() { return targetUserIds; }
    public void setTargetUserIds(List<String> targetUserIds) { this.targetUserIds = targetUserIds; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public int getExpiresInHours() { return expiresInHours; }
    public void setExpiresInHours(int expiresInHours) { this.expiresInHours = expiresInHours; }
}

/** 通知结果 DTO */
public record NotificationResultDTO(
        String notificationId, String title, String body, String type,
        String status, Instant createdAt
) {
    public static NotificationResultDTO from(Notification n) {
        return new NotificationResultDTO(n.getNotificationId(), n.getTitle(), n.getBody(),
                n.getType().name(), n.getStatus().name(), n.getCreatedAt());
    }
}

/** 收件箱分页结果 */
public record InboxPageDTO(List<NotificationResultDTO> items, long totalCount, long unreadCount) {}

/** 设备注册命令 */
public record RegisterDeviceCommand(
        String userId, String deviceToken, String platform,
        String deviceName, String appVersion, String osVersion) {}

/** 设备注册结果 */
public record DeviceRegistrationResultDTO(
        String registrationId, String userId, String platform, String status, Instant createdAt) {}

/** 偏好更新命令 */
public record PreferenceUpdateCommand(
        String userId, String notificationType, String channel, boolean enabled) {}

/** 偏好结果 */
public record PreferenceResultDTO(
        String prefId, String userId, String notificationType, String channel, boolean enabled) {}
