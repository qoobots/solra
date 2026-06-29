"""Embedding engine: model loading, inference, and similarity computation."""

import time
import logging
from typing import List, Tuple, Optional

import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingEngine:
    """
    Core embedding engine supporting text embedding and similarity search.

    Uses sentence-transformers for model loading and inference.
    Provides a mock implementation for environments without GPU/models.
    """

    def __init__(
        self,
        model_name: str = "text2vec-large-chinese",
        device: str = "cpu",
        max_seq_length: int = 512,
    ):
        self.model_name = model_name
        self.device = device
        self.max_seq_length = max_seq_length
        self._model = None
        self._dimensions = 1024  # Default for text2vec-large-chinese
        self._is_loaded = False

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded

    @property
    def dimensions(self) -> int:
        return self._dimensions

    def load_model(self) -> None:
        """
        Load the embedding model into memory.

        Attempts to load via sentence-transformers; falls back to mock.
        """
        try:
            from sentence_transformers import SentenceTransformer

            logger.info(f"Loading embedding model: {self.model_name}")
            self._model = SentenceTransformer(
                self.model_name,
                device=self.device,
            )
            self._dimensions = self._model.get_sentence_embedding_dimension()
            self._is_loaded = True
            logger.info(
                f"Model loaded: {self.model_name}, dim={self._dimensions}, device={self.device}"
            )
        except Exception as e:
            logger.warning(f"Failed to load model {self.model_name}: {e}. Using mock embeddings.")
            self._model = None
            self._is_loaded = True  # Mock mode is ready

    def _generate_mock_embedding(self, text: str, dim: int = 1024) -> np.ndarray:
        """Generate a deterministic mock embedding for testing."""
        seed = sum(ord(c) for c in text)
        rng = np.random.RandomState(seed)
        vec = rng.randn(dim).astype(np.float32)
        vec = vec / np.linalg.norm(vec)  # L2 normalize
        return vec

    def encode(
        self,
        texts: List[str],
        batch_size: int = 32,
        normalize: bool = True,
    ) -> np.ndarray:
        """
        Encode a list of texts into embedding vectors.

        Args:
            texts: List of text strings to encode.
            batch_size: Internal batch size for model inference.
            normalize: Whether to L2-normalize the output vectors.

        Returns:
            numpy array of shape (len(texts), dimensions).
        """
        if not texts:
            return np.array([], dtype=np.float32).reshape(0, self._dimensions)

        if self._model is not None:
            embeddings = self._model.encode(
                texts,
                batch_size=batch_size,
                normalize_embeddings=normalize,
                show_progress_bar=False,
            )
        else:
            # Mock mode
            embeddings = np.array([
                self._generate_mock_embedding(t, self._dimensions)
                for t in texts
            ])

        return np.asarray(embeddings, dtype=np.float32)

    def similarity(
        self,
        query: str,
        candidates: List[str],
        top_k: int = 10,
    ) -> List[Tuple[int, str, float]]:
        """
        Compute cosine similarity between a query and candidate texts.

        Args:
            query: Query text.
            candidates: List of candidate texts.
            top_k: Number of top results to return.

        Returns:
            List of (index, text, score) tuples sorted by score descending.
        """
        if not candidates:
            return []

        start = time.perf_counter()
        all_texts = [query] + candidates
        embeddings = self.encode(all_texts, normalize=True)
        query_vec = embeddings[0]
        candidate_vecs = embeddings[1:]

        # Cosine similarity (already normalized, so dot product = cosine)
        scores = np.dot(candidate_vecs, query_vec)

        # Get top-k indices
        if top_k >= len(candidates):
            top_indices = np.argsort(scores)[::-1]
        else:
            top_indices = np.argpartition(scores, -top_k)[-top_k:]
            top_indices = top_indices[np.argsort(scores[top_indices])[::-1]]

        results = [
            (int(idx), candidates[idx], float(scores[idx]))
            for idx in top_indices
        ]

        elapsed = (time.perf_counter() - start) * 1000
        logger.debug(f"Similarity computed in {elapsed:.1f}ms, {len(candidates)} candidates")

        return results

    def get_model_info(self) -> dict:
        """Get current model information."""
        return {
            "name": self.model_name,
            "dimensions": self._dimensions,
            "max_seq_length": self.max_seq_length,
            "device": self.device,
            "is_loaded": self._is_loaded,
        }

    def unload_model(self) -> None:
        """Unload model to free memory."""
        self._model = None
        self._is_loaded = False
        logger.info(f"Model {self.model_name} unloaded")
