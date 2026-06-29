"""FastAPI dependency injection for TTS Service."""

import logging
from functools import lru_cache

from config import config
from core.tts_engine import TTSEngine

logger = logging.getLogger(__name__)

_engine: TTSEngine | None = None


def get_engine() -> TTSEngine:
    """Get or create the singleton TTS engine."""
    global _engine
    if _engine is None:
        _engine = TTSEngine(
            model_name=config.MODEL_NAME,
            device=config.DEVICE,
            sample_rate=config.SAMPLE_RATE,
        )
        _engine.load_model()
    return _engine
