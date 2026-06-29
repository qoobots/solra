"""API routes for Safety Model Service — single/multi-modality classification."""

from typing import Optional, List, Dict, Any

from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field
import structlog

from ..core.pipeline import SafetyPipeline

logger = structlog.get_logger(__name__)

# ── Pydantic schemas ───────────────────────────────────────────────


class SafetyRequest(BaseModel):
    content_id: str
    content_type: str = Field(..., pattern="^(text|image|audio)$")
    content: Optional[str] = None         # text content or base64 for image
    content_url: Optional[str] = None     # remote URL alternative
    filter_level: str = "normal"          # lenient | normal | strict


class CategoryScore(BaseModel):
    category: str
    score: float
    above_threshold: bool
    label: str = ""


class SafetyResponse(BaseModel):
    content_id: str
    passed: bool
    overall_score: float
    categories: List[CategoryScore] = []
    violations: List[Dict[str, Any]] = []
    processing_time_ms: float = 0.0
    model_version: str = ""


class ClassifyAllRequest(BaseModel):
    content_id: str
    text: Optional[str] = None
    image_url: Optional[str] = None
    audio_url: Optional[str] = None
    filter_level: str = "normal"


class ClassifyAllResponse(BaseModel):
    content_id: str
    overall_passed: bool
    results: Dict[str, SafetyResponse]


class ModelInfoResponse(BaseModel):
    text_model: str
    image_model: str
    audio_model: str
    supported_modalities: List[str]
    categories: Dict[str, List[str]]
    device: str
    loaded: bool


# ── Router factory ──────────────────────────────────────────────────


def create_router(pipeline: SafetyPipeline) -> APIRouter:
    router = APIRouter(tags=["safety"])

    @router.post("/classify", response_model=SafetyResponse)
    async def classify(req: SafetyRequest):
        """Classify single-modality content safety."""
        if req.content_type == "text" and not req.content:
            raise HTTPException(400, "content required for text type")
        if req.content_type in ("image", "audio") and not req.content and not req.content_url:
            raise HTTPException(400, f"content or content_url required for {req.content_type}")

        content = req.content or req.content_url
        try:
            verdict = await pipeline.classify(
                content_id=req.content_id,
                content_type=req.content_type,
                content=content,
                filter_level=req.filter_level,
            )
        except ValueError as e:
            raise HTTPException(400, str(e))

        logger.info("classify.complete",
                    content_id=req.content_id,
                    content_type=req.content_type,
                    passed=verdict.passed,
                    score=verdict.overall_score,
                    violations=len(verdict.violations),
                    elapsed_ms=verdict.processing_time_ms)

        return SafetyResponse(
            content_id=verdict.content_id,
            passed=verdict.passed,
            overall_score=verdict.overall_score,
            categories=[
                CategoryScore(
                    category=c.category,
                    score=c.score,
                    above_threshold=c.above_threshold,
                    label=c.label,
                )
                for c in verdict.categories
            ],
            violations=verdict.violations,
            processing_time_ms=verdict.processing_time_ms,
            model_version=verdict.model_version,
        )

    @router.post("/classify-all", response_model=ClassifyAllResponse)
    async def classify_all(req: ClassifyAllRequest):
        """Classify across multiple modalities concurrently."""
        if not any([req.text, req.image_url, req.audio_url]):
            raise HTTPException(400, "at least one modality required")

        verdicts = await pipeline.classify_all_modalities(
            content_id=req.content_id,
            text=req.text,
            image=req.image_url,
            audio=req.audio_url,
            filter_level=req.filter_level,
        )

        results = {}
        for modality, v in verdicts.items():
            results[modality] = SafetyResponse(
                content_id=v.content_id,
                passed=v.passed,
                overall_score=v.overall_score,
                categories=[CategoryScore(
                    category=c.category, score=c.score,
                    above_threshold=c.above_threshold, label=c.label,
                ) for c in v.categories],
                violations=v.violations,
                processing_time_ms=v.processing_time_ms,
                model_version=v.model_version,
            )

        overall_passed = all(r.passed for r in results.values())

        logger.info("classify_all.complete",
                    content_id=req.content_id,
                    modalities=len(results),
                    passed=overall_passed)

        return ClassifyAllResponse(
            content_id=req.content_id,
            overall_passed=overall_passed,
            results=results,
        )

    @router.get("/model/info", response_model=ModelInfoResponse)
    async def model_info():
        """Get model metadata and status."""
        info = pipeline.model_info()
        return ModelInfoResponse(
            text_model=info["text"],
            image_model=info["image"],
            audio_model=info["audio"],
            supported_modalities=info["supported_modalities"],
            categories=info["categories"],
            device="mock" if not pipeline.is_loaded else "loaded",
            loaded=pipeline.is_loaded,
        )

    @router.post("/classify/file")
    async def classify_file(
        content_id: str = Form(...),
        content_type: str = Form(..., pattern="^(image|audio)$"),
        filter_level: str = Form("normal"),
        file: UploadFile = File(...),
    ):
        """Classify uploaded image/audio file."""
        content = await file.read()
        verdict = await pipeline.classify(
            content_id=content_id,
            content_type=content_type,
            content=content,
            filter_level=filter_level,
        )
        return SafetyResponse(
            content_id=verdict.content_id,
            passed=verdict.passed,
            overall_score=verdict.overall_score,
            categories=[CategoryScore(
                category=c.category, score=c.score,
                above_threshold=c.above_threshold, label=c.label,
            ) for c in verdict.categories],
            violations=verdict.violations,
            processing_time_ms=verdict.processing_time_ms,
            model_version=verdict.model_version,
        )

    return router
