package com.solra.auth.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AUTH-005: Device binding aggregate — manages a user's trusted devices.
 * Enforces the max-3-devices policy.
 */
public class DeviceBinding {

    /** Maximum number of concurrent devices per user (AUTH-005). */
    public static final int MAX_DEVICES = 3;

    private final String userId;
    private final List<DeviceInfo> devices;
    private Instant updatedAt;

    private DeviceBinding(String userId) {
        this.userId = userId;
        this.devices = new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    /**
     * Create a new empty device binding for a user.
     */
    public static DeviceBinding create(String userId) {
        return new DeviceBinding(userId);
    }

    /**
     * AUTH-005: Register a new device login. If the device already exists, update lastSeenAt.
     * If max devices reached and this is a new device, throw exception.
     *
     * @return the registered DeviceInfo
     * @throws DeviceLimitExceededException if max devices reached with a new device
     */
    public DeviceInfo registerDevice(DeviceInfo deviceInfo) {
        // Check if device already registered
        for (int i = 0; i < devices.size(); i++) {
            DeviceInfo existing = devices.get(i);
            if (existing.getDeviceId().equals(deviceInfo.getDeviceId())) {
                // Update lastSeenAt
                DeviceInfo updated = new DeviceInfo.Builder()
                        .deviceId(existing.getDeviceId())
                        .deviceName(deviceInfo.getDeviceName())
                        .platform(deviceInfo.getPlatform())
                        .osVersion(deviceInfo.getOsVersion() != null ? deviceInfo.getOsVersion() : existing.getOsVersion())
                        .appVersion(deviceInfo.getAppVersion() != null ? deviceInfo.getAppVersion() : existing.getAppVersion())
                        .deviceModel(deviceInfo.getDeviceModel() != null ? deviceInfo.getDeviceModel() : existing.getDeviceModel())
                        .firstSeenAt(existing.getFirstSeenAt())
                        .lastSeenAt(Instant.now())
                        .build();
                devices.set(i, updated);
                this.updatedAt = Instant.now();
                return updated;
            }
        }

        // New device
        if (devices.size() >= MAX_DEVICES) {
            throw new DeviceLimitExceededException(
                    "Maximum " + MAX_DEVICES + " devices reached. Remove an existing device first.");
        }

        DeviceInfo registered = new DeviceInfo.Builder()
                .deviceId(deviceInfo.getDeviceId())
                .deviceName(deviceInfo.getDeviceName())
                .platform(deviceInfo.getPlatform())
                .osVersion(deviceInfo.getOsVersion())
                .appVersion(deviceInfo.getAppVersion())
                .deviceModel(deviceInfo.getDeviceModel())
                .firstSeenAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build();
        devices.add(registered);
        this.updatedAt = Instant.now();
        return registered;
    }

    /**
     * AUTH-005: Remove a device by its deviceId.
     * @return true if removed, false if device not found
     */
    public boolean removeDevice(String deviceId) {
        boolean removed = devices.removeIf(d -> d.getDeviceId().equals(deviceId));
        if (removed) {
            this.updatedAt = Instant.now();
        }
        return removed;
    }

    /**
     * AUTH-005: Check if a device is registered for this user.
     */
    public boolean hasDevice(String deviceId) {
        return devices.stream().anyMatch(d -> d.getDeviceId().equals(deviceId));
    }

    /**
     * AUTH-005: Get the number of currently registered devices.
     */
    public int deviceCount() {
        return devices.size();
    }

    /**
     * AUTH-005: Check if the user can register another device.
     */
    public boolean canRegisterDevice() {
        return devices.size() < MAX_DEVICES;
    }

    // -- Getters --
    public String getUserId() { return userId; }
    public List<DeviceInfo> getDevices() { return Collections.unmodifiableList(devices); }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Exception thrown when the device limit is exceeded.
     */
    public static class DeviceLimitExceededException extends RuntimeException {
        public DeviceLimitExceededException(String message) {
            super(message);
        }
    }
}
