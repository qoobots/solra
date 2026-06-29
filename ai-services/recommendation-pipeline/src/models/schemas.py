"""Pydantic data models for Recommendation Pipeline."""

from typing import List, Optional
from enum import Enum
from pydantic import BaseModel, Field


class RecommendationMode(str, Enum):
    """Recommendation strategy modes."""
    POPULAR = "popular"
    PERSONALIZED = "personalized"
    NEWEST = "newest"
    TRENDING = "trending"
    HYBRID = "hybrid"


class SpaceRecommendationRequest(BaseModel):
    """Request for space recommendations."""
    user_id: str = Field(..., description="Target user identifier")
    mode: RecommendationMode = Field(RecommendationMode.HYBRID, description="Recommendation strategy")
    size: int = Field(20, ge=1, le=100, description="Number of recommendations to return")
    categories: Optional[List[str]] = Field(None, description="Filter by space categories")
    exclude_ids: Optional[List[str]] = Field(None, description="Space IDs to exclude")


class RecommendationScore(BaseModel):
    """Detailed recommendation scoring."""
    relevance: float = Field(0.0, ge=0.0, le=1.0, description="Content relevance score")
    popularity: float = Field(0.0, ge=0.0, le=1.0, description="Popularity score")
    freshness: float = Field(0.0, ge=0.0, le=1.0, description="Freshness/newest score")
    overall: float = Field(0.0, ge=0.0, le=1.0, description="Combined overall score")


class RecommendationItem(BaseModel):
    """A single recommendation result."""
    space_id: str = Field(..., description="Recommended space ID")
    score: RecommendationScore = Field(..., description="Detailed scoring")
    reason: str = Field("", description="Human-readable recommendation reason")
    rank: int = Field(..., description="Rank position (1-based)")


class SpaceRecommendationResponse(BaseModel):
    """Response containing space recommendations."""
    items: List[RecommendationItem] = Field(..., description="Recommended spaces")
    mode: RecommendationMode = Field(..., description="Strategy used")
    total_candidates: int = Field(..., description="Total candidates evaluated")
    processing_time_ms: float = Field(0.0, description="Processing time in milliseconds")
    cursor: Optional[str] = Field(None, description="Pagination cursor")
    has_more: bool = Field(False, description="Whether more results available")


class UserInteractionEvent(BaseModel):
    """Event for recording user-space interactions."""
    user_id: str
    space_id: str
    event_type: str = Field(..., pattern="^(view|like|share|enter|dwell)$")
    dwell_time_ms: Optional[int] = Field(None, ge=0)
    timestamp: Optional[float] = None


class ModelTrainingStatus(BaseModel):
    """Status of recommendation model training."""
    is_trained: bool
    model_version: str
    training_samples: int
    last_trained_at: Optional[str] = None
    training_duration_sec: Optional[float] = None
