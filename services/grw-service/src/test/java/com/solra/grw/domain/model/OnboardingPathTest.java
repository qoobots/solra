package com.solra.grw.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OnboardingPath 实体 单元测试")
class OnboardingPathTest {

    @Nested
    @DisplayName("构造 — 创建引导路径")
    class ConstructionTests {

        @Test
        @DisplayName("构造后默认值")
        void defaults() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);

            assertEquals("p1", p.getPathId());
            assertEquals("u1", p.getUserId());
            assertEquals(6, p.getTotalSteps());
            assertEquals(0, p.getCurrentStep());
            assertEquals(OnboardingStatus.NOT_STARTED, p.getStatus());
            assertNotNull(p.getStartTime());
            assertNotNull(p.getStepHistory());
            assertTrue(p.getStepHistory().isEmpty());
        }

        @Test
        @DisplayName("无参构造 stepHistory 不为 null")
        void noArgConstructor() {
            OnboardingPath p = new OnboardingPath();
            assertNotNull(p.getStepHistory());
        }
    }

    // ========== start ==========

    @Nested
    @DisplayName("start — 开始引导")
    class StartTests {

        @Test
        @DisplayName("开始引导 → IN_PROGRESS, currentStep=1")
        void startsOnboarding() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.start();

            assertEquals(OnboardingStatus.IN_PROGRESS, p.getStatus());
            assertEquals(1, p.getCurrentStep());
        }
    }

    // ========== recordStep ==========

    @Nested
    @DisplayName("recordStep — 记录步骤")
    class RecordStepTests {

        @Test
        @DisplayName("完成步骤追加到历史")
        void completedStepAppended() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            OnboardingStep s = new OnboardingStep(1, OnboardingStepType.WELCOME);
            s.complete();

            p.recordStep(s);

            assertEquals(1, p.getStepHistory().size());
            assertEquals(2, p.getCurrentStep()); // stepNumber(1) + 1
        }

        @Test
        @DisplayName("完成最后一步 → 状态 COMPLETED")
        void finalStepCompletes() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            OnboardingStep s = new OnboardingStep(6, OnboardingStepType.NOTIFICATION_ENABLE);
            s.complete();

            p.recordStep(s);

            assertEquals(OnboardingStatus.COMPLETED, p.getStatus());
            assertNotNull(p.getCompletedAt());
        }

        @Test
        @DisplayName("跳过步骤（无 completedAt）→ currentStep 不变")
        void skippedStepNotAdvanced() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.start(); // currentStep=1
            OnboardingStep s = new OnboardingStep(1, OnboardingStepType.WELCOME);
            s.skip(); // no completedAt

            p.recordStep(s);

            assertTrue(p.getStepHistory().get(0).isSkipped());
            // currentStep unchanged because completedAt is null
        }
    }

    // ========== complete / abandon ==========

    @Nested
    @DisplayName("complete / abandon — 状态变更")
    class StateChangeTests {

        @Test
        @DisplayName("complete → COMPLETED + 记录完成时间")
        void completes() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.complete();

            assertEquals(OnboardingStatus.COMPLETED, p.getStatus());
            assertNotNull(p.getCompletedAt());
            assertTrue(p.isCompleted());
        }

        @Test
        @DisplayName("abandon → ABANDONED")
        void abandons() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.abandon();

            assertEquals(OnboardingStatus.ABANDONED, p.getStatus());
            assertFalse(p.isCompleted());
        }
    }

    // ========== getProgressPercentage ==========

    @Nested
    @DisplayName("getProgressPercentage — 进度百分比")
    class ProgressPercentageTests {

        @Test
        @DisplayName("totalSteps=0 → 0.0%")
        void zeroStepsReturnsZero() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 0);

            assertEquals(0.0, p.getProgressPercentage(), 0.001);
        }

        @Test
        @DisplayName("3/6 完成 → 50.0%")
        void halfCompleted() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.setStepHistory(new ArrayList<>());
            for (int i = 1; i <= 3; i++) {
                OnboardingStep s = new OnboardingStep(i, OnboardingStepType.WELCOME);
                s.complete();
                p.getStepHistory().add(s);
            }

            assertEquals(50.0, p.getProgressPercentage(), 0.001);
        }

        @Test
        @DisplayName("全部完成 → 100.0%")
        void fullyCompleted() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.setStepHistory(new ArrayList<>());
            for (int i = 1; i <= 6; i++) {
                OnboardingStep s = new OnboardingStep(i, OnboardingStepType.WELCOME);
                s.complete();
                p.getStepHistory().add(s);
            }

            assertEquals(100.0, p.getProgressPercentage(), 0.001);
        }

        @Test
        @DisplayName("仅有跳过步骤 → 0.0%")
        void onlySkippedSteps() {
            OnboardingPath p = new OnboardingPath("p1", "u1", 6);
            p.setStepHistory(new ArrayList<>());
            for (int i = 1; i <= 3; i++) {
                OnboardingStep s = new OnboardingStep(i, OnboardingStepType.WELCOME);
                s.skip();
                p.getStepHistory().add(s);
            }

            assertEquals(0.0, p.getProgressPercentage(), 0.001);
        }
    }
}
