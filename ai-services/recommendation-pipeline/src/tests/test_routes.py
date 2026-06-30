"""Tests for Recommendation Pipeline API routes."""

import pytest
from fastapi.testclient import TestClient

from main import app
from api.dependencies import get_engine
from core.engine import RecommendationEngine
import time


@pytest.fixture
def engine():
    """Create a pre-seeded engine for testing."""
    eng = RecommendationEngine(candidate_pool_size=500)
    for i in range(10):
        eng.register_space(
            f"space-{i}",
            categories=["explore"] if i % 2 == 0 else ["social"],
            created_at=time.time() - i * 3600,
        )
    eng.record_interaction("user-1", "space-0", "like")
    eng.record_interaction("user-1", "space-1", "view")
    eng.record_interaction("user-1", "space-2", "share")
    eng.train()
    return eng


@pytest.fixture
def client(engine):
    """Create TestClient with mocked engine."""
    app.dependency_overrides[get_engine] = lambda: engine
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


class TestHealthEndpoint:
    def test_health_returns_ok(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["service"] == "recommendation-pipeline"
        assert data["model_trained"] is True


class TestRecommendationsEndpoint:
    def test_get_recommendations_hybrid(self, client):
        response = client.post("/api/v1/spaces/recommendations", json={
            "user_id": "user-1",
            "mode": "hybrid",
            "size": 5,
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) <= 5
        assert data["mode"] == "hybrid"
        assert data["processing_time_ms"] >= 0

    def test_get_recommendations_popular(self, client):
        response = client.post("/api/v1/spaces/recommendations", json={
            "user_id": "user-new",
            "mode": "popular",
            "size": 3,
        })
        assert response.status_code == 200
        data = response.json()
        assert data["mode"] == "popular"

    def test_get_recommendations_with_exclude(self, client):
        response = client.post("/api/v1/spaces/recommendations", json={
            "user_id": "user-1",
            "mode": "popular",
            "size": 5,
            "exclude_ids": ["space-0"],
        })
        assert response.status_code == 200
        space_ids = [item["space_id"] for item in response.json()["items"]]
        assert "space-0" not in space_ids

    def test_get_recommendations_with_categories(self, client):
        response = client.post("/api/v1/spaces/recommendations", json={
            "user_id": "user-1",
            "mode": "popular",
            "size": 10,
            "categories": ["social"],
        })
        assert response.status_code == 200
        for item in response.json()["items"]:
            assert "space-" in item["space_id"]

    def test_get_recommendations_validation_error(self, client):
        response = client.post("/api/v1/spaces/recommendations", json={
            "user_id": "",
            "mode": "invalid",
        })
        assert response.status_code == 422


class TestInteractionEndpoint:
    def test_record_interaction(self, client):
        response = client.post("/api/v1/interactions", json={
            "user_id": "user-1",
            "space_id": "space-5",
            "event_type": "view",
        })
        assert response.status_code == 200
        assert response.json()["status"] == "recorded"

    def test_record_interaction_invalid_type(self, client):
        response = client.post("/api/v1/interactions", json={
            "user_id": "user-1",
            "space_id": "space-5",
            "event_type": "invalid_type",
        })
        assert response.status_code == 422


class TestModelEndpoints:
    def test_train_model(self, client):
        response = client.post("/api/v1/model/train")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "completed"

    def test_get_model_status(self, client):
        response = client.get("/api/v1/model/status")
        assert response.status_code == 200
        data = response.json()
        assert data["is_trained"] is True
        assert data["training_samples"] > 0


class TestRegisterSpace:
    def test_register_space(self, client):
        response = client.post("/api/v1/spaces/register?space_id=space-new&categories=explore&categories=social")
        assert response.status_code == 200
        assert response.json()["status"] == "registered"
