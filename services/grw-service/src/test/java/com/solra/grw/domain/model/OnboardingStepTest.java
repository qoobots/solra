package com.solra.grw.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OnboardingStep 值对象 单元测试")
class OnboardingStepTest {

    @Nested
    @DisplayName("构造 — 创建引导步骤")
    class ConstructionTests {

        @Test
        @DisplayName("构造后初始状态")
        void initializesCorrectly() {
            OnboardingStep s = new OnboardingStep(1, OnboardingStepType.WELCOME);

            assertEquals(1, s.getStepNumber());
            assertEquals(OnboardingStepType.WELCOME, s.getStepType());
            assertNull(s.getCompletedAt());
            assertNull(s.getSkippedAt());
            assertFalse(s.isCompleted());
            assertFalse(s.isSkipped());
        }
    }

    // ========== complete ==========

    @Nested
    @DisplayName("complete — 完成步骤")
    class CompleteTests {

        @Test
        @DisplayName("完成设置 completedAt")
        void setsCompletedAt() {
            OnboardingStep s = new OnboardingStep(1, OnboardingStepType.WELCOME);
            s.complete();

            assertNotNull(s.getCompletedAt());
            assertTrue(s.isCompleted());
            assertFalse(s.isSkipped());
        }
    }

    // ========== skip ==========

    @Nested
    @DisplayName("skip — 跳过步骤")
    class SkipTests {

        @Test
        @DisplayName("跳过设置 skippedAt")
        void setsSkippedAt() {
            OnboardingStep s = new OnboardingStep(2, OnboardingStepType.AVATAR_INTRODUCTION);
            s.skip();

            assertNotNull(s.getSkippedAt());
            assertTrue(s.isSkipped());
            assertFalse(s.isCompleted());
        }
    }

    // ========== 组合操作 ==========

    @Nested
    @DisplayName("组合操作")
    class CombinedTests {

        @Test
        @DisplayName("先完成再跳过 → 两者皆有标记")
        void completeThenSkip() {
            OnboardingStep s = new OnboardingStep(1, OnboardingStepType.WELCOME);
            s.complete();
            s.skip();

            assertTrue(s.isCompleted());
            assertTrue(s.isSkipped());
        }

        @Test
        @DisplayName("不同步骤类型创建")
        void differentStepTypes() {
            for (OnboardingStepType type : OnboardingStepType.values()) {
                OnboardingStep s = new OnboardingStep(1, type);
                assertEquals(type, s.getStepType());
            }
        }
    }
}
