package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.OnboardingPathRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultOnboardingEngine 单元测试")
class DefaultOnboardingEngineTest {

    @Mock
    private OnboardingPathRepository onboardingPathRepo;

    private DefaultOnboardingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DefaultOnboardingEngine(onboardingPathRepo);
        lenient().when(onboardingPathRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ========== startOnboarding ==========

    @Nested
    @DisplayName("startOnboarding — 启动引导")
    class StartOnboardingTests {

        @Test
        @DisplayName("新用户 → 创建引导路径，6 步，状态 IN_PROGRESS")
        void createsNewPathForNewUser() {
            when(onboardingPathRepo.findByUserId("u1")).thenReturn(Optional.empty());

            OnboardingPath result = engine.startOnboarding("u1");

            assertNotNull(result);
            assertEquals("u1", result.getUserId());
            assertEquals(0, result.getCurrentStep());
            assertEquals(6, result.getTotalSteps());
            assertEquals(OnboardingStatus.IN_PROGRESS, result.getStatus());
            assertNotNull(result.getPathId());
            verify(onboardingPathRepo).save(any());
        }

        @Test
        @DisplayName("已有引导路径 → 直接返回现有路径")
        void returnsExistingPath() {
            OnboardingPath existing = new OnboardingPath("p1", "u2", 6);
            existing.setStatus(OnboardingStatus.IN_PROGRESS);
            when(onboardingPathRepo.findByUserId("u2")).thenReturn(Optional.of(existing));

            OnboardingPath result = engine.startOnboarding("u2");

            assertEquals("p1", result.getPathId());
            verify(onboardingPathRepo, never()).save(any());
        }
    }

    // ========== advanceStep ==========

    @Nested
    @DisplayName("advanceStep — 推进引导步骤")
    class AdvanceStepTests {

        @Test
        @DisplayName("正常推进第 1 步 → 返回 WELCOME 步骤")
        void advancesToFirstStep() {
            OnboardingPath path = createPath("u1", 0);
            when(onboardingPathRepo.findByUserId("u1")).thenReturn(Optional.of(path));

            OnboardingStep step = engine.advanceStep("u1");

            assertNotNull(step);
            assertEquals(1, step.getStepNumber());
            assertEquals(OnboardingStepType.WELCOME, step.getStepType());
            assertTrue(step.isCompleted());
            assertEquals(1, path.getCurrentStep());
            verify(onboardingPathRepo).save(path);
        }

        @Test
        @DisplayName("连续推进 6 步 → 状态变为 COMPLETED")
        void allStepsComplete() {
            OnboardingPath path = createPath("u2", 0);
            when(onboardingPathRepo.findByUserId("u2")).thenReturn(Optional.of(path));

            // advance all 6 steps
            for (int i = 0; i < 6; i++) {
                engine.advanceStep("u2");
            }

            assertEquals(6, path.getCurrentStep());
            assertEquals(OnboardingStatus.COMPLETED, path.getStatus());
            assertEquals(6, path.getStepHistory().size());
            verify(onboardingPathRepo, times(6)).save(path);
        }

        @Test
        @DisplayName("已完成引导再推进 → 抛出异常")
        void completedThrowsOnAdvance() {
            OnboardingPath path = createPath("u3", 6);
            path.setStatus(OnboardingStatus.COMPLETED);
            when(onboardingPathRepo.findByUserId("u3")).thenReturn(Optional.of(path));

            assertThrows(IllegalStateException.class, () -> engine.advanceStep("u3"));
        }

        @Test
        @DisplayName("无引导路径 → 抛出异常")
        void noPathThrowsOnAdvance() {
            when(onboardingPathRepo.findByUserId("u4")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> engine.advanceStep("u4"));
        }

        @Test
        @DisplayName("各步骤类型按顺序推进")
        void stepsInCorrectOrder() {
            OnboardingPath path = createPath("u5", 0);
            when(onboardingPathRepo.findByUserId("u5")).thenReturn(Optional.of(path));

            OnboardingStepType[] expectedTypes = {
                    OnboardingStepType.WELCOME,
                    OnboardingStepType.AVATAR_INTRODUCTION,
                    OnboardingStepType.SPACE_EXPLORATION,
                    OnboardingStepType.FRIEND_SUGGESTION,
                    OnboardingStepType.SHARE_PROMPT,
                    OnboardingStepType.NOTIFICATION_ENABLE
            };

            for (int i = 0; i < 6; i++) {
                OnboardingStep step = engine.advanceStep("u5");
                assertEquals(expectedTypes[i], step.getStepType(), "Step " + (i + 1) + " type mismatch");
                assertEquals(i + 1, step.getStepNumber());
            }
        }
    }

    // ========== skipStep ==========

    @Nested
    @DisplayName("skipStep — 跳过引导步骤")
    class SkipStepTests {

        @Test
        @DisplayName("跳过步骤 → 步骤标记为已跳过")
        void skipStepMarked() {
            OnboardingPath path = createPath("u1", 0);
            when(onboardingPathRepo.findByUserId("u1")).thenReturn(Optional.of(path));

            OnboardingStep step = engine.skipStep("u1");

            assertNotNull(step);
            assertTrue(step.isSkipped());
            assertFalse(step.isCompleted());
            assertEquals(1, path.getCurrentStep());
        }

        @Test
        @DisplayName("跳过全部 6 步 → COMPLETED")
        void skipAllCompletes() {
            OnboardingPath path = createPath("u2", 0);
            when(onboardingPathRepo.findByUserId("u2")).thenReturn(Optional.of(path));

            for (int i = 0; i < 6; i++) {
                engine.skipStep("u2");
            }

            assertEquals(OnboardingStatus.COMPLETED, path.getStatus());
        }

        @Test
        @DisplayName("已完成引导再跳过 → 抛出异常")
        void completedThrowsOnSkip() {
            OnboardingPath path = createPath("u3", 6);
            path.setStatus(OnboardingStatus.COMPLETED);
            when(onboardingPathRepo.findByUserId("u3")).thenReturn(Optional.of(path));

            assertThrows(IllegalStateException.class, () -> engine.skipStep("u3"));
        }
    }

    // ========== completeOnboarding ==========

    @Nested
    @DisplayName("completeOnboarding — 强制完成引导")
    class CompleteOnboardingTests {

        @Test
        @DisplayName("强制完成 → 状态 COMPLETED，步数设为总步数")
        void forceCompletes() {
            OnboardingPath path = createPath("u1", 2); // only on step 2
            when(onboardingPathRepo.findByUserId("u1")).thenReturn(Optional.of(path));

            OnboardingPath result = engine.completeOnboarding("u1");

            assertEquals(OnboardingStatus.COMPLETED, result.getStatus());
            assertEquals(6, result.getCurrentStep());
        }

        @Test
        @DisplayName("无引导路径 → 抛出异常")
        void noPathThrows() {
            when(onboardingPathRepo.findByUserId("u99")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> engine.completeOnboarding("u99"));
        }
    }

    // ========== getNextRecommendedAction ==========

    @Nested
    @DisplayName("getNextRecommendedAction — 获取下一步推荐")
    class GetNextRecommendedActionTests {

        @Test
        @DisplayName("进行中且未完成 → 返回下一步")
        void inProgressReturnsNext() {
            OnboardingPath path = createPath("u1", 2);
            path.setStatus(OnboardingStatus.IN_PROGRESS);
            when(onboardingPathRepo.findByUserId("u1")).thenReturn(Optional.of(path));

            Optional<OnboardingStep> result = engine.getNextRecommendedAction("u1");

            assertTrue(result.isPresent());
            assertEquals(3, result.get().getStepNumber());
            assertEquals(OnboardingStepType.SPACE_EXPLORATION, result.get().getStepType());
        }

        @Test
        @DisplayName("已完成 → 返回 empty")
        void completedReturnsEmpty() {
            OnboardingPath path = createPath("u2", 6);
            path.setStatus(OnboardingStatus.COMPLETED);
            when(onboardingPathRepo.findByUserId("u2")).thenReturn(Optional.of(path));

            Optional<OnboardingStep> result = engine.getNextRecommendedAction("u2");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("未开始 (NOT_STARTED) → 返回 empty")
        void notStartedReturnsEmpty() {
            OnboardingPath path = createPath("u3", 0);
            path.setStatus(OnboardingStatus.NOT_STARTED);
            when(onboardingPathRepo.findByUserId("u3")).thenReturn(Optional.of(path));

            Optional<OnboardingStep> result = engine.getNextRecommendedAction("u3");

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("无路径 → 返回 empty")
        void noPathReturnsEmpty() {
            when(onboardingPathRepo.findByUserId("u99")).thenReturn(Optional.empty());

            Optional<OnboardingStep> result = engine.getNextRecommendedAction("u99");

            assertFalse(result.isPresent());
        }
    }

    // ========== 帮助方法 ==========

    private OnboardingPath createPath(String userId, int currentStep) {
        OnboardingPath path = new OnboardingPath();
        path.setPathId("p_" + userId);
        path.setUserId(userId);
        path.setCurrentStep(currentStep);
        path.setTotalSteps(6);
        path.setStepHistory(new ArrayList<>());
        path.setStatus(OnboardingStatus.IN_PROGRESS);
        return path;
    }
}
