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
        assert engine.is_vision_loaded is False
        info = engine.get_model_info()
        assert info["is_loaded"] is False

    # ---- Multimodal / Vision Tests ----

    def test_encode_image_single(self, engine):
        """Single image encoding returns correct shape."""
        fake_image = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100
        embeddings = engine.encode_image([fake_image])
        assert embeddings.shape == (1, 512)
        assert embeddings.dtype == np.float32

    def test_encode_image_multiple(self, engine):
        """Multiple images return correct shape."""
        images = [b"\xff\xd8\xff\xe0" + bytes([i]) * 100 for i in range(3)]
        embeddings = engine.encode_image(images)
        assert embeddings.shape == (3, 512)

    def test_encode_image_empty(self, engine):
        """Empty image list returns empty array."""
        embeddings = engine.encode_image([])
        assert embeddings.shape == (0, 512)

    def test_encode_image_normalized(self, engine):
        """Image embeddings should be L2-normalized."""
        fake_image = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100
        embeddings = engine.encode_image([fake_image], normalize=True)
        norms = np.linalg.norm(embeddings, axis=1)
        assert np.allclose(norms, 1.0, atol=1e-5)

    def test_encode_image_deterministic(self, engine):
        """Same image bytes produce same embedding."""
        img = b"\xff\xd8" + b"\xaa" * 200
        e1 = engine.encode_image([img])
        e2 = engine.encode_image([img])
        assert np.allclose(e1, e2)

    def test_encode_multimodal_text_only(self, engine):
        """Multimodal with only text."""
        embeddings = engine.encode_multimodal(texts=["hello", "world"])
        assert embeddings.shape == (2, 512)  # Projected to vision dim

    def test_encode_multimodal_image_only(self, engine):
        """Multimodal with only images."""
        images = [b"\x89PNG" + b"\x00" * 100 for _ in range(2)]
        embeddings = engine.encode_multimodal(images=images)
        assert embeddings.shape == (2, 512)

    def test_encode_multimodal_mixed(self, engine):
        """Multimodal with both text and images."""
        images = [b"\xff\xd8" + b"\x00" * 100]
        embeddings = engine.encode_multimodal(texts=["hello"], images=images)
        assert embeddings.shape == (2, 512)

    def test_encode_multimodal_empty(self, engine):
        """Empty multimodal input returns empty array."""
        embeddings = engine.encode_multimodal()
        assert embeddings.shape == (0, 512)

    def test_cross_modal_similarity(self, engine):
        """Cross-modal similarity: text query vs images."""
        images = [bytes([i]) * 100 for i in range(5)]
        results = engine.cross_modal_similarity(
            query_text="a cat",
            image_candidates=images,
            top_k=3,
        )
        assert len(results) == 3
        for idx, score in results:
            assert isinstance(idx, int)
            assert -1.0 <= score <= 1.0

    def test_cross_modal_similarity_empty(self, engine):
        """Cross-modal with empty candidates."""
        results = engine.cross_modal_similarity(
            query_text="test",
            image_candidates=[],
        )
        assert results == []

    def test_get_model_info_includes_vision(self, engine):
        """Model info should include vision model details."""
        info = engine.get_model_info()
        assert "vision_model" in info
        assert info["vision_model"]["name"] is not None
        assert info["vision_model"]["dimensions"] == 512
        assert info["vision_model"]["is_loaded"] is True

    def test_project_embedding_padding(self, engine):
        """Projection pads smaller dimensions to target dimension."""
        src = np.array([[1.0, 0.0, 0.0]], dtype=np.float32)  # 3-dim
        result = engine._project_embedding(src, src_dim=3, tgt_dim=5)
        assert result.shape == (1, 5)
        assert result[0, 0] > 0  # First element preserved
        assert result[0, 3] == 0.0  # Padded zero

    def test_project_embedding_truncation(self, engine):
        """Projection truncates larger dimensions to target dimension."""
        src = np.array([[1.0, 0.0, 0.0, 0.0, 0.0]], dtype=np.float32)  # 5-dim
        result = engine._project_embedding(src, src_dim=5, tgt_dim=3)
        assert result.shape == (1, 3)

    def test_project_embedding_same_dim(self, engine):
        """Projection with same dimensions is identity (after re-normalize)."""
        src = np.array([[1.0, 0.0]], dtype=np.float32)
        result = engine._project_embedding(src, src_dim=2, tgt_dim=2)
        assert np.allclose(result, src)
