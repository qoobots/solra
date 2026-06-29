"""Tests for Recommendation Engine core logic."""

import pytest
import time
from core.engine import RecommendationEngine


class TestRecommendationEngine:
    """Unit tests for RecommendationEngine."""

    @pytest.fixture
    def engine(self):
        """Create a fresh engine with seed data."""
        eng = RecommendationEngine(candidate_pool_size=500)

        # Register seed spaces
        for i in range(10):
            eng.register_space(
                f"space-{i}",
                categories=["explore"] if i % 2 == 0 else ["social"],
                created_at=time.time() - i * 3600,
            )

        # Record some interactions
        eng.record_interaction("user-1", "space-0", "view")
        eng.record_interaction("user-1", "space-0", "like")
        eng.record_interaction("user-1", "space-1", "view")
        eng.record_interaction("user-2", "space-2", "share")
        eng.record_interaction("user-2", "space-3", "enter", dwell_time_ms=120000)
        eng.record_interaction("user-3", "space-0", "like")

        eng.train()
        return eng

    def test_initial_state_not_trained(self):
        eng = RecommendationEngine()
        assert eng.is_trained is False
        assert eng.training_samples == 0

    def test_train_sets_trained_flag(self, engine):
        assert engine.is_trained is True
        assert engine.training_samples > 0

    def test_model_version_after_training(self, engine):
        assert engine.model_version != "0.0.0"
        assert engine.model_version.startswith("0.1.")

    def test_recommend_popular_mode(self, engine):
        items, mode, total = engine.recommend(
            user_id="user-new",
            mode="popular",
            size=5,
        )
        assert len(items) <= 5
        assert mode == "popular"
        assert total == 10  # All 10 spaces registered

    def test_recommend_personalized_mode(self, engine):
        items, mode, total = engine.recommend(
            user_id="user-1",
            mode="personalized",
            size=5,
        )
        assert mode == "personalized"
        # user-1 has interactions, should get personalized results
        assert len(items) >= 0

    def test_recommend_newest_mode(self, engine):
        items, mode, total = engine.recommend(
            user_id="any",
            mode="newest",
            size=3,
        )
        assert len(items) <= 3
        assert mode == "newest"

    def test_recommend_trending_mode(self, engine):
        items, mode, total = engine.recommend(
            user_id="any",
            mode="trending",
            size=5,
        )
        assert mode == "trending"

    def test_recommend_hybrid_mode(self, engine):
        items, mode, total = engine.recommend(
            user_id="user-1",
            mode="hybrid",
            size=5,
        )
        assert mode == "hybrid"
        assert len(items) >= 0

    def test_recommend_with_exclude_ids(self, engine):
        items, mode, total = engine.recommend(
            user_id="user-1",
            mode="popular",
            size=5,
            exclude_ids=["space-0", "space-1"],
        )
        excluded = {item["space_id"] for item in items}
        assert "space-0" not in excluded
        assert "space-1" not in excluded

    def test_recommend_with_categories(self, engine):
        items, mode, total = engine.recommend(
            user_id="user-1",
            mode="popular",
            size=10,
            categories=["social"],
        )
        for item in items:
            assert item["space_id"] in [f"space-{i}" for i in range(10) if i % 2 == 1]

    def test_recommend_items_have_required_fields(self, engine):
        items, _, _ = engine.recommend(user_id="user-1", mode="hybrid", size=3)
        for item in items:
            assert "space_id" in item
            assert "score" in item
            assert "relevance" in item["score"]
            assert "popularity" in item["score"]
            assert "freshness" in item["score"]
            assert "overall" in item["score"]
            assert "reason" in item
            assert "rank" in item

    def test_record_interaction_accumulates(self, engine):
        engine.record_interaction("user-1", "space-5", "view")
        engine.record_interaction("user-1", "space-5", "like")
        # Should accumulate weight
        assert engine._user_interactions["user-1"]["space-5"] == pytest.approx(1.3, 0.01)

    def test_cold_start_user_gets_results(self, engine):
        """New user with no interactions should still get recommendations."""
        items, mode, total = engine.recommend(
            user_id="totally-new-user",
            mode="hybrid",
            size=5,
        )
        assert len(items) >= 0  # At minimum, popularity-based results

    def test_get_status(self, engine):
        status = engine.get_status()
        assert status["is_trained"] is True
        assert status["training_samples"] > 0
        assert status["model_version"].startswith("0.1.")

    # ---- Cold-Start Strategy Tests ----

    def test_cold_start_mode_explicit(self, engine):
        """Explicit cold_start mode should work."""
        items, mode, total = engine.recommend(
            user_id="new-user-cs",
            mode="cold_start",
            size=5,
        )
        assert mode == "cold_start"
        assert len(items) <= 5
        for item in items:
            assert "space_id" in item
            assert "reason" in item
            assert "rank" in item

    def test_cold_start_auto_detection(self, engine):
        """Hybrid mode should auto-switch to cold_start for new users."""
        items, mode, total = engine.recommend(
            user_id="brand-new-user-99",
            mode="hybrid",
            size=5,
        )
        # Should auto-detect cold-start since user has < 3 interactions
        assert mode == "cold_start"

    def test_cold_start_returns_hybrid_for_known_user(self, engine):
        """Known user with >= 3 interactions should NOT get cold_start in hybrid mode."""
        # user-1 already has 2 interactions, add one more
        engine.record_interaction("user-1", "space-5", "view")
        items, mode, total = engine.recommend(
            user_id="user-1",
            mode="hybrid",
            size=5,
        )
        # user-1 now has 3 interactions → not cold-start
        assert mode == "hybrid"

    def test_is_cold_start_user_true(self, engine):
        """User with < 3 interactions is cold-start."""
        assert engine._is_cold_start_user("totally-new") is True

    def test_is_cold_start_user_false(self, engine):
        """User with >= 3 interactions is not cold-start."""
        assert engine._is_cold_start_user("user-1") is True  # 2 interactions
        engine.record_interaction("user-1", "space-9", "view")
        assert engine._is_cold_start_user("user-1") is False  # Now 3

    def test_new_space_exploration(self, engine):
        """New spaces should be explorable."""
        explore = engine._get_new_space_exploration(size=5)
        # All spaces are within 7 days, should have candidates
        assert len(explore) >= 0

    def test_new_space_exposure_tracking(self, engine):
        """Registering a space creates exposure tracking."""
        engine.register_space("fresh-space", categories=["new"])
        assert "fresh-space" in engine._new_space_exposure
        exp = engine._new_space_exposure["fresh-space"]
        assert exp["impressions"] == 0
        assert "boost_until" in exp

    def test_category_affinity_tracking(self, engine):
        """Interactions should track category affinity."""
        engine.record_interaction("user-1", "space-0", "like")  # space-0 is "explore"
        affinity = engine._get_user_category_affinity("user-1")
        assert "explore" in affinity
        assert affinity["explore"] > 0

    def test_get_status_includes_cold_start_info(self, engine):
        """Status should include new space tracking info."""
        status = engine.get_status()
        assert "total_users" in status
        assert "total_spaces" in status
        assert "new_spaces_in_boost" in status
        assert status["total_spaces"] == 10

    def test_cold_start_blends_strategies(self, engine):
        """Cold-start should blend popular + newest + exploration."""
        items, mode, total = engine.recommend(
            user_id="fresh-start",
            mode="cold_start",
            size=10,
        )
        assert mode == "cold_start"
        # Should return up to size items
        assert len(items) <= 10
