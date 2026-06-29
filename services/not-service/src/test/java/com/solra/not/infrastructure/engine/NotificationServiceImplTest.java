package com.solra.not.infrastructure.engine;

import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.*;
import com.solra.not.domain.service.SmartPushEngine;
import com.solra.not.infrastructure.push.PushDispatcher;
import com.solra.not.infrastructure.push.PushProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationServiceImpl 单元测试。
 * NOT-001 推送通知系统的核心业务逻辑测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl — 通知领域服务测试")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepo;
    @Mock private DeviceRegistrationRepository deviceRepo;
    @Mock private PushMessageRepository pushMessageRepo;
    @Mock private NotificationPreferenceRepository prefRepo;
    @Mock private PushDispatcher pushDispatcher;
    @Mock private SmartPushEngine smartPushEngine;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(
                notificationRepo, deviceRepo, pushMessageRepo, prefRepo,
                pushDispatcher, smartPushEngine);
    }

    @Nested
    @DisplayName("send — 发送通知")
    class SendNotification {

        @Test
        @DisplayName("向单个用户发送通知，无设备时应返回1条通知")
        void sendToSingleUserNoDevices() {
            Notification template = createTemplate();
            when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceRepo.findByUserId("user-1")).thenReturn(Collections.emptyList());

            List<Notification> results = service.send(List.of("user-1"), template);

            assertEquals(1, results.size());
            assertEquals("user-1", results.get(0).getUserId());
            assertEquals(NotificationStatus.SENT, results.get(0).getStatus());
        }

        @Test
        @DisplayName("向多个用户发送通知")
        void sendToMultipleUsers() {
            Notification template = createTemplate();
            when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceRepo.findByUserId(anyString())).thenReturn(Collections.emptyList());

            List<Notification> results = service.send(List.of("user-1", "user-2", "user-3"), template);

            assertEquals(3, results.size());
        }

        @Test
        @DisplayName("通知创建后应有独立ID")
        void notificationHasUniqueId() {
            Notification template = createTemplate();
            when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceRepo.findByUserId(anyString())).thenReturn(Collections.emptyList());

            List<Notification> results = service.send(List.of("user-1", "user-2"), template);

            assertNotNull(results.get(0).getNotificationId());
            assertNotNull(results.get(1).getNotificationId());
            assertNotEquals(results.get(0).getNotificationId(), results.get(1).getNotificationId());
        }

        @Test
        @DisplayName("通知应继承模板的 imageUrl 和 deepLink")
        void inheritsTemplateFields() {
            Notification template = createTemplate();
            template.setImageUrl("https://img.solra.io/avatar.png");
            template.setDeepLink("solra://space/123");

            when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceRepo.findByUserId(anyString())).thenReturn(Collections.emptyList());

            List<Notification> results = service.send(List.of("user-1"), template);

            assertEquals("https://img.solra.io/avatar.png", results.get(0).getImageUrl());
            assertEquals("solra://space/123", results.get(0).getDeepLink());
        }

        @Test
        @DisplayName("通知应继承模板的 expiresAt")
        void inheritsExpiresAt() {
            Instant expiry = Instant.now().plusSeconds(86400);
            Notification template = createTemplate();
            template.setExpiresAt(expiry);

            when(notificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(deviceRepo.findByUserId(anyString())).thenReturn(Collections.emptyList());

            List<Notification> results = service.send(List.of("user-1"), template);

            assertEquals(expiry, results.get(0).getExpiresAt());
        }

        @Test
        @DisplayName("空用户列表应返回空结果")
        void emptyUserList() {
            Notification template = createTemplate();

            List<Notification> results = service.send(Collections.emptyList(), template);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("getInbox — 获取收件箱")
    class GetInbox {

        @Test
        @DisplayName("正常获取收件箱")
        void getInboxNormal() {
            Notification notif = createPersistedNotification("user-1");
            when(notificationRepo.findByUserId("user-1", 0, 20))
                    .thenReturn(List.of(notif));

            List<Notification> inbox = service.getInbox("user-1", 0, 20);

            assertEquals(1, inbox.size());
            assertEquals("user-1", inbox.get(0).getUserId());
        }

        @Test
        @DisplayName("空收件箱")
        void emptyInbox() {
            when(notificationRepo.findByUserId("user-1", 0, 20))
                    .thenReturn(Collections.emptyList());

            List<Notification> inbox = service.getInbox("user-1", 0, 20);

            assertTrue(inbox.isEmpty());
        }

        @Test
        @DisplayName("分页查询")
        void pagedInbox() {
            when(notificationRepo.findByUserId("user-1", 10, 5))
                    .thenReturn(Collections.emptyList());

            List<Notification> inbox = service.getInbox("user-1", 10, 5);

            assertNotNull(inbox);
            verify(notificationRepo).findByUserId("user-1", 10, 5);
        }
    }

    @Nested
    @DisplayName("getUnreadCount — 获取未读计数")
    class GetUnreadCount {

        @Test
        @DisplayName("返回未读计数")
        void returnsUnreadCount() {
            when(notificationRepo.getUnreadCount("user-1")).thenReturn(5L);

            long count = service.getUnreadCount("user-1");

            assertEquals(5L, count);
        }

        @Test
        @DisplayName("无未读通知时返回0")
        void noUnread() {
            when(notificationRepo.getUnreadCount("user-1")).thenReturn(0L);

            long count = service.getUnreadCount("user-1");

            assertEquals(0L, count);
        }
    }

    private Notification createTemplate() {
        Notification n = new Notification();
        n.setType(NotificationType.SPACE_INVITE);
        n.setPriority(NotificationPriority.NORMAL);
        n.setTitle("测试标题");
        n.setBody("测试内容");
        return n;
    }

    private Notification createPersistedNotification(String userId) {
        Notification n = new Notification("notif-1", userId,
                NotificationType.SPACE_INVITE, NotificationPriority.NORMAL,
                "标题", "内容");
        n.setStatus(NotificationStatus.DELIVERED);
        return n;
    }
}
