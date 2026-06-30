"""推荐流水线服务主入口。协同过滤 + 内容推荐 + 热门推荐混合策略。"""
import logging
import uvicorn
from contextlib import asynccontextmanager
from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator

from api.routes import router as rec_router
from api.dependencies import get_engine
from core.engine import RecommendationEngine
from config import config

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize engine on startup."""
    logger.info(f"Starting Recommendation Pipeline on port {config.PORT}")
    engine = get_engine()
    # Register some seed spaces for cold start
    seed_spaces = ["space-seed-1", "space-seed-2", "space-seed-3"]
    import time
    for sid in seed_spaces:
        engine.register_space(sid, categories=["explore"], created_at=time.time())
    logger.info(f"Recommendation engine ready with {len(seed_spaces)} seed spaces")
    yield
    logger.info("Recommendation Pipeline shut down")


app = FastAPI(
    title="Solra Recommendation Pipeline",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

Instrumentator().instrument(app).expose(app)

app.include_router(rec_router)


@app.get("/health")
async def health(engine: RecommendationEngine = Depends(get_engine)):
    status = engine.get_status()
    return {
        "status": "ok",
        "service": "recommendation-pipeline",
        "model_trained": status["is_trained"],
        "model_version": status["model_version"],
    }


if __name__ == "__main__":
    uvicorn.run(app, host=config.HOST, port=config.PORT)
