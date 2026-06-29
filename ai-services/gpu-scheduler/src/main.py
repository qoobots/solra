"""
Solra GPU Scheduler — 多模型 CUDA 显存管理与负载均衡

职责：
- 实时监控 GPU 显存/利用率 (NVML)
- 模型加载/卸载调度 (LRU 淘汰 + 显存水位)
- 为 LLM/TTS/Embedding/Safety 等推理服务提供统一的 GPU 分配接口
"""
from __future__ import annotations

import asyncio
import logging
import time
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, HTTPException
from prometheus_client import Gauge, generate_latest
from pydantic import BaseModel, Field

from .monitor import GpuMonitor
from .scheduler import ModelScheduler, ModelId, ModelType, MemoryBudget

logger = logging.getLogger("gpu-scheduler")

# ---- Globals ----
monitor: Optional[GpuMonitor] = None
scheduler: Optional[ModelScheduler] = None

# ---- Prometheus Metrics ----
METRIC_GPU_MEMORY_USED = Gauge("solra_gpu_vram_used_bytes", "Used GPU VRAM", ["gpu_id"])
METRIC_GPU_UTILIZATION = Gauge("solra_gpu_utilization_pct", "GPU utilization %", ["gpu_id"])
METRIC_MODEL_LOADED = Gauge("solra_model_loaded", "Model loaded state", ["model_id"])

# ---- Lifecycle ----
@asynccontextmanager
async def lifespan(app: FastAPI):
    global monitor, scheduler
    monitor = GpuMonitor()
    scheduler = ModelScheduler(monitor)
    logger.info("GPU Scheduler started")
    yield
    # shutdown: evict all models
    if scheduler:
        await scheduler.shutdown()
    logger.info("GPU Scheduler stopped")


app = FastAPI(title="Solra GPU Scheduler", version="0.1.0", lifespan=lifespan)

# ---- Health ----
@app.get("/health")
async def health():
    return {"status": "UP"}

@app.get("/metrics")
async def metrics():
    if monitor:
        for gpu_id, stats in monitor.snapshot().items():
            METRIC_GPU_MEMORY_USED.labels(gpu_id=str(gpu_id)).set(stats["vram_used_bytes"])
            METRIC_GPU_UTILIZATION.labels(gpu_id=str(gpu_id)).set(stats["utilization_pct"])
    return generate_latest()

# ---- Request/Response models ----
class LoadRequest(BaseModel):
    model_id: str = Field(..., description="模型标识 e.g. qwen2.5-7b, cosyvoice2")
    model_type: str = Field(..., description="LLM/TTS/EMBEDDING/SAFETY")
    min_vram_bytes: int = Field(default=0)
    max_vram_bytes: int = Field(default=0)
    priority: int = Field(default=0, ge=0, le=10)

class LoadResponse(BaseModel):
    success: bool
    model_id: str
    gpu_id: int
    endpoint: str = ""
    message: str = ""

class GpuStatus(BaseModel):
    gpu_id: int
    name: str
    total_vram_bytes: int
    used_vram_bytes: int
    free_vram_bytes: int
    utilization_pct: int
    loaded_models: list[str]

# ---- API ----
@app.post("/api/v1/models/load", response_model=LoadResponse)
async def load_model(req: LoadRequest):
    """请求加载模型到 GPU"""
    model_id = ModelId(req.model_id, ModelType(req.model_type))
    budget = MemoryBudget(min_bytes=req.min_vram_bytes, max_bytes=req.max_vram_bytes)
    try:
        result = await scheduler.load(model_id, budget, priority=req.priority)
        return LoadResponse(
            success=True,
            model_id=req.model_id,
            gpu_id=result.gpu_id,
            endpoint=result.endpoint,
            message="loaded",
        )
    except RuntimeError as e:
        raise HTTPException(status_code=507, detail=str(e))

@app.post("/api/v1/models/{model_id}/unload")
async def unload_model(model_id: str):
    """卸载模型释放显存"""
    await scheduler.unload(ModelId(model_id, ModelType("LLM")))
    return {"success": True}

@app.get("/api/v1/gpus")
async def list_gpus() -> list[GpuStatus]:
    """所有 GPU 状态"""
    if not monitor or not scheduler:
        return []
    snap = monitor.snapshot()
    result = []
    for gpu_id, stats in snap.items():
        result.append(GpuStatus(
            gpu_id=gpu_id,
            name=stats["name"],
            total_vram_bytes=stats["total_vram_bytes"],
            used_vram_bytes=stats["vram_used_bytes"],
            free_vram_bytes=stats["total_vram_bytes"] - stats["vram_used_bytes"],
            utilization_pct=stats["utilization_pct"],
            loaded_models=scheduler.get_loaded_models(gpu_id),
        ))
    return result

@app.get("/api/v1/models")
async def get_loaded_models():
    """当前已加载的所有模型"""
    if not scheduler:
        return {}
    return scheduler.get_all_loaded()
