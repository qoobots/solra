package com.solra.not.domain.service;

import com.solra.not.domain.model.NotificationPreference;
import com.solra.not.domain.model.NotificationType;
import com.solra.not.domain.repository.NotificationPreferenceRepository;
import com.solra.not.domain.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * SmartPushEngine 单元测试。
 * NOT-003: 智能推送策略。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmartPushEngine — 智能推送引擎测试")
class SmartPushEngineTest {

    @Mock private NotificationPreferenceRepository prefRepo;
    @Mock private NotificationRepository notificationRepo;

    private SmartPushEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SmartPushEngine(prefRepo, notificationRepo);
    }

    @Nested
    @DisplayName("evaluate — 推送决策")
    class Evaluate {

        @Test
        @DisplayName("URGENT 优先级应绕过所有限制")
        void urgentBypassesAll() {
            SmartPushEngine.PushDecision decision = engine.evaluate(
                    "user-1", NotificationType.INTERACTION, true);
            assertEquals(SmartPushEngine.Decision.ALLOW, decision.decision());
        }

        @Test
        @DisplayName("用户禁用某类型通知时应阻止")
        void blockedByPreference() {
            NotificationPreference pref = new NotificationPreference();
            pref.setEnabled(false);

            when(prefRepo.findByUserIdAndType("user-1", "INTERACTION"))
                    .thenReturn(Optional.of(pref));

            SmartPushEngine.PushDecision decision = engine.evaluate(
                    "user-1", NotificationType.INTERACTION, false);
            assertEquals(SmartPushEngine.Decision.BLOCKED_BY_PREFERENCE, decision.decision());
        }

        @Test
        @DisplayName("无偏好设置时应允许推送")
        void noPreferenceAllows() {
            when(prefRepo.findByUserIdAndType("user-1", "INTERACTION"))
                    .thenReturn(Optional.empty());

            SmartPushEngine.PushDecision decision = engine.evaluate(
                    "user-1", NotificationType.INTERACTION, false);
            assertEquals(SmartPushEngine.Decision.ALLOW, decision.decision());
        }

        @Test
        @DisplayName("用户启用偏好且渠道为 PUSH 时应允许")
        void enabledPreferenceAllows() {
            NotificationPreference pref = new NotificationPreference();
            pref.setEnabled(true);

            when(prefRepo.findByUserIdAndType("user-1", "INTERACTION"))
                    .thenReturn(Optional.of(pref));

            SmartPushEngine.PushDecision decision = engine.evaluate(
                    "user-1", NotificationType.INTERACTION, false);
            assertEquals(SmartPushEngine.Decision.ALLOW, decision.decision());
        }

        @Test
        @DisplayName("频率限制：达到每日上限时应阻止")
        void rateLimitBlocks() {
            when(prefRepo.findByUserIdAndType("user-1", "INTERACTION"))
                    .thenReturn(Optional.empty());

            // 发送达到上限次数
            int limit = engine.getDailyLimit(NotificationType.INTERACTION);
            for (int i = 0; i < limit; i++) {
                SmartPushEngine.PushDecision d = engine.evaluate(
                        "user-1", NotificationType.INTERACTION, false);
                assertEquals(SmartPushEngine.Decision.ALLOW, d.decision(), "Attempt " + i);
            }

            // 第 limit+1 次应被阻止
            SmartPushEngine.PushDecision blocked = engine.evaluate(
                    "user-1", NotificationType.INTERACTION, false);
            assertEquals(SmartPushEngine.Decision.BLOCKED_BY_RATE_LIMIT, blocked.decision());
        }
    }

    @Nested
    @DisplayName("isInQuietHours — 静默时段检测")
    class QuietHours {

        @Test
        @DisplayName("无静默时段设置时返回 false")
        void noQuietHoursReturnsFalse() {
            when(prefRepo.findByUserId("user-1")).thenReturn(Collections.emptyList());
            assertFalse(engine.isInQuietHours("user-1"));
        }
    }

    @Nested
    @DisplayName("getDailyLimit — 每日限制")
    class DailyLimits {

        @Test
        @DisplayName("INTERACTION 每日限制为 5")
        void interactionLimit() {
            assertEquals(5, engine.getDailyLimit(NotificationType.INTERACTION));
        }

        @Test
        @DisplayName("SYSTEM_ANNOUNCEMENT 每日限制为 1")
        void announcementLimit() {
            assertEquals(1, engine.getDailyLimit(NotificationType.SYSTEM_ANNOUNCEMENT));
        }

        @Test
        @DisplayName("未知类型默认限制为 3")
        void defaultLimit() {
            assertEquals(3, engine.getDailyLimit(NotificationType.PURCHASE_SUCCESS));
        }
    }

    @Nested
    @DisplayName("resetDailyCounters — 重置计数")
    class ResetCounters {

        @Test
        @DisplayName("重置后计数应为空")
        void resetClearsCounters() {
            when(prefRepo.findByUserIdAndType("user-1", "INTERACTION"))
                    .thenReturn(Optional.empty());

            engine.evaluate("user-1", NotificationType.INTERACTION, false);
            engine.resetDailyCounters("user-1");

            assertTrue(engine.getUserDailyStats("user-1").isEmpty());
        }
    }
}
