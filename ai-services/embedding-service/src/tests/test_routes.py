"""Tests for Embedding Service API routes."""

import pytest
from fastapi.testclient import TestClient

from main import app
from api.dependencies import get_engine
from core.embedding_engine import EmbeddingEngine


@pytest.fixture
def engine():
    """Create a mock engine for testing."""
    eng = EmbeddingEngine(model_name="test-model", device="cpu")
    eng.load_model()
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
        assert data["service"] == "embedding-service"
        assert data["model_loaded"] is True


class TestEmbeddingsEndpoint:
    def test_generate_embeddings_single_text(self, client):
        response = client.post("/api/v1/embeddings", json={
            "texts": ["hello world"],
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data["embeddings"]) == 1
        assert len(data["embeddings"][0]) == 1024
        assert data["model_name"] == "test-model"
        assert data["dimensions"] == 1024
        assert data["processing_time_ms"] >= 0

    def test_generate_embeddings_multiple_texts(self, client):
        response = client.post("/api/v1/embeddings", json={
            "texts": ["a", "b", "c"],
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data["embeddings"]) == 3

    def test_generate_embeddings_empty_texts(self, client):
        response = client.post("/api/v1/embeddings", json={
            "texts": [],
        })
        assert response.status_code == 422  # Validation error

    def test_generate_embeddings_no_normalize(self, client):
        response = client.post("/api/v1/embeddings", json={
            "texts": ["test"],
            "normalize": False,
        })
        assert response.status_code == 200


class TestBatchEmbeddingsEndpoint:
    def test_batch_embeddings(self, client):
        texts = ["text" + str(i) for i in range(10)]
        response = client.post("/api/v1/embeddings/batch", json={
            "texts": texts,
            "batch_size": 4,
        })
        assert response.status_code == 200
        data = response.json()
        assert data["count"] == 10
        assert len(data["embeddings"]) == 10

    def test_batch_embeddings_custom_batch_size(self, client):
        response = client.post("/api/v1/embeddings/batch", json={
            "texts": ["a", "b"],
            "batch_size": 1,
        })
        assert response.status_code == 200


class TestSimilarityEndpoint:
    def test_similarity_basic(self, client):
        response = client.post("/api/v1/similarity", json={
            "query": "hello world",
            "candidates": ["hello there", "goodbye", "world news", "something else"],
            "top_k": 2,
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data["results"]) == 2
        assert data["total_candidates"] == 4
        assert data["query"] == "hello world"

    def test_similarity_empty_candidates(self, client):
        response = client.post("/api/v1/similarity", json={
            "query": "test",
            "candidates": [],
            "top_k": 10,
        })
        assert response.status_code == 200
        data = response.json()
        assert data["results"] == []

    def test_similarity_scores_are_sorted(self, client):
        response = client.post("/api/v1/similarity", json={
            "query": "hello",
            "candidates": ["a", "b", "c", "d", "e"],
            "top_k": 3,
        })
        assert response.status_code == 200
        scores = [r["score"] for r in response.json()["results"]]
        assert scores == sorted(scores, reverse=True)


class TestModelInfoEndpoint:
    def test_model_info(self, client):
        response = client.get("/api/v1/model")
        assert response.status_code == 200
        data = response.json()
        assert data["name"] == "test-model"
        assert data["dimensions"] == 1024
        assert data["is_loaded"] is True
