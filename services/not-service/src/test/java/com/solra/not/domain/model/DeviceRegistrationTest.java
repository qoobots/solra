package com.solra.not.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeviceRegistration 实体单元测试。
 */
@DisplayName("DeviceRegistration — 设备注册测试")
class DeviceRegistrationTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("创建后状态应为 ACTIVE")
        void initialStatusIsActive() {
            DeviceRegistration reg = new DeviceRegistration("reg-1", "user-1",
                    "token-abc", Platform.IOS, PushProvider.APNS, "iPhone 15");
            assertEquals(DeviceStatus.ACTIVE, reg.getStatus());
            assertTrue(reg.isActive());
        }

        @Test
        @DisplayName("创建后时间戳不为空")
        void timestampsAreSet() {
            DeviceRegistration reg = new DeviceRegistration("reg-1", "user-1",
                    "token-abc", Platform.ANDROID, PushProvider.FCM, "Pixel 8");
            assertNotNull(reg.getCreatedAt());
            assertNotNull(reg.getLastUsedAt());
        }

        @Test
        @DisplayName("设备名称和令牌正确")
        void deviceInfoCorrect() {
            DeviceRegistration reg = new DeviceRegistration("reg-1", "user-1",
                    "token-xyz", Platform.WEB, PushProvider.WEBPUSH, "Chrome");

            assertEquals("Chrome", reg.getDeviceName());
            assertEquals("token-xyz", reg.getDeviceToken());
        }
    }

    @Nested
    @DisplayName("deactivate() — 停用设备")
    class Deactivate {

        @Test
        @DisplayName("状态变为 INACTIVE")
        void becomesInactive() {
            DeviceRegistration reg = createActiveDevice();
            reg.deactivate();
            assertEquals(DeviceStatus.INACTIVE, reg.getStatus());
            assertFalse(reg.isActive());
        }
    }

    @Nested
    @DisplayName("unregister() — 注销设备")
    class Unregister {

        @Test
        @DisplayName("状态变为 UNREGISTERED")
        void becomesUnregistered() {
            DeviceRegistration reg = createActiveDevice();
            reg.unregister();
            assertEquals(DeviceStatus.UNREGISTERED, reg.getStatus());
            assertFalse(reg.isActive());
        }
    }

    @Nested
    @DisplayName("isActive() — 活跃判断")
    class IsActive {

        @Test
        @DisplayName("ACTIVE 返回 true")
        void activeReturnsTrue() {
            DeviceRegistration reg = createActiveDevice();
            assertTrue(reg.isActive());
        }

        @Test
        @DisplayName("INACTIVE 返回 false")
        void inactiveReturnsFalse() {
            DeviceRegistration reg = createActiveDevice();
            reg.deactivate();
            assertFalse(reg.isActive());
        }

        @Test
        @DisplayName("UNREGISTERED 返回 false")
        void unregisteredReturnsFalse() {
            DeviceRegistration reg = createActiveDevice();
            reg.unregister();
            assertFalse(reg.isActive());
        }
    }

    @Nested
    @DisplayName("多平台支持")
    class MultiPlatform {

        @Test
        @DisplayName("iOS 设备注册")
        void iosDevice() {
            DeviceRegistration reg = new DeviceRegistration("reg-ios", "user-1",
                    "apns-token", Platform.IOS, PushProvider.APNS, "iPhone");
            assertEquals(Platform.IOS, reg.getPlatform());
            assertEquals(PushProvider.APNS, reg.getPushProvider());
        }

        @Test
        @DisplayName("Android 设备注册")
        void androidDevice() {
            DeviceRegistration reg = new DeviceRegistration("reg-and", "user-1",
                    "fcm-token", Platform.ANDROID, PushProvider.FCM, "Samsung");
            assertEquals(Platform.ANDROID, reg.getPlatform());
            assertEquals(PushProvider.FCM, reg.getPushProvider());
        }

        @Test
        @DisplayName("Web 设备注册")
        void webDevice() {
            DeviceRegistration reg = new DeviceRegistration("reg-web", "user-1",
                    "web-token", Platform.WEB, PushProvider.WEBPUSH, "Firefox");
            assertEquals(Platform.WEB, reg.getPlatform());
            assertEquals(PushProvider.WEBPUSH, reg.getPushProvider());
        }

        @Test
        @DisplayName("HarmonyOS 设备注册")
        void harmonyDevice() {
            DeviceRegistration reg = new DeviceRegistration("reg-hm", "user-1",
                    "hm-token", Platform.HARMONYOS, PushProvider.FCM, "Mate 60");
            assertEquals(Platform.HARMONYOS, reg.getPlatform());
        }
    }

    private DeviceRegistration createActiveDevice() {
        return new DeviceRegistration("reg-1", "user-1",
                "token-abc", Platform.IOS, PushProvider.APNS, "iPhone");
    }
}
