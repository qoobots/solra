package com.solra.not.domain.model;

import java.time.Instant;

/**
 * DeviceRegistration 实体 — 设备注册管理。
 */
public class DeviceRegistration {

    private String registrationId;
    private String userId;
    private String deviceToken;
    private Platform platform;
    private PushProvider pushProvider;
    private String deviceName;
    private String appVersion;
    private String osVersion;
    private DeviceStatus status;
    private Instant createdAt;
    private Instant lastUsedAt;

    public DeviceRegistration() {}

    public DeviceRegistration(String registrationId, String userId, String deviceToken,
                              Platform platform, PushProvider pushProvider, String deviceName) {
        this.registrationId = registrationId;
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.platform = platform;
        this.pushProvider = pushProvider;
        this.deviceName = deviceName;
        this.status = DeviceStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    public void deactivate() { this.status = DeviceStatus.INACTIVE; }
    public void unregister() { this.status = DeviceStatus.UNREGISTERED; }
    public boolean isActive() { return status == DeviceStatus.ACTIVE; }

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }
    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public PushProvider getPushProvider() { return pushProvider; }
    public void setPushProvider(PushProvider pushProvider) { this.pushProvider = pushProvider; }
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public String getOsVersion() { return osVersion; }
    public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
