package com.solra.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * AUTH-005: Structured device information value object.
 * Captures device fingerprint for multi-device login management.
 */
public class DeviceInfo {

    private final String deviceId;
    private final String deviceName;
    private final String platform;       // ios / android / web / desktop
    private final String osVersion;
    private final String appVersion;
    private final String deviceModel;
    private final Instant firstSeenAt;
    private final Instant lastSeenAt;

    private DeviceInfo(Builder builder) {
        this.deviceId = builder.deviceId != null ? builder.deviceId : UUID.randomUUID().toString();
        this.deviceName = builder.deviceName != null ? builder.deviceName : "Unknown";
        this.platform = builder.platform != null ? builder.platform : "unknown";
        this.osVersion = builder.osVersion;
        this.appVersion = builder.appVersion;
        this.deviceModel = builder.deviceModel;
        this.firstSeenAt = builder.firstSeenAt != null ? builder.firstSeenAt : Instant.now();
        this.lastSeenAt = builder.lastSeenAt != null ? builder.lastSeenAt : Instant.now();
    }

    /**
     * AUTH-005: Generate a fingerprint hash from raw device info string.
     * Used to identify the same device across sessions.
     */
    public static String fingerprint(String rawDeviceInfo) {
        if (rawDeviceInfo == null || rawDeviceInfo.isBlank()) return "unknown-" + UUID.randomUUID().toString().substring(0, 8);
        // Simple fingerprint: hash the raw info
        int hash = rawDeviceInfo.hashCode();
        return "dev-" + Integer.toHexString(hash & 0xFFFFFFFF);
    }

    /**
     * AUTH-005: Parse device info from login request string.
     * Expected format: "platform/osVersion/appVersion/deviceModel"
     */
    public static DeviceInfo fromRaw(String rawDeviceInfo) {
        if (rawDeviceInfo == null || rawDeviceInfo.isBlank()) {
            return new Builder().build();
        }
        String[] parts = rawDeviceInfo.split("/", 4);
        Builder builder = new Builder();
        if (parts.length > 0 && !parts[0].isBlank()) builder.platform(parts[0]);
        if (parts.length > 1 && !parts[1].isBlank()) builder.osVersion(parts[1]);
        if (parts.length > 2 && !parts[2].isBlank()) builder.appVersion(parts[2]);
        if (parts.length > 3 && !parts[3].isBlank()) builder.deviceModel(parts[3]);
        builder.deviceName(rawDeviceInfo);
        builder.deviceId(fingerprint(rawDeviceInfo));
        return builder.build();
    }

    /**
     * AUTH-005: Create a simple DeviceInfo with just platform info.
     */
    public static DeviceInfo simple(String platform) {
        return new Builder().platform(platform).deviceName(platform + "-device").build();
    }

    // -- Getters --
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public String getPlatform() { return platform; }
    public String getOsVersion() { return osVersion; }
    public String getAppVersion() { return appVersion; }
    public String getDeviceModel() { return deviceModel; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceInfo that)) return false;
        return Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId);
    }

    @Override
    public String toString() {
        return "DeviceInfo{deviceId='" + deviceId + "', platform='" + platform + "', name='" + deviceName + "'}";
    }

    // -- Builder --
    public static class Builder {
        private String deviceId;
        private String deviceName;
        private String platform;
        private String osVersion;
        private String appVersion;
        private String deviceModel;
        private Instant firstSeenAt;
        private Instant lastSeenAt;

        public Builder deviceId(String deviceId) { this.deviceId = deviceId; return this; }
        public Builder deviceName(String deviceName) { this.deviceName = deviceName; return this; }
        public Builder platform(String platform) { this.platform = platform; return this; }
        public Builder osVersion(String osVersion) { this.osVersion = osVersion; return this; }
        public Builder appVersion(String appVersion) { this.appVersion = appVersion; return this; }
        public Builder deviceModel(String deviceModel) { this.deviceModel = deviceModel; return this; }
        public Builder firstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; return this; }
        public Builder lastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; return this; }

        public DeviceInfo build() { return new DeviceInfo(this); }
    }
}
