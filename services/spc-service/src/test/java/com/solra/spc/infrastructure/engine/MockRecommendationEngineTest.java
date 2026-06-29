package com.solra.spc.infrastructure.engine;

import com.solra.spc.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MockRecommendationEngine.
 */
@DisplayName("MockRecommendationEngine")
class MockRecommendationEngineTest {

    private MockRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MockRecommendationEngine();
    }

    @Nested
    @DisplayName("recommend")
    class RecommendTests {

        @Test
        @DisplayName("should return recommendations up to limit")
        void shouldReturnUpToLimit() {
            List<Recommendation> results = engine.recommend("user-1", 5, null);

            assertThat(results).hasSize(5);
            assertThat(results.get(0).getSpaceId()).isNotEmpty();
            assertThat(results.get(0).getScore()).isNotNull();
            assertThat(results.get(0).getRecommendReasons()).isNotEmpty();
        }

        @Test
        @DisplayName("should cap at available space pool size")
        void shouldCapAtPoolSize() {
            // Pool has 10 spaces, request 20
            List<Recommendation> results = engine.recommend("user-1", 20, null);

            assertThat(results).hasSize(10); // capped at pool size
        }

        @Test
        @DisplayName("should have valid scores")
        void shouldHaveValidScores() {
            List<Recommendation> results = engine.recommend("user-1", 3, null);

            for (Recommendation rec : results) {
                assertThat(rec.getScore().getOverall()).isBetween(0.0f, 1.0f);
                assertThat(rec.getScore().getRelevance()).isBetween(0.0f, 1.0f);
                assertThat(rec.getGeneratedAt()).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("recommendation modes")
    class ModeTests {

        @Test
        @DisplayName("popular should return recommendations")
        void popularShouldReturnRecommendations() {
            List<Recommendation> results = engine.popular(5, null);
            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("newest should return recommendations")
        void newestShouldReturnRecommendations() {
            List<Recommendation> results = engine.newest(5, null);
            assertThat(results).isNotEmpty();
        }

        @Test
        @DisplayName("trending should return recommendations")
        void trendingShouldReturnRecommendations() {
            List<Recommendation> results = engine.trending(5, null);
            assertThat(results).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("reportAction")
    class ReportActionTests {

        @Test
        @DisplayName("should accept user action without error")
        void shouldAcceptUserAction() {
            UserAction action = new UserAction();
            action.setUserId("user-1");
            action.setSpaceId("spc-001");
            action.setActionType(UserActionType.VIEW);

            assertThatCode(() -> engine.reportAction(action)).doesNotThrowAnyException();
        }
    }
}
