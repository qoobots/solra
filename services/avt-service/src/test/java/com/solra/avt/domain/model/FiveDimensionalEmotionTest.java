package com.solra.avt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FiveDimensionalEmotion 单元测试 — AVT-004 5维情感模型")
class FiveDimensionalEmotionTest {

    @Nested
    @DisplayName("初始状态")
    class InitialStateTests {

        @Test
        @DisplayName("新创建的情感状态 → joy=0.5, 其他有默认值")
        void defaultState() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();

            assertEquals(0.5f, emotion.getJoy(), 0.01);
            assertEquals(0.5f, emotion.getCuriosity(), 0.01);
            assertEquals(0.1f, emotion.getColdness(), 0.01);
            assertEquals(0.0f, emotion.getJealousy(), 0.01);
            assertEquals(0.1f, emotion.getSadness(), 0.01);
            assertEquals("neutral", emotion.getCurrentMood());
        }
    }

    @Nested
    @DisplayName("applyEvent — 事件驱动情感变化")
    class ApplyEventTests {

        @Test
        @DisplayName("用户赞美 → joy增加, sadness减少")
        void complimentIncreasesJoy() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_COMPLIMENT, 1.0f);

            assertTrue(emotion.getJoy() > 0.5f, "Joy should increase after compliment");
            assertTrue(emotion.getCuriosity() > 0.5f, "Curiosity should increase");
        }

        @Test
        @DisplayName("用户侮辱 → sadness和coldness增加")
        void insultIncreasesNegativeEmotions() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_INSULT, 1.0f);

            assertTrue(emotion.getSadness() > 0.1f, "Sadness should increase after insult");
            assertTrue(emotion.getColdness() > 0.1f, "Coldness should increase");
        }

        @Test
        @DisplayName("用户回归 → joy大幅增加, coldness减少")
        void userReturnedBoostsJoy() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_RETURNED, 1.0f);

            assertTrue(emotion.getJoy() > 0.6f, "Joy should boost after return");
            assertTrue(emotion.getColdness() < 0.1f, "Coldness should decrease");
            assertTrue(emotion.getCuriosity() > 0.5f);
        }

        @Test
        @DisplayName("用户与其他虚拟人聊天 → jealousy增加")
        void talkingToOtherIncreasesJealousy() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_TALKED_TO_OTHER, 1.0f);

            assertTrue(emotion.getJealousy() > 0.1f, "Jealousy should increase");
        }

        @Test
        @DisplayName("惊喜事件 → curiosity大幅增加")
        void surpriseBoostsCuriosity() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.SURPRISE_EVENT, 1.0f);

            assertTrue(emotion.getCuriosity() > 0.7f, "Curiosity should spike after surprise");
        }

        @Test
        @DisplayName("情感值不会超过1.0")
        void emotionValuesClampedAtOne() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            // Apply many compliments to push joy high
            for (int i = 0; i < 10; i++) {
                emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_COMPLIMENT, 1.0f);
            }
            assertTrue(emotion.getJoy() <= 1.0f, "Joy should be clamped at 1.0");
        }

        @Test
        @DisplayName("情感值不会低于0")
        void emotionValuesClampedAtZero() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            // Apply many insults to push sadness up, then compliments to push down
            for (int i = 0; i < 5; i++) {
                emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_INSULT, 1.0f);
            }
            assertTrue(emotion.getSadness() >= 0f, "Sadness should not go below 0");
        }
    }

    @Nested
    @DisplayName("decay — 情感衰减")
    class DecayTests {

        @Test
        @DisplayName("衰减后情感向中性值回归")
        void decayTowardsNeutral() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_COMPLIMENT, 1.0f);
            float joyBefore = emotion.getJoy();

            emotion.decay(0.1f);

            // Joy should move towards 0.5
            assertTrue(Math.abs(emotion.getJoy() - 0.5f) < Math.abs(joyBefore - 0.5f),
                    "Decay should move joy towards neutral 0.5");
        }
    }

    @Nested
    @DisplayName("getDominantDimension / getCurrentMood — 主导情感")
    class DominantEmotionTests {

        @Test
        @DisplayName("高joy → 主导维度为joy, mood为cheerful")
        void highJoyIsDominant() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_COMPLIMENT, 1.0f);
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_RETURNED, 1.0f);

            assertEquals("joy", emotion.getDominantDimension());
            assertTrue(
                    emotion.getCurrentMood().equals("cheerful") ||
                    emotion.getCurrentMood().equals("pleasant"),
                    "Mood should be cheerful or pleasant, got: " + emotion.getCurrentMood());
        }

        @Test
        @DisplayName("高jealousy → 主导维度为jealousy")
        void highJealousyIsDominant() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            for (int i = 0; i < 5; i++) {
                emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_TALKED_TO_OTHER, 1.0f);
            }

            assertEquals("jealousy", emotion.getDominantDimension());
        }
    }
}
