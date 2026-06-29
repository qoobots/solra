package com.solra.not.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Notification 聚合根单元测试。
 * 验证通知生命周期状态机。
 */
@DisplayName("Notification — 通知聚合根测试")
class NotificationTest {

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification("notif-1", "user-1",
                NotificationType.SPACE_INVITE, NotificationPriority.NORMAL,
                "欢迎加入", "你被邀请加入创作空间");
    }

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("创建后状态应为 PENDING")
        void initialStatusIsPending() {
            assertEquals(NotificationStatus.UNREAD, notification.getStatus());
        }

        @Test
        @DisplayName("创建后 createdAt 不为空")
        void createdAtIsSet() {
            assertNotNull(notification.getCreatedAt());
        }

        @Test
        @DisplayName("初始 readAt 应为 null")
        void readAtIsNull() {
            assertNull(notification.getReadAt());
        }
    }

    @Nested
    @DisplayName("sent() — 标记已发送")
    class Sent {

        @Test
        @DisplayName("PENDING → SENT")
        void pendingToSent() {
            notification.sent();
            assertEquals(NotificationStatus.SENT, notification.getStatus());
        }

        @Test
        @DisplayName("非PENDING状态调用 sent 不生效")
        void nonPendingToSentNoEffect() {
            notification.setStatus(NotificationStatus.READ);
            notification.sent();
            assertEquals(NotificationStatus.READ, notification.getStatus());
        }
    }

    @Nested
    @DisplayName("delivered() — 标记已送达")
    class Delivered {

        @Test
        @DisplayName("SENT → DELIVERED")
        void sentToDelivered() {
            notification.setStatus(NotificationStatus.SENT);
            notification.delivered();
            assertEquals(NotificationStatus.DELIVERED, notification.getStatus());
        }

        @Test
        @DisplayName("非SENT状态调用 delivered 不生效")
        void nonSentToDeliveredNoEffect() {
            notification.setStatus(NotificationStatus.READ);
            notification.delivered();
            assertEquals(NotificationStatus.READ, notification.getStatus());
        }
    }

    @Nested
    @DisplayName("markRead() — 标记已读")
    class MarkRead {

        @Test
        @DisplayName("DELIVERED → READ 并设置 readAt")
        void deliveredToRead() {
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.markRead();
            assertEquals(NotificationStatus.READ, notification.getStatus());
            assertNotNull(notification.getReadAt());
        }

        @Test
        @DisplayName("非DELIVERED状态调用 markRead 不生效")
        void nonDeliveredToReadNoEffect() {
            notification.setStatus(NotificationStatus.SENT);
            notification.markRead();
            assertEquals(NotificationStatus.SENT, notification.getStatus());
            assertNull(notification.getReadAt());
        }
    }

    @Nested
    @DisplayName("dismiss() — 忽略通知")
    class Dismiss {

        @Test
        @DisplayName("任意状态都可以 dismiss")
        void anyStatusCanDismiss() {
            notification.setStatus(NotificationStatus.READ);
            notification.dismiss();
            assertEquals(NotificationStatus.DISMISSED, notification.getStatus());
        }
    }

    @Nested
    @DisplayName("isExpired() — 过期判断")
    class IsExpired {

        @Test
        @DisplayName("未设置过期时间 → 永不过期")
        void noExpiryNeverExpires() {
            assertFalse(notification.isExpired());
        }

        @Test
        @DisplayName("已过期的通知返回 true")
        void pastExpiryReturnsTrue() {
            notification.setExpiresAt(Instant.now().minusSeconds(3600));
            assertTrue(notification.isExpired());
        }

        @Test
        @DisplayName("未到期的通知返回 false")
        void futureExpiryReturnsFalse() {
            notification.setExpiresAt(Instant.now().plusSeconds(86400));
            assertFalse(notification.isExpired());
        }
    }

    @Nested
    @DisplayName("isUnread() — 未读判断")
    class IsUnread {

        @Test
        @DisplayName("PENDING 状态为未读")
        void pendingIsUnread() {
            notification.setStatus(NotificationStatus.UNREAD);
            assertTrue(notification.isUnread());
        }

        @Test
        @DisplayName("SENT 状态为未读")
        void sentIsUnread() {
            notification.setStatus(NotificationStatus.SENT);
            assertTrue(notification.isUnread());
        }

        @Test
        @DisplayName("DELIVERED 状态为未读")
        void deliveredIsUnread() {
            notification.setStatus(NotificationStatus.DELIVERED);
            assertTrue(notification.isUnread());
        }

        @Test
        @DisplayName("READ 状态为已读")
        void readIsNotUnread() {
            notification.setStatus(NotificationStatus.READ);
            assertFalse(notification.isUnread());
        }

        @Test
        @DisplayName("DISMISSED 状态为已读")
        void dismissedIsNotUnread() {
            notification.setStatus(NotificationStatus.DISMISSED);
            assertFalse(notification.isUnread());
        }
    }

    @Nested
    @DisplayName("完整生命周期")
    class FullLifecycle {

        @Test
        @DisplayName("PENDING → SENT → DELIVERED → READ 完整流程")
        void fullLifecycle() {
            assertEquals(NotificationStatus.UNREAD, notification.getStatus());
            assertTrue(notification.isUnread());

            notification.sent();
            assertEquals(NotificationStatus.SENT, notification.getStatus());
            assertTrue(notification.isUnread());

            notification.delivered();
            assertEquals(NotificationStatus.DELIVERED, notification.getStatus());
            assertTrue(notification.isUnread());

            notification.markRead();
            assertEquals(NotificationStatus.READ, notification.getStatus());
            assertFalse(notification.isUnread());
            assertNotNull(notification.getReadAt());
        }
    }
}
