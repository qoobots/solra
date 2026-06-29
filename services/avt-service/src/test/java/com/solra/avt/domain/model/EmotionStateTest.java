package com.solra.avt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for EmotionState value object.
 */
@DisplayName("EmotionState")
class EmotionStateTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("should create with NEUTRAL by default")
        void shouldDefaultToNeutral() {
            EmotionState state = new EmotionState();
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.NEUTRAL);
            assertThat(state.getIntensity()).isEqualTo(0.5f);
        }

        @Test
        @DisplayName("should clamp intensity to 0..1")
        void shouldClampIntensity() {
            EmotionState state = new EmotionState(EmotionCategory.JOY, 1.5f);
            assertThat(state.getIntensity()).isEqualTo(1.0f);

            state = new EmotionState(EmotionCategory.SADNESS, -0.5f);
            assertThat(state.getIntensity()).isEqualTo(0.0f);
        }
    }

    @Nested
    @DisplayName("updateFromText")
    class UpdateFromText {

        @Test
        @DisplayName("should map joy sentiment")
        void shouldMapJoy() {
            EmotionState state = new EmotionState();
            state.updateFromText("joy", 0.9f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.JOY);
            assertThat(state.getIntensity()).isEqualTo(0.9f);
        }

        @Test
        @DisplayName("should map happy sentiment")
        void shouldMapHappy() {
            EmotionState state = new EmotionState();
            state.updateFromText("happy", 0.8f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.JOY);
        }

        @Test
        @DisplayName("should map sadness sentiment")
        void shouldMapSadness() {
            EmotionState state = new EmotionState();
            state.updateFromText("sadness", 0.7f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.SADNESS);
        }

        @Test
        @DisplayName("should map anger sentiment")
        void shouldMapAnger() {
            EmotionState state = new EmotionState();
            state.updateFromText("anger", 0.9f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.ANGER);
        }

        @Test
        @DisplayName("should map fear sentiment")
        void shouldMapFear() {
            EmotionState state = new EmotionState();
            state.updateFromText("fear", 0.7f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.FEAR);
        }

        @Test
        @DisplayName("should map surprise sentiment")
        void shouldMapSurprise() {
            EmotionState state = new EmotionState();
            state.updateFromText("surprise", 0.8f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.SURPRISE);
        }

        @Test
        @DisplayName("should map trust sentiment")
        void shouldMapTrust() {
            EmotionState state = new EmotionState();
            state.updateFromText("trust", 0.7f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.TRUST);
        }

        @Test
        @DisplayName("should map anticipation sentiment")
        void shouldMapAnticipation() {
            EmotionState state = new EmotionState();
            state.updateFromText("anticipation", 0.6f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.ANTICIPATION);
        }

        @Test
        @DisplayName("should default to NEUTRAL for unknown sentiment")
        void shouldDefaultToNeutral() {
            EmotionState state = new EmotionState();
            state.updateFromText("unknown_feeling", 0.5f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.NEUTRAL);
        }

        @Test
        @DisplayName("should default to NEUTRAL for null sentiment")
        void shouldDefaultToNeutralForNull() {
            EmotionState state = new EmotionState();
            state.updateFromText(null, 0.5f);
            assertThat(state.getPrimaryEmotion()).isEqualTo(EmotionCategory.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("isIntense")
    class IsIntense {

        @Test
        @DisplayName("should return true when intensity >= 0.7")
        void shouldReturnTrueWhenIntense() {
            EmotionState state = new EmotionState(EmotionCategory.JOY, 0.7f);
            assertThat(state.isIntense()).isTrue();
        }

        @Test
        @DisplayName("should return false when intensity < 0.7")
        void shouldReturnFalseWhenNotIntense() {
            EmotionState state = new EmotionState(EmotionCategory.NEUTRAL, 0.3f);
            assertThat(state.isIntense()).isFalse();
        }
    }
}
