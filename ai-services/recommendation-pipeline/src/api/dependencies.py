"""FastAPI dependency injection for Recommendation Pipeline."""

import logging
from functools import lru_cache

from core.engine import RecommendationEngine

logger = logging.getLogger(__name__)

_engine: RecommendationEngine | None = None


def get_engine() -> RecommendationEngine:
    """Get or create the singleton recommendation engine."""
    global _engine
    if _engine is None:
        _engine = RecommendationEngine(candidate_pool_size=500)
    return _engine
