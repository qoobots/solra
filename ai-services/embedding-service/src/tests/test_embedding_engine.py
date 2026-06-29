"""Tests for Embedding Engine core logic."""

import pytest
import numpy as np
from core.embedding_engine import EmbeddingEngine


class TestEmbeddingEngine:
    """Unit tests for EmbeddingEngine (mock mode)."""

    @pytest.fixture
    def engine(self):
        """Create an engine in mock mode."""
        eng = EmbeddingEngine(model_name="test-model", device="cpu")
        eng.load_model()
        return eng

    def test_load_model_sets_loaded_flag(self, engine):
        assert engine.is_loaded is True

    def test_get_model_info(self, engine):
        info = engine.get_model_info()
        assert info["name"] == "test-model"
        assert info["dimensions"] == 1024
        assert info["device"] == "cpu"
        assert info["is_loaded"] is True

    def test_encode_single_text(self, engine):
        embeddings = engine.encode(["hello world"])
        assert embeddings.shape == (1, 1024)
        assert embeddings.dtype == np.float32

    def test_encode_multiple_texts(self, engine):
        texts = ["text one", "text two", "text three"]
        embeddings = engine.encode(texts)
        assert embeddings.shape == (3, 1024)

    def test_encode_empty_list(self, engine):
        embeddings = engine.encode([])
        assert embeddings.shape == (0, 1024)

    def test_encode_normalized(self, engine):
        embeddings = engine.encode(["test text"], normalize=True)
        norms = np.linalg.norm(embeddings, axis=1)
        assert np.allclose(norms, 1.0, atol=1e-5)

    def test_encode_deterministic(self, engine):
        """Same text should produce same embedding."""
        e1 = engine.encode(["hello"])
        e2 = engine.encode(["hello"])
        assert np.allclose(e1, e2)

    def test_encode_different_texts_different_embeddings(self, engine):
        e1 = engine.encode(["hello"])
        e2 = engine.encode(["world"])
        assert not np.allclose(e1, e2)

    def test_similarity_returns_top_k(self, engine):
        results = engine.similarity(
            query="hello world",
            candidates=["hello there", "goodbye world", "random text", "hello world again"],
            top_k=2,
        )
        assert len(results) == 2
        # Results should be (index, text, score) tuples
        for item in results:
            assert len(item) == 3
            assert isinstance(item[0], int)
            assert isinstance(item[1], str)
            assert isinstance(item[2], float)

    def test_similarity_scores_in_range(self, engine):
        results = engine.similarity(
            query="test",
            candidates=["test", "other", "something"],
            top_k=3,
        )
        for _, _, score in results:
            assert -1.0 <= score <= 1.0

    def test_similarity_empty_candidates(self, engine):
        results = engine.similarity(query="test", candidates=[], top_k=10)
        assert results == []

    def test_similarity_top_k_larger_than_candidates(self, engine):
        candidates = ["a", "b", "c"]
        results = engine.similarity(query="x", candidates=candidates, top_k=100)
        assert len(results) == 3

    def test_unload_model(self, engine):
        engine.unload_model()
        assert engine.is_loaded is False
        info = engine.get_model_info()
        assert info["is_loaded"] is False
