package com.solra.avt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AvatarExpression 单元测试 — AVT-006 肢体与表情表达")
class AvatarExpressionTest {

    @Nested
    @DisplayName("fromType — 从类型创建表情")
    class FromTypeTests {

        @Test
        @DisplayName("HAPPY表情 → 生成mouthSmile和cheekSquint blendshape")
        void happyExpression() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.HAPPY, 1.0f);

            assertNotNull(expr.getExpressionId());
            assertEquals(AvatarExpression.ExpressionType.HAPPY, expr.getExpressionType());
            assertEquals(1.0f, expr.getIntensity(), 0.01);
            assertFalse(expr.getBlendshapes().isEmpty());
            assertTrue(expr.getBlendshapes().stream()
                    .anyMatch(b -> b.blendshapeName().equals("mouthSmile")));
        }

        @Test
        @DisplayName("SAD表情 → 包含mouthFrown和browInnerUp")
        void sadExpression() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.SAD, 0.8f);

            assertEquals(0.8f, expr.getIntensity(), 0.01);
            assertTrue(expr.getBlendshapes().stream()
                    .anyMatch(b -> b.blendshapeName().equals("mouthFrown")));
            assertTrue(expr.getBlendshapes().stream()
                    .anyMatch(b -> b.blendshapeName().equals("browInnerUp")));
        }

        @Test
        @DisplayName("SURPRISED表情 → 包含browUp和eyeWide")
        void surprisedExpression() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.SURPRISED, 1.0f);

            assertTrue(expr.getBlendshapes().stream()
                    .anyMatch(b -> b.blendshapeName().equals("browUp")));
            assertTrue(expr.getBlendshapes().stream()
                    .anyMatch(b -> b.blendshapeName().equals("eyeWide")));
        }

        @Test
        @DisplayName("NEUTRAL表情 → 低强度blendshapes")
        void neutralExpression() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.NEUTRAL, 0.5f);

            assertFalse(expr.getBlendshapes().isEmpty());
            // Neutral should have low weight values
            for (var bs : expr.getBlendshapes()) {
                assertTrue(bs.weight() <= 0.2f,
                        "Neutral blendshape weights should be low, got: " + bs.weight());
            }
        }

        @Test
        @DisplayName("强度为0时blendshape权重为0")
        void zeroIntensity() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.HAPPY, 0.0f);

            for (var bs : expr.getBlendshapes()) {
                assertEquals(0.0f, bs.weight(), 0.01);
            }
        }

        @Test
        @DisplayName("≥15种表情类型可用")
        void atLeast15Expressions() {
            AvatarExpression.ExpressionType[] types = AvatarExpression.ExpressionType.values();
            assertTrue(types.length >= 15,
                    "Should have at least 15 expression types, got: " + types.length);
        }
    }

    @Nested
    @DisplayName("fromEmotion — 从5D情感生成表情")
    class FromEmotionTests {

        @Test
        @DisplayName("高joy情感 → HAPPY或EXCITED表情")
        void highJoyMapsToHappy() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_COMPLIMENT, 1.0f);
            emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_GAVE_GIFT, 1.0f);

            AvatarExpression expr = AvatarExpression.fromEmotion(emotion);

            assertTrue(
                    expr.getExpressionType() == AvatarExpression.ExpressionType.HAPPY ||
                    expr.getExpressionType() == AvatarExpression.ExpressionType.EXCITED,
                    "High joy should map to HAPPY or EXCITED, got: " + expr.getExpressionType());
        }

        @Test
        @DisplayName("高jealousy情感 → ANGRY表情")
        void highJealousyMapsToAngry() {
            FiveDimensionalEmotion emotion = new FiveDimensionalEmotion();
            for (int i = 0; i < 5; i++) {
                emotion.applyEvent(FiveDimensionalEmotion.EmotionEventType.USER_TALKED_TO_OTHER, 1.0f);
            }

            AvatarExpression expr = AvatarExpression.fromEmotion(emotion);

            assertEquals(AvatarExpression.ExpressionType.ANGRY, expr.getExpressionType());
        }
    }

    @Nested
    @DisplayName("surprise — 惊喜表情")
    class SurpriseTests {

        @Test
        @DisplayName("记忆召回惊喜 → SURPRISE_HAPPY")
        void memoryRecallSurprise() {
            AvatarExpression expr = AvatarExpression.surprise("memory_recall");

            assertEquals(AvatarExpression.ExpressionType.SURPRISE_HAPPY, expr.getExpressionType());
            assertTrue(expr.getIntensity() >= 0.8f, "Surprise intensity should be high");
        }

        @Test
        @DisplayName("成就惊喜 → PROUD")
        void achievementSurprise() {
            AvatarExpression expr = AvatarExpression.surprise("achievement");

            assertEquals(AvatarExpression.ExpressionType.PROUD, expr.getExpressionType());
        }
    }

    @Nested
    @DisplayName("GestureType — 手势类型")
    class GestureTests {

        @Test
        @DisplayName("≥20种手势类型")
        void atLeast20Gestures() {
            AvatarExpression.GestureType[] gestures = AvatarExpression.GestureType.values();
            assertTrue(gestures.length >= 20,
                    "Should have at least 20 gesture types, got: " + gestures.length);
        }

        @Test
        @DisplayName("HAPPY表情配CLAP手势")
        void happyGestureIsClap() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.HAPPY, 1.0f);
            assertEquals(AvatarExpression.GestureType.CLAP, expr.getGesture());
        }

        @Test
        @DisplayName("SAD表情配HEAD_DOWN手势")
        void sadGestureIsHeadDown() {
            AvatarExpression expr = AvatarExpression.fromType(
                    AvatarExpression.ExpressionType.SAD, 1.0f);
            assertEquals(AvatarExpression.GestureType.HEAD_DOWN, expr.getGesture());
        }
    }
}
