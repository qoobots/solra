"""API routes for Safety Model Service."""

from fastapi import APIRouter
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
import structlog

router = APIRouter(tags=["safety"])
logger = structlog.get_logger(__name__)


class SafetyRequest(BaseModel):
    content_id: str
    content_type: str = Field(..., pattern="^(text|image|audio|video)$")
    content: Optional[str] = None
    content_url: Optional[str] = None
    filter_level: str = "normal"  # lenient | normal | strict


class CategoryScore(BaseModel):
    category: str
    score: float
    above_threshold: bool


class SafetyResponse(BaseModel):
    content_id: str
    passed: bool
    overall_score: float
    category_scores: List[CategoryScore]
    violations: List[Dict[str, Any]] = []
    model_version: str = "solra-safety-v1-stub"


@router.post("/classify", response_model=SafetyResponse)
async def classify_content(request: SafetyRequest):
    """Classify content safety across multiple categories."""
    logger.info("Safety classification requested",
                content_id=request.content_id,
                content_type=request.content_type)

    # TODO: Load model and run inference
    # Categories: nsfw, hate_speech, harassment, violence, self_harm,
    #             spam, fraud, minor_safety, personal_info

    return SafetyResponse(
        content_id=request.content_id,
        passed=True,
        overall_score=0.95,  # 1.0 = perfectly safe
        category_scores=[
            CategoryScore(category="nsfw", score=0.98, above_threshold=False),
            CategoryScore(category="hate_speech", score=0.99, above_threshold=False),
            CategoryScore(category="violence", score=0.97, above_threshold=False),
        ],
        model_version="solra-safety-v1-stub",
    )


@router.get("/model/info")
async def model_info():
    """Get information about the loaded safety model."""
    return {
        "model_name": "solra-safety-v1-stub",
        "status": "not_loaded",
        "supported_types": ["text", "image", "audio"],
        "categories": [
            "nsfw", "hate_speech", "harassment", "violence",
            "self_harm", "spam", "fraud", "minor_safety", "personal_info"
        ],
        "device": "stub",
    }
