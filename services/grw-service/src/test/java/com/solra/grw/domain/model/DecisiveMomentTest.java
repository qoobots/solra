package com.solra.grw.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DecisiveMoment 实体 单元测试")
class DecisiveMomentTest {

    @Nested
    @DisplayName("构造 — 创建决定性时刻")
    class ConstructionTests {

        @Test
        @DisplayName("构造后默认值")
        void defaults() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);

            assertEquals("m1", m.getMomentId());
            assertEquals("u1", m.getUserId());
            assertEquals(DecisiveMomentType.FIRST_CONVERSATION, m.getMomentType());
            assertEquals(0.0, m.getConversionValue(), 0.001);
            assertFalse(m.getTriggered());
            assertNotNull(m.getDetectedAt());
            assertNotNull(m.getUserStateBefore());
            assertNotNull(m.getUserStateAfter());
            assertTrue(m.getUserStateBefore().isEmpty());
        }

        @Test
        @DisplayName("无参构造创建空状态 Map")
        void noArgConstructor() {
            DecisiveMoment m = new DecisiveMoment();
            assertNotNull(m.getUserStateBefore());
            assertNotNull(m.getUserStateAfter());
        }
    }

    // ========== trigger ==========

    @Nested
    @DisplayName("trigger — 触发时刻")
    class TriggerTests {

        @Test
        @DisplayName("触发后 triggered=true，更新 conversionValue 和时间")
        void triggersMoment() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);
            m.trigger(0.8);

            assertTrue(m.isTriggered());
            assertEquals(0.8, m.getConversionValue(), 0.001);
            assertNotNull(m.getDetectedAt());
        }

        @Test
        @DisplayName("值钳制：超 1.0 → 1.0")
        void valueClampedToOne() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_SHARE);
            m.trigger(2.5);

            assertEquals(1.0, m.getConversionValue(), 0.001);
        }

        @Test
        @DisplayName("值钳制：负值 → 0.0")
        void valueClampedToZero() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_FRIEND_ADDED);
            m.trigger(-0.5);

            assertEquals(0.0, m.getConversionValue(), 0.001);
        }

        @Test
        @DisplayName("值钳制：0.0 正常")
        void zeroValue() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_SPACE_EXPLORED);
            m.trigger(0.0);

            assertEquals(0.0, m.getConversionValue(), 0.001);
            assertTrue(m.isTriggered());
        }
    }

    // ========== setStateSnapshot ==========

    @Nested
    @DisplayName("setStateSnapshot — 状态快照")
    class StateSnapshotTests {

        @Test
        @DisplayName("设置前后状态快照")
        void setsStateSnapshot() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);
            var before = java.util.Map.of("totalInteractions", 0);
            var after = java.util.Map.of("totalInteractions", 1);

            m.setStateSnapshot(before, after);

            assertEquals(0, m.getUserStateBefore().get("totalInteractions"));
            assertEquals(1, m.getUserStateAfter().get("totalInteractions"));
        }

        @Test
        @DisplayName("null 快照 → 空 Map")
        void nullSnapshotBecomesEmpty() {
            DecisiveMoment m = new DecisiveMoment("m1", "u1", DecisiveMomentType.FIRST_CONVERSATION);
            m.setStateSnapshot(null, null);

            assertTrue(m.getUserStateBefore().isEmpty());
            assertTrue(m.getUserStateAfter().isEmpty());
        }
    }
}
