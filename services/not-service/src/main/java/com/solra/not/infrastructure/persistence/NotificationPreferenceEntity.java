package com.solra.not.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "notification_type", "channel"})
})
public class NotificationPreferenceEntity {
    @Id @Column(name = "pref_id", length = 64)
    private String prefId;
    @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "notification_type", length = 30)
    private String notificationType;
    @Column(name = "channel", length = 10)
    private String channel;
    @Column(name = "enabled")
    private boolean enabled;
    @Column(name = "quiet_hours_start", length = 5)
    private String quietHoursStart;
    @Column(name = "quiet_hours_end", length = 5)
    private String quietHoursEnd;

    public NotificationPreferenceEntity() {}

    public String getPrefId() { return prefId; }
    public void setPrefId(String prefId) { this.prefId = prefId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(String quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public String getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(String quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
}
