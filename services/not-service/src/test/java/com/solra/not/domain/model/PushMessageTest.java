package com.solra.not.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PushMessage 实体单元测试。
 */
@DisplayName("PushMessage — 推送消息测试")
class PushMessageTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("创建后状态应为 PENDING")
        void initialStatusIsPending() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.IOS, PushProvider.APNS);
            assertEquals(PushStatus.PENDING, msg.getStatus());
        }

        @Test
        @DisplayName("关联通知ID正确")
        void linksNotification() {
            PushMessage msg = new PushMessage("push-1", "notif-123",
                    "token-abc", Platform.ANDROID, PushProvider.FCM);
            assertEquals("notif-123", msg.getNotificationId());
        }

        @Test
        @DisplayName("平台和提供商标识正确")
        void platformAndProvider() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.ANDROID, PushProvider.FCM);
            assertEquals(Platform.ANDROID, msg.getPlatform());
            assertEquals(PushProvider.FCM, msg.getPushProvider());
        }
    }

    @Nested
    @DisplayName("sent() — 标记已发送")
    class Sent {

        @Test
        @DisplayName("设置状态为 SENT 并记录消息ID和时间")
        void markAsSent() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.IOS, PushProvider.APNS);
            msg.sent("apns-msg-001");

            assertEquals(PushStatus.SENT, msg.getStatus());
            assertEquals("apns-msg-001", msg.getProviderMessageId());
            assertNotNull(msg.getSentAt());
        }
    }

    @Nested
    @DisplayName("delivered() — 标记已送达")
    class Delivered {

        @Test
        @DisplayName("设置状态为 DELIVERED 并记录送达时间")
        void markAsDelivered() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.ANDROID, PushProvider.FCM);
            msg.delivered();

            assertEquals(PushStatus.DELIVERED, msg.getStatus());
            assertNotNull(msg.getDeliveredAt());
        }
    }

    @Nested
    @DisplayName("failed() — 标记失败")
    class Failed {

        @Test
        @DisplayName("设置状态为 FAILED 并记录失败原因")
        void markAsFailed() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.WEB, PushProvider.WEBPUSH);
            msg.failed("Device token expired");

            assertEquals(PushStatus.FAILED, msg.getStatus());
            assertEquals("Device token expired", msg.getFailureReason());
        }
    }

    @Nested
    @DisplayName("完整生命周期")
    class FullLifecycle {

        @Test
        @DisplayName("PENDING → SENT → DELIVERED")
        void pendingToDelivered() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.IOS, PushProvider.APNS);

            assertEquals(PushStatus.PENDING, msg.getStatus());
            msg.sent("apns-msg-001");
            assertEquals(PushStatus.SENT, msg.getStatus());
            msg.delivered();
            assertEquals(PushStatus.DELIVERED, msg.getStatus());
        }

        @Test
        @DisplayName("PENDING → SENT → FAILED")
        void pendingToFailed() {
            PushMessage msg = new PushMessage("push-1", "notif-1",
                    "token-abc", Platform.ANDROID, PushProvider.FCM);

            msg.sent("fcm-msg-001");
            assertEquals(PushStatus.SENT, msg.getStatus());
            msg.failed("Network timeout");
            assertEquals(PushStatus.FAILED, msg.getStatus());
        }
    }
}
