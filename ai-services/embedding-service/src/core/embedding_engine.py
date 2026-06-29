"""Embedding engine: model loading, inference, and similarity computation."""

import time
import logging
import hashlib
import io
from typing import List, Tuple, Optional, Union

import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingEngine:
    """
    Core embedding engine supporting text and multimodal embedding + similarity search.

    Uses sentence-transformers for model loading and inference.
    Provides a mock implementation for environments without GPU/models.
    Supports text embedding (text2vec-large-chinese) and image embedding (CLIP/ViT).
    """

    def __init__(
        self,
        model_name: str = "text2vec-large-chinese",
        device: str = "cpu",
        max_seq_length: int = 512,
        vision_model_name: Optional[str] = None,
    ):
        self.model_name = model_name
        self.device = device
        self.max_seq_length = max_seq_length
        self._model = None
        self._dimensions = 1024  # Default for text2vec-large-chinese
        self._is_loaded = False
        # Vision/multimodal support
        self.vision_model_name = vision_model_name or "clip-ViT-B-32-multilingual-v1"
        self._vision_model = None
        self._vision_dimensions = 512  # Default for CLIP ViT-B/32
        self._vision_loaded = False

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded

    @property
    def dimensions(self) -> int:
        return self._dimensions

    @property
    def is_vision_loaded(self) -> bool:
        return self._vision_loaded

    @property
    def vision_dimensions(self) -> int:
        return self._vision_dimensions

    def load_model(self) -> None:
        """
        Load the embedding model into memory.

        Attempts to load via sentence-transformers; falls back to mock.
        Also attempts to load the vision/multimodal model if available.
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

        # Attempt to load vision model for multimodal embedding
        self._load_vision_model()

    def _load_vision_model(self) -> None:
        """Load vision/multimodal model for image embedding."""
        try:
            from sentence_transformers import SentenceTransformer

            logger.info(f"Loading vision model: {self.vision_model_name}")
            self._vision_model = SentenceTransformer(
                self.vision_model_name,
                device=self.device,
            )
            self._vision_dimensions = self._vision_model.get_sentence_embedding_dimension()
            self._vision_loaded = True
            logger.info(
                f"Vision model loaded: {self.vision_model_name}, dim={self._vision_dimensions}"
            )
        except Exception as e:
            logger.warning(
                f"Failed to load vision model {self.vision_model_name}: {e}. Using mock."
            )
            self._vision_model = None
            self._vision_loaded = True  # Mock mode is ready

    def _generate_mock_embedding(self, text: str, dim: int = 1024) -> np.ndarray:
        """Generate a deterministic mock embedding for testing."""
        seed = sum(ord(c) for c in text)
        rng = np.random.RandomState(seed)
        vec = rng.randn(dim).astype(np.float32)
        vec = vec / np.linalg.norm(vec)  # L2 normalize
        return vec

    def _generate_mock_image_embedding(self, image_bytes: bytes, dim: int = 512) -> np.ndarray:
        """Generate a deterministic mock image embedding."""
        seed = int(hashlib.md5(image_bytes).hexdigest(), 16) % (2 ** 31)
        rng = np.random.RandomState(seed)
        vec = rng.randn(dim).astype(np.float32)
        vec = vec / np.linalg.norm(vec)
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

    def encode_image(
        self,
        images: List[bytes],
        normalize: bool = True,
    ) -> np.ndarray:
        """
        Encode image bytes into embedding vectors using vision model.

        Args:
            images: List of raw image bytes (JPEG/PNG).
            normalize: Whether to L2-normalize the output vectors.

        Returns:
            numpy array of shape (len(images), vision_dimensions).
        """
        if not images:
            return np.array([], dtype=np.float32).reshape(0, self._vision_dimensions)

        if self._vision_model is not None:
            try:
                from PIL import Image as PILImage

                pil_images = [PILImage.open(io.BytesIO(img)).convert("RGB") for img in images]
                embeddings = self._vision_model.encode(
                    pil_images,
                    normalize_embeddings=normalize,
                    show_progress_bar=False,
                )
            except Exception as e:
                logger.warning(f"Vision model encode failed: {e}, using mock")
                embeddings = np.array([
                    self._generate_mock_image_embedding(img, self._vision_dimensions)
                    for img in images
                ])
        else:
            # Mock mode
            embeddings = np.array([
                self._generate_mock_image_embedding(img, self._vision_dimensions)
                for img in images
            ])

        return np.asarray(embeddings, dtype=np.float32)

    def encode_multimodal(
        self,
        texts: Optional[List[str]] = None,
        images: Optional[List[bytes]] = None,
        normalize: bool = True,
    ) -> np.ndarray:
        """
        Encode mixed text and image inputs into a unified embedding space.

        Text embeddings are projected to the vision dimension space via
        zero-padding or truncation to enable cross-modal similarity.

        Args:
            texts: Optional list of text strings.
            images: Optional list of raw image bytes.
            normalize: Whether to L2-normalize output vectors.

        Returns:
            numpy array of shape (len(texts)+len(images), vision_dimensions).
        """
        results = []

        if texts:
            text_vecs = self.encode(texts, normalize=normalize)
            # Project text embeddings to vision dimension
            text_vecs = self._project_embedding(text_vecs, self._dimensions, self._vision_dimensions)
            results.append(text_vecs)

        if images:
            image_vecs = self.encode_image(images, normalize=normalize)
            results.append(image_vecs)

        if not results:
            return np.array([], dtype=np.float32).reshape(0, self._vision_dimensions)

        combined = np.concatenate(results, axis=0)
        return np.asarray(combined, dtype=np.float32)

    def _project_embedding(
        self,
        vectors: np.ndarray,
        src_dim: int,
        tgt_dim: int,
    ) -> np.ndarray:
        """Project vectors from source dimension to target dimension."""
        if src_dim == tgt_dim:
            return vectors
        n = vectors.shape[0]
        projected = np.zeros((n, tgt_dim), dtype=np.float32)
        copy_dim = min(src_dim, tgt_dim)
        projected[:, :copy_dim] = vectors[:, :copy_dim]
        # Re-normalize after projection
        norms = np.linalg.norm(projected, axis=1, keepdims=True)
        norms[norms == 0] = 1.0
        return projected / norms

    def cross_modal_similarity(
        self,
        query_text: str,
        image_candidates: List[bytes],
        top_k: int = 10,
    ) -> List[Tuple[int, float]]:
        """
        Compute similarity between a text query and image candidates.

        Args:
            query_text: Query text.
            image_candidates: List of image bytes.
            top_k: Number of top results.

        Returns:
            List of (index, score) tuples sorted by score descending.
        """
        if not image_candidates:
            return []

        start = time.perf_counter()
        query_vec = self.encode([query_text], normalize=True)
        query_vec = self._project_embedding(query_vec, self._dimensions, self._vision_dimensions)
        image_vecs = self.encode_image(image_candidates, normalize=True)

        scores = np.dot(image_vecs, query_vec.T).flatten()

        if top_k >= len(image_candidates):
            top_indices = np.argsort(scores)[::-1]
        else:
            top_indices = np.argpartition(scores, -top_k)[-top_k:]
            top_indices = top_indices[np.argsort(scores[top_indices])[::-1]]

        results = [
            (int(idx), float(scores[idx]))
            for idx in top_indices
        ]

        elapsed = (time.perf_counter() - start) * 1000
        logger.debug(
            f"Cross-modal similarity: {elapsed:.1f}ms, {len(image_candidates)} candidates"
        )
        return results

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
        """Get current model information including vision model status."""
        return {
            "name": self.model_name,
            "dimensions": self._dimensions,
            "max_seq_length": self.max_seq_length,
            "device": self.device,
            "is_loaded": self._is_loaded,
            "vision_model": {
                "name": self.vision_model_name,
                "dimensions": self._vision_dimensions,
                "is_loaded": self._vision_loaded,
            },
        }

    def unload_model(self) -> None:
        """Unload model to free memory."""
        self._model = None
        self._is_loaded = False
        self._vision_model = None
        self._vision_loaded = False
        logger.info(f"Models unloaded: {self.model_name} + {self.vision_model_name}")
