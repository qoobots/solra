"""
Solra LLM Router - Intelligent routing and scheduling for LLM inference.

Routes conversation requests to the appropriate LLM backend:
- On-device (llama.cpp via Core SDK)
- Cloud (Qwen2.5-7B / DeepSeek-V2-Lite via gRPC)

Handles load balancing, fallback, and cost optimization.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import make_asgi_app
import structlog

from .config import settings
from .api.routes import router

logger = structlog.get_logger(__name__)

app = FastAPI(
    title="Solra LLM Router",
    description="Intelligent LLM routing and scheduling service",
    version="0.1.0",
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus metrics endpoint
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)

# API routes
app.include_router(router, prefix="/api/v1")

@app.on_event("startup")
async def startup():
    logger.info("LLM Router starting", version="0.1.0")

@app.on_event("shutdown")
async def shutdown():
    logger.info("LLM Router shutting down")

@app.get("/health")
async def health():
    return {"status": "healthy", "service": "llm-router"}
