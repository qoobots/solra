"""API routes for recommendation pipeline."""

import time
import logging

from fastapi import APIRouter, Depends, HTTPException

from models.schemas import (
    SpaceRecommendationRequest,
    SpaceRecommendationResponse,
    RecommendationItem,
    RecommendationScore,
    UserInteractionEvent,
    ModelTrainingStatus,
)
from core.engine import RecommendationEngine
from api.dependencies import get_engine

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["recommendation"])


@router.post(
    "/spaces/recommendations",
    response_model=SpaceRecommendationResponse,
    summary="Get personalized space recommendations",
)
async def get_recommendations(
    request: SpaceRecommendationRequest,
    engine: RecommendationEngine = Depends(get_engine),
):
    """
    Generate space recommendations for a user.

    Supports multiple strategies: popular, personalized, newest, trending, hybrid.
    """
    start = time.perf_counter()
    try:
        items_raw, mode_used, total = engine.recommend(
            user_id=request.user_id,
            mode=request.mode.value,
            size=request.size,
            categories=request.categories,
            exclude_ids=request.exclude_ids,
        )
        elapsed = (time.perf_counter() - start) * 1000

        items = [
            RecommendationItem(
                space_id=item["space_id"],
                score=RecommendationScore(**item["score"]),
                reason=item["reason"],
                rank=item["rank"],
            )
            for item in items_raw
        ]

        return SpaceRecommendationResponse(
            items=items,
            mode=request.mode,
            total_candidates=total,
            processing_time_ms=round(elapsed, 2),
            cursor=None if not items else items[-1].space_id,
            has_more=len(items_raw) >= request.size,
        )
    except Exception as e:
        logger.error(f"Recommendation failed: {e}")
        raise HTTPException(status_code=500, detail=f"Recommendation failed: {str(e)}")


@router.post(
    "/interactions",
    summary="Record user-space interaction for training",
)
async def record_interaction(
    event: UserInteractionEvent,
    engine: RecommendationEngine = Depends(get_engine),
):
    """
    Record a user interaction event for model training.

    Supported event types: view, like, share, enter, dwell.
    """
    try:
        engine.record_interaction(
            user_id=event.user_id,
            space_id=event.space_id,
            event_type=event.event_type,
            dwell_time_ms=event.dwell_time_ms,
        )
        return {"status": "recorded", "event_type": event.event_type}
    except Exception as e:
        logger.error(f"Failed to record interaction: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/model/train",
    summary="Trigger model training",
)
async def train_model(
    engine: RecommendationEngine = Depends(get_engine),
):
    """
    Manually trigger model retraining.

    Uses accumulated user interaction data for collaborative filtering.
    """
    try:
        engine.train()
        status = engine.get_status()
        return {
            "status": "completed",
            "model_version": status["model_version"],
            "training_samples": status["training_samples"],
        }
    except Exception as e:
        logger.error(f"Model training failed: {e}")
        raise HTTPException(status_code=500, detail=f"Training failed: {str(e)}")


@router.get(
    "/model/status",
    response_model=ModelTrainingStatus,
    summary="Get model training status",
)
async def get_model_status(
    engine: RecommendationEngine = Depends(get_engine),
):
    """Get current model training status."""
    return ModelTrainingStatus(**engine.get_status())


@router.post(
    "/spaces/register",
    summary="Register a space as recommendation candidate",
)
async def register_space(
    space_id: str,
    categories: list[str] | None = None,
    engine: RecommendationEngine = Depends(get_engine),
):
    """
    Register a new space in the recommendation candidate pool.
    Called when a space is published.
    """
    import time as _time
    engine.register_space(
        space_id=space_id,
        categories=categories,
        created_at=_time.time(),
    )
    return {"status": "registered", "space_id": space_id}
