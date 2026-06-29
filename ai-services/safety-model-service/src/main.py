"""
Solra Safety Model Service — Multimodal content safety classification.

Endpoints:
  POST /api/v1/classify       — classify single-modality content
  POST /api/v1/classify-all   — classify multi-modality content
  GET  /api/v1/model/info     — model metadata
  GET  /health                — liveness probe
  GET  /metrics               — Prometheus metrics
"""

from contextlib import asynccontextmanager
from fastapi import FastAPI
from prometheus_client import make_asgi_app
import structlog

from .config.settings import settings
from .core.pipeline import SafetyPipeline
from .api.routes import create_router

logger = structlog.get_logger(__name__)

# ── Global pipeline singleton ──────────────────────────────────────
pipeline: SafetyPipeline = SafetyPipeline(
    text_model_path=settings.TEXT_MODEL_PATH,
    image_model_path=settings.IMAGE_MODEL_PATH,
    audio_model_path=settings.AUDIO_MODEL_PATH,
    device=settings.DEVICE,
    use_real_models=settings.USE_REAL_MODELS,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load models on startup, release on shutdown."""
    logger.info("safety_model_service.starting",
                device=settings.DEVICE,
                real_models=settings.USE_REAL_MODELS)
    await pipeline.load_all()
    yield
    await pipeline.shutdown()
    logger.info("safety_model_service.shutdown_complete")


app = FastAPI(
    title="Solra Safety Model Service",
    description="Multimodal content safety classification (text + image + audio)",
    version="0.2.0",
    lifespan=lifespan,
)

# Prometheus metrics
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)

# API routes with pipeline injected
router = create_router(pipeline)
app.include_router(router, prefix="/api/v1")


@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "service": "safety-model-service",
        "version": "0.2.0",
        "model_loaded": pipeline.is_loaded,
        "device": settings.DEVICE,
    }
