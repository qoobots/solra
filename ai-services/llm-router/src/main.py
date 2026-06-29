"""Solra LLM Router — Intelligent LLM routing and scheduling service."""

from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator

from config import settings
from api.routes import router as api_router
from api.dependencies import get_registry

logger = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application startup and shutdown lifecycle."""
    # Startup
    logger.info("llm_router_starting",
                port=settings.PORT,
                strategy=settings.ROUTING_STRATEGY)

    registry = get_registry()
    logger.info("backends_initialized", count=len(registry.list_all()))

    # Initial health check
    results = await registry.check_all_health()
    healthy = sum(1 for v in results.values() if v)
    logger.info("initial_health_check", healthy=healthy, total=len(results))

    yield  # Application runs here

    # Shutdown
    logger.info("llm_router_shutting_down")


app = FastAPI(
    title="Solra LLM Router",
    description="Intelligent routing and scheduling for LLM inference backends",
    version="0.1.0",
    lifespan=lifespan,
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Prometheus metrics
Instrumentator().instrument(app).expose(app, endpoint="/metrics")

# API routes
app.include_router(api_router, prefix="/api/v1")


@app.get("/health")
async def health_check():
    """Kubernetes-compatible health check endpoint."""
    registry = get_registry()
    healthy_backends = registry.list_healthy()
    status = "ok" if len(healthy_backends) > 0 else "degraded"
    return {
        "status": status,
        "healthy_backends": len(healthy_backends),
        "total_backends": len(registry.list_all()),
    }
