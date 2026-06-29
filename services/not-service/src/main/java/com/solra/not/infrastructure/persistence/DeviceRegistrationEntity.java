package com.solra.not.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "device_registrations")
public class DeviceRegistrationEntity {
    @Id @Column(name = "registration_id", length = 64)
    private String registrationId;
    @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "device_token", length = 512, unique = true)
    private String deviceToken;
    @Column(name = "platform", length = 10)
    private String platform;
    @Column(name = "push_provider", length = 10)
    private String pushProvider;
    @Column(name = "device_name", length = 100)
    private String deviceName;
    @Column(name = "app_version", length = 20)
    private String appVersion;
    @Column(name = "os_version", length = 20)
    private String osVersion;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public DeviceRegistrationEntity() {}

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getPushProvider() { return pushProvider; }
    public void setPushProvider(String pushProvider) { this.pushProvider = pushProvider; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
