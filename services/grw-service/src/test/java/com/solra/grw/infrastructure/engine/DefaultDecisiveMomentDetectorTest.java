package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.DecisiveMoment;
import com.solra.grw.domain.model.DecisiveMomentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DefaultDecisiveMomentDetector 单元测试")
class DefaultDecisiveMomentDetectorTest {

    private DefaultDecisiveMomentDetector detector;

    @BeforeEach
    void setUp() {
        detector = new DefaultDecisiveMomentDetector();
    }

    // ========== detectMoments ==========

    @Nested
    @DisplayName("detectMoments — 检测决定性时刻")
    class DetectMomentsTests {

        @Test
        @DisplayName("首次对话 (conversationsHad>=1) → 检测 FIRST_CONVERSATION")
        void detectsFirstConversation() {
            Map<String, Object> state = Map.of("conversationsHad", 1, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u1", actions, state);

            assertEquals(1, moments.size());
            assertEquals(DecisiveMomentType.FIRST_CONVERSATION, moments.get(0).getMomentType());
            assertEquals(0.6, moments.get(0).getConversionValue(), 0.001);
        }

        @Test
        @DisplayName("首次空间探索 (spacesVisited>=1) → 检测 FIRST_SPACE_EXPLORED")
        void detectsFirstSpaceExplored() {
            Map<String, Object> state = Map.of("conversationsHad", 0, "spacesVisited", 1, "friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u2", actions, state);

            assertEquals(1, moments.size());
            assertEquals(DecisiveMomentType.FIRST_SPACE_EXPLORED, moments.get(0).getMomentType());
            assertEquals(0.5, moments.get(0).getConversionValue(), 0.001);
        }

        @Test
        @DisplayName("首次添加好友 (friendsCount>=1) → 检测 FIRST_FRIEND_ADDED")
        void detectsFirstFriendAdded() {
            Map<String, Object> state = Map.of("conversationsHad", 0, "spacesVisited", 0, "friendsCount", 1);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u3", actions, state);

            assertEquals(1, moments.size());
            assertEquals(DecisiveMomentType.FIRST_FRIEND_ADDED, moments.get(0).getMomentType());
            assertEquals(0.7, moments.get(0).getConversionValue(), 0.001);
        }

        @Test
        @DisplayName("首次分享 (actions 包含 SHARE_CREATE) → 检测 FIRST_SHARE")
        void detectsFirstShare() {
            Map<String, Object> state = Map.of("conversationsHad", 0, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of("SHARE_CREATE");

            List<DecisiveMoment> moments = detector.detectMoments("u4", actions, state);

            assertEquals(1, moments.size());
            assertEquals(DecisiveMomentType.FIRST_SHARE, moments.get(0).getMomentType());
            assertEquals(0.8, moments.get(0).getConversionValue(), 0.001);
        }

        @Test
        @DisplayName("多维度同时满足 → 检测所有类型")
        void detectsMultipleMoments() {
            Map<String, Object> state = Map.of("conversationsHad", 1, "spacesVisited", 1, "friendsCount", 1);
            List<String> actions = List.of("SHARE_CREATE");

            List<DecisiveMoment> moments = detector.detectMoments("u5", actions, state);

            assertEquals(4, moments.size());
            assertTrue(moments.stream().anyMatch(m -> m.getMomentType() == DecisiveMomentType.FIRST_CONVERSATION));
            assertTrue(moments.stream().anyMatch(m -> m.getMomentType() == DecisiveMomentType.FIRST_SPACE_EXPLORED));
            assertTrue(moments.stream().anyMatch(m -> m.getMomentType() == DecisiveMomentType.FIRST_FRIEND_ADDED));
            assertTrue(moments.stream().anyMatch(m -> m.getMomentType() == DecisiveMomentType.FIRST_SHARE));
        }

        @Test
        @DisplayName("零行为新用户 → 不检测任何时刻")
        void noBehaviorNoMoments() {
            Map<String, Object> state = Map.of("conversationsHad", 0, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u6", actions, state);

            assertTrue(moments.isEmpty());
        }

        @Test
        @DisplayName("已检测过的不再重复检测 (actions 中已有)")
        void alreadyDetectedNotRepeated() {
            Map<String, Object> state = Map.of("conversationsHad", 1, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of("FIRST_CONVERSATION");

            List<DecisiveMoment> moments = detector.detectMoments("u7", actions, state);

            assertTrue(moments.isEmpty());
        }

        @Test
        @DisplayName("已通过行为标记检测过 (actions 包含 CONVERSATION)")
        void alreadyDetectedViaActionTag() {
            Map<String, Object> state = Map.of("conversationsHad", 1, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of("CONVERSATION");

            List<DecisiveMoment> moments = detector.detectMoments("u8", actions, state);

            assertTrue(moments.isEmpty());
        }
    }

    // ========== shouldTrigger ==========

    @Nested
    @DisplayName("shouldTrigger — 触发判断")
    class ShouldTriggerTests {

        @Test
        @DisplayName("conversionValue >= 0.5 → true")
        void valueAboveThreshold() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);
            m.setConversionValue(0.6);

            assertTrue(detector.shouldTrigger(m));
        }

        @Test
        @DisplayName("conversionValue = 0.5 边界 → true")
        void valueAtThreshold() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_SPACE_EXPLORED);
            m.setConversionValue(0.5);

            assertTrue(detector.shouldTrigger(m));
        }

        @Test
        @DisplayName("conversionValue < 0.5 → false")
        void valueBelowThreshold() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);
            m.setConversionValue(0.4);

