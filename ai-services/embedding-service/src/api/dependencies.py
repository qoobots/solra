"""FastAPI dependency injection for Embedding Service."""

import logging
from functools import lru_cache

from config import config
from core.embedding_engine import EmbeddingEngine

logger = logging.getLogger(__name__)

_engine: EmbeddingEngine | None = None


def get_engine() -> EmbeddingEngine:
    """Get or create the singleton embedding engine (thread-safe)."""
    global _engine
    if _engine is None:
        _engine = EmbeddingEngine(
            model_name=config.MODEL_NAME,
            device=config.DEVICE,
            max_seq_length=config.MAX_LENGTH,
        )
        _engine.load_model()
    return _engine
