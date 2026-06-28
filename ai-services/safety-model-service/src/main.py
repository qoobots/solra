"""
Solra Safety Model Service - Content safety classification and filtering.

Provides real-time content safety scoring for:
- Text (NLP classification)
- Images (multimodal safety)
- Audio (speech content filtering)

Model: Custom-trained safety classifier + open-source models.
"""

from fastapi import FastAPI
from prometheus_client import make_asgi_app
import structlog

from .config import settings
from .api.routes import router

logger = structlog.get_logger(__name__)

app = FastAPI(
    title="Solra Safety Model Service",
    description="Content safety classification and filtering",
    version="0.1.0",
)

metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)
app.include_router(router, prefix="/api/v1")

@app.on_event("startup")
async def startup():
    logger.info("Safety Model Service starting", version="0.1.0")
    # TODO: Load safety classification model

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "safety-model-service"}