            assertFalse(detector.shouldTrigger(m));
        }

        @Test
        @DisplayName("conversionValue = 0 → false")
        void zeroValue() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);

            assertFalse(detector.shouldTrigger(m));
        }
    }

    // ========== calculateConversionValue ==========

    @Nested
    @DisplayName("calculateConversionValue — 计算转化价值")
    class CalculateConversionValueTests {

        @Test
        @DisplayName("互动增长 delta/5 → 转化价值")
        void growthCalculated() {
            Map<String, Object> before = Map.of("totalInteractions", 0);
            Map<String, Object> after = Map.of("totalInteractions", 5);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(1.0, value, 0.001);
        }

        @Test
        @DisplayName("delta=1 → 0.2")
        void deltaOne() {
            Map<String, Object> before = Map.of("totalInteractions", 10);
            Map<String, Object> after = Map.of("totalInteractions", 11);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(0.2, value, 0.001);
        }

        @Test
        @DisplayName("delta=3 → 0.6")
        void deltaThree() {
            Map<String, Object> before = Map.of("totalInteractions", 2);
            Map<String, Object> after = Map.of("totalInteractions", 5);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(0.6, value, 0.001);
        }

        @Test
        @DisplayName("无增长 → 0.0")
        void noGrowthReturnsZero() {
            Map<String, Object> before = Map.of("totalInteractions", 5);
            Map<String, Object> after = Map.of("totalInteractions", 5);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(0.0, value, 0.001);
        }

        @Test
        @DisplayName("负增长 → 0.0")
        void negativeGrowthReturnsZero() {
            Map<String, Object> before = Map.of("totalInteractions", 10);
            Map<String, Object> after = Map.of("totalInteractions", 5);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(0.0, value, 0.001);
        }

        @Test
        @DisplayName("超大 delta → 钳制为 1.0")
        void largeDeltaClamped() {
            Map<String, Object> before = Map.of("totalInteractions", 0);
            Map<String, Object> after = Map.of("totalInteractions", 100);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(1.0, value, 0.001);
        }

        @Test
        @DisplayName("缺少字段默认为 0")
        void missingFieldDefaultsToZero() {
            Map<String, Object> before = Map.of();
            Map<String, Object> after = Map.of("totalInteractions", 3);

            double value = detector.calculateConversionValue(before, after);

            assertEquals(0.6, value, 0.001);
        }
    }

    // ========== getInt 边界 (通过 detectMoments 间接测试) ==========

    @Nested
    @DisplayName("getInt 数值类型转换")
    class GetIntConversionTests {

        @Test
        @DisplayName("Long 值可转为 int")
        void longValueConverted() {
            Map<String, Object> state = Map.of("conversationsHad", 1L, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u1", actions, state);

            assertEquals(1, moments.size());
        }

        @Test
        @DisplayName("Double 值可转为 int")
        void doubleValueConverted() {
            Map<String, Object> state = Map.of("conversationsHad", 1.5, "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u1", actions, state);

            assertEquals(1, moments.size());
        }

        @Test
        @DisplayName("非数值类型默认为 0")
        void nonNumericDefaultsToZero() {
            Map<String, Object> state = Map.of("conversationsHad", "not a number", "spacesVisited", 0, "friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u1", actions, state);

            assertTrue(moments.isEmpty());
        }

        @Test
        @DisplayName("null 值默认为 0")
        void nullValueDefaultsToZero() {
            Map<String, Object> state = new HashMap<>();
            state.put("conversationsHad", null);
            state.put("spacesVisited", 0);
            state.put("friendsCount", 0);
            List<String> actions = List.of();

            List<DecisiveMoment> moments = detector.detectMoments("u1", actions, state);

            assertTrue(moments.isEmpty());
        }
    }
}
