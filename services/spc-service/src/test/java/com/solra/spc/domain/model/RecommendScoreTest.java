package com.solra.spc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RecommendScore value object.
 */
@DisplayName("RecommendScore")
class RecommendScoreTest {

    @Nested
    @DisplayName("weighted formula")
    class WeightedFormula {

        @Test
        @DisplayName("should calculate overall with correct weights")
        void shouldCalculateOverall() {
            // 0.4 * relevance + 0.35 * popularity + 0.25 * freshness
            RecommendScore score = new RecommendScore(1.0f, 1.0f, 1.0f);
            assertThat(score.getOverall()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("should calculate weighted average")
        void shouldCalculateWeightedAverage() {
            RecommendScore score = new RecommendScore(0.5f, 0.6f, 0.7f);
            float expected = 0.4f * 0.5f + 0.35f * 0.6f + 0.25f * 0.7f;
            assertThat(score.getOverall()).isEqualTo(expected);
        }

        @Test
        @DisplayName("should handle zero scores")
        void shouldHandleZeroScores() {
            RecommendScore score = new RecommendScore(0.0f, 0.0f, 0.0f);
            assertThat(score.getOverall()).isEqualTo(0.0f);
        }
    }

    @Nested
    @DisplayName("accessors")
    class Accessors {

        @Test
        @DisplayName("should return individual scores")
        void shouldReturnIndividualScores() {
            RecommendScore score = new RecommendScore(0.5f, 0.6f, 0.7f);
            assertThat(score.getRelevance()).isEqualTo(0.5f);
            assertThat(score.getPopularity()).isEqualTo(0.6f);
            assertThat(score.getFreshness()).isEqualTo(0.7f);
        }
    }
}
