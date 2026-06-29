"""嵌入服务主入口。文本/多模态向量嵌入与相似度计算。"""
import logging
import uvicorn
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_fastapi_instrumentator import Instrumentator

from api.routes import router as embedding_router
from api.dependencies import get_engine
from config import config

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(name)s: %(message)s")
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load model on startup, unload on shutdown."""
    logger.info(f"Starting Embedding Service on port {config.PORT}")
    engine = get_engine()
    logger.info(f"Embedding engine ready: {engine.model_name}, dim={engine.dimensions}")
    yield
    engine.unload_model()
    logger.info("Embedding Service shut down")


app = FastAPI(
    title="Solra Embedding Service",
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

app.include_router(embedding_router)


@app.get("/health")
async def health():
    engine = get_engine()
    return {
        "status": "ok",
        "service": "embedding-service",
        "model": engine.model_name,
        "model_loaded": engine.is_loaded,
    }


if __name__ == "__main__":
    uvicorn.run(app, host=config.HOST, port=config.PORT)
