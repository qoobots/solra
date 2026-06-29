package com.solra.not.domain.model;

/**
 * NotificationPreference 实体 — 通知偏好设置。
 */
public class NotificationPreference {

    private String prefId;
    private String userId;
    private NotificationType notificationType;
    private DeliveryChannel channel;
    private boolean enabled;
    private String quietHoursStart;
    private String quietHoursEnd;

    public NotificationPreference() {}

    public NotificationPreference(String prefId, String userId, NotificationType notificationType,
                                   DeliveryChannel channel, boolean enabled) {
        this.prefId = prefId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.enabled = enabled;
    }

    public void toggle() { this.enabled = !this.enabled; }
    public void enable() { this.enabled = true; }
    public void disable() { this.enabled = false; }

    public String getPrefId() { return prefId; }
    public void setPrefId(String prefId) { this.prefId = prefId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public NotificationType getNotificationType() { return notificationType; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public DeliveryChannel getChannel() { return channel; }
    public void setChannel(DeliveryChannel channel) { this.channel = channel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
}
