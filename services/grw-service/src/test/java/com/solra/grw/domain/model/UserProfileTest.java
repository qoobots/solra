package com.solra.grw.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserProfile 聚合根 单元测试")
class UserProfileTest {

    @Nested
    @DisplayName("构造 — 创建用户画像")
    class ConstructionTests {

        @Test
        @DisplayName("新用户画像初始化默认值")
        void initializesWithDefaults() {
            UserProfile p = new UserProfile("user1");

            assertEquals("user1", p.getUserId());
            assertEquals(0.0, p.getPresenceScore(), 0.001);
            assertEquals(FaithLevel.SEEKER, p.getFaithLevel());
            assertEquals(0, p.getTotalInteractions());
            assertEquals(0, p.getSpacesVisited());
            assertEquals(0, p.getConversationsHad());
            assertEquals(0, p.getFriendsCount());
            assertNotNull(p.getCreatedAt());
            assertNotNull(p.getUpdatedAt());
            assertNotNull(p.getLastActiveAt());
            assertFalse(p.isOnboardingCompleted());
        }
    }

    // ========== recordInteraction ==========

    @Nested
    @DisplayName("recordInteraction — 记录互动")
    class RecordInteractionTests {

        @Test
        @DisplayName("每次调用增加 totalInteractions")
        void incrementsTotalInteractions() {
            UserProfile p = new UserProfile("u1");

            p.recordInteraction();
            assertEquals(1, p.getTotalInteractions());

            p.recordInteraction();
            p.recordInteraction();
            assertEquals(3, p.getTotalInteractions());
        }

        @Test
        @DisplayName("更新活跃时间")
        void updatesLastActiveAt() {
            UserProfile p = new UserProfile("u1");
            var before = p.getLastActiveAt();

            p.recordInteraction();

            assertNotNull(p.getLastActiveAt());
        }
    }

    // ========== updateFaithLevel ==========

    @Nested
    @DisplayName("updateFaithLevel — 更新信誉等级")
    class UpdateFaithLevelTests {

        @Test
        @DisplayName("等级变化 → 返回 true")
        void levelChangedReturnsTrue() {
            UserProfile p = new UserProfile("u1");
            boolean changed = p.updateFaithLevel(FaithLevel.BELIEVER);

            assertTrue(changed);
            assertEquals(FaithLevel.BELIEVER, p.getFaithLevel());
        }

        @Test
        @DisplayName("等级未变 → 返回 false")
        void levelUnchangedReturnsFalse() {
            UserProfile p = new UserProfile("u1");
            boolean changed = p.updateFaithLevel(FaithLevel.SEEKER);

            assertFalse(changed);
            assertEquals(FaithLevel.SEEKER, p.getFaithLevel());
        }
    }

    // ========== adjustPresenceScore ==========

    @Nested
    @DisplayName("adjustPresenceScore — 调整活跃度分数")
    class AdjustPresenceScoreTests {

        @Test
        @DisplayName("正向增加分数")
        void positiveDelta() {
            UserProfile p = new UserProfile("u1");
            p.adjustPresenceScore(10.0);

            assertEquals(10.0, p.getPresenceScore(), 0.001);
        }

        @Test
        @DisplayName("连续累加")
        void accumulateScore() {
            UserProfile p = new UserProfile("u1");
            p.adjustPresenceScore(5.0);
            p.adjustPresenceScore(3.0);

            assertEquals(8.0, p.getPresenceScore(), 0.001);
        }

        @Test
        @DisplayName("负值不会低于 0")
        void negativeNotBelowZero() {
            UserProfile p = new UserProfile("u1");
            p.adjustPresenceScore(2.0);
            p.adjustPresenceScore(-5.0);

            assertEquals(0.0, p.getPresenceScore(), 0.001);
        }

        @Test
        @DisplayName("更新活跃时间")
        void updatesLastActiveAt() {
            UserProfile p = new UserProfile("u1");
            p.adjustPresenceScore(1.0);

            assertNotNull(p.getLastActiveAt());
        }
    }

    // ========== increment 方法 ==========

    @Nested
    @DisplayName("increment 方法 — 各类计数递增")
    class IncrementTests {

        @Test
        @DisplayName("incrementSpacesVisited 递增")
        void incrementSpaces() {
            UserProfile p = new UserProfile("u1");
            p.incrementSpacesVisited();
            p.incrementSpacesVisited();

            assertEquals(2, p.getSpacesVisited());
        }

        @Test
        @DisplayName("incrementConversations 递增")
        void incrementConversations() {
            UserProfile p = new UserProfile("u1");
            p.incrementConversations();

            assertEquals(1, p.getConversationsHad());
        }

        @Test
        @DisplayName("incrementFriends 递增")
        void incrementFriends() {
            UserProfile p = new UserProfile("u1");
            p.incrementFriends();
            p.incrementFriends();
            p.incrementFriends();

            assertEquals(3, p.getFriendsCount());
        }
    }

    // ========== Onboarding 相关 ==========

    @Nested
    @DisplayName("Onboarding 引导流程")
    class OnboardingTests {

        @Test
        @DisplayName("advanceOnboardingStep 更新步骤")
        void advancesOnboardingStep() {
            UserProfile p = new UserProfile("u1");
            p.advanceOnboardingStep("WELCOME");

            assertEquals("WELCOME", p.getCurrentOnboardingStep());
        }

        @Test
        @DisplayName("completeOnboarding 标记完成")
        void completesOnboarding() {
            UserProfile p = new UserProfile("u1");
            p.advanceOnboardingStep("NOTIFICATION_ENABLE");
            p.completeOnboarding();

            assertTrue(p.isOnboardingCompleted());
            assertNull(p.getCurrentOnboardingStep());
        }
    }
}
