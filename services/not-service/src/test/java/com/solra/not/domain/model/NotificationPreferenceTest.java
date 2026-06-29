package com.solra.not.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationPreference 实体单元测试。
 */
@DisplayName("NotificationPreference — 通知偏好测试")
class NotificationPreferenceTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("创建偏好时 enabled 由参数决定")
        void enabledByConstructor() {
            NotificationPreference pref = new NotificationPreference(
                    "pref-1", "user-1", NotificationType.SPACE_INVITE,
                    DeliveryChannel.PUSH, true);
            assertTrue(pref.isEnabled());
        }

        @Test
        @DisplayName("创建偏好时 disabled")
        void disabledByConstructor() {
            NotificationPreference pref = new NotificationPreference(
                    "pref-1", "user-1", NotificationType.SYSTEM_ALERT,
                    DeliveryChannel.IN_APP, false);
            assertFalse(pref.isEnabled());
        }
    }

    @Nested
    @DisplayName("toggle() — 切换开关")
    class Toggle {

        @Test
        @DisplayName("enabled → disabled")
        void enabledToDisabled() {
            NotificationPreference pref = createEnabledPreference();
            pref.toggle();
            assertFalse(pref.isEnabled());
        }

        @Test
        @DisplayName("disabled → enabled")
        void disabledToEnabled() {
            NotificationPreference pref = createDisabledPreference();
            pref.toggle();
            assertTrue(pref.isEnabled());
        }

        @Test
        @DisplayName("多次切换")
        void multipleToggles() {
            NotificationPreference pref = createEnabledPreference();
            pref.toggle(); // false
            pref.toggle(); // true
            pref.toggle(); // false
            assertFalse(pref.isEnabled());
        }
    }

    @Nested
    @DisplayName("enable() — 启用")
    class Enable {

        @Test
        @DisplayName("启用后 isEnabled 为 true")
        void becomesEnabled() {
            NotificationPreference pref = createDisabledPreference();
            pref.enable();
            assertTrue(pref.isEnabled());
        }

        @Test
        @DisplayName("已启用的再次启用仍为 true")
        void alreadyEnabledStaysTrue() {
            NotificationPreference pref = createEnabledPreference();
            pref.enable();
            assertTrue(pref.isEnabled());
        }
    }

    @Nested
    @DisplayName("disable() — 禁用")
    class Disable {

        @Test
        @DisplayName("禁用后 isEnabled 为 false")
        void becomesDisabled() {
            NotificationPreference pref = createEnabledPreference();
            pref.disable();
            assertFalse(pref.isEnabled());
        }

        @Test
        @DisplayName("已禁用的再次禁用仍为 false")
        void alreadyDisabledStaysFalse() {
            NotificationPreference pref = createDisabledPreference();
            pref.disable();
            assertFalse(pref.isEnabled());
        }
    }

    @Nested
    @DisplayName("安静时段")
    class QuietHours {

        @Test
        @DisplayName("设置安静时段")
        void setQuietHours() {
            NotificationPreference pref = createEnabledPreference();
            pref.setQuietHoursStart("22:00");
            pref.setQuietHoursEnd("08:00");

            assertEquals("22:00", pref.getQuietHoursStart());
            assertEquals("08:00", pref.getQuietHoursEnd());
        }
    }

    @Nested
    @DisplayName("多通知类型")
    class MultiType {

        @Test
        @DisplayName("SPACE_INVITE 类型")
        void spaceInviteType() {
            NotificationPreference pref = new NotificationPreference(
                    "pref-1", "user-1", NotificationType.SPACE_INVITE,
                    DeliveryChannel.PUSH, true);
            assertEquals(NotificationType.SPACE_INVITE, pref.getNotificationType());
        }

        @Test
        @DisplayName("FRIEND_REQUEST 类型")
        void friendRequestType() {
            NotificationPreference pref = new NotificationPreference(
                    "pref-2", "user-1", NotificationType.FRIEND_REQUEST,
                    DeliveryChannel.IN_APP, true);
            assertEquals(NotificationType.FRIEND_REQUEST, pref.getNotificationType());
        }

        @Test
        @DisplayName("SYSTEM_ALERT 类型")
        void systemAlertType() {
            NotificationPreference pref = new NotificationPreference(
                    "pref-3", "user-1", NotificationType.SYSTEM_ALERT,
                    DeliveryChannel.EMAIL, true);
            assertEquals(NotificationType.SYSTEM_ALERT, pref.getNotificationType());
        }
    }

    private NotificationPreference createEnabledPreference() {
        return new NotificationPreference("pref-1", "user-1",
                NotificationType.SPACE_INVITE, DeliveryChannel.PUSH, true);
    }

    private NotificationPreference createDisabledPreference() {
        return new NotificationPreference("pref-2", "user-1",
                NotificationType.SYSTEM_ALERT, DeliveryChannel.IN_APP, false);
    }
}
