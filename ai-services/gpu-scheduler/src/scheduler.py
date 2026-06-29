"""Model Scheduler — LRU 淘汰 + 显存水位管理"""

from __future__ import annotations

import asyncio
import logging
import time
from dataclasses import dataclass, field
from enum import Enum
from typing import Dict, List, Optional

logger = logging.getLogger(__name__)


class ModelType(str, Enum):
    LLM = "LLM"
    TTS = "TTS"
    EMBEDDING = "EMBEDDING"
    SAFETY = "SAFETY"


@dataclass(frozen=True)
class ModelId:
    name: str
    type: ModelType


@dataclass
class MemoryBudget:
    min_bytes: int = 0
    max_bytes: int = 0


@dataclass
class Slot:
    model_id: ModelId
    gpu_id: int
    endpoint: str
    allocated_bytes: int
    last_access: float = field(default_factory=time.time)
    priority: int = 0

    def touch(self):
        self.last_access = time.time()


class ModelScheduler:
    """GPU 模型加载/卸载调度器

    策略：
    - 首选空闲显存最多的 GPU
    - 显存不足时 LRU 淘汰低优先级模型
    - 预留 20% 安全水位
    """

    SAFETY_WATERMARK = 0.80  # 80% 以下才加载新模型

    def __init__(self, monitor):
        self._monitor = monitor
        self._slots: Dict[str, Slot] = {}  # model_key → slot
        self._lock = asyncio.Lock()

    async def load(self, model_id: ModelId, budget: MemoryBudget, priority: int = 0) -> Slot:
        async with self._lock:
            key = f"{model_id.type.value}:{model_id.name}"
            if key in self._slots:
                self._slots[key].touch()
                logger.info(f"Model {key} already loaded")
                return self._slots[key]

            need = budget.min_bytes or budget.max_bytes or (4 * 1024 ** 3)  # default 4GiB
            gpu_id = self._monitor.recommend_gpu(need)

            if gpu_id is None:
                # try evict LRU
                freed = await self._evict_lru(need)
                if freed < need:
                    raise RuntimeError(f"Insufficient GPU memory: need {need>>30}GiB, freed {freed>>30}GiB")
                gpu_id = self._monitor.recommend_gpu(need)
                if gpu_id is None:
                    raise RuntimeError("GPU allocation failed after eviction")

            endpoint = f"http://{model_id.type.value.lower()}-service:8000"
            slot = Slot(
                model_id=model_id,
                gpu_id=gpu_id,
                endpoint=endpoint,
                allocated_bytes=need,
                priority=priority,
            )
            self._slots[key] = slot
            logger.info(f"Loaded {key} on GPU {gpu_id} ({need>>20}MiB)")
            return slot

    async def unload(self, model_id: ModelId):
        async with self._lock:
            key = f"{model_id.type.value}:{model_id.name}"
            if key in self._slots:
                slot = self._slots.pop(key)
                logger.info(f"Unloaded {key} from GPU {slot.gpu_id} ({slot.allocated_bytes>>20}MiB)")

    async def _evict_lru(self, need_bytes: int) -> int:
        """LRU 淘汰直到释放足够显存"""
        freed = 0
        items = sorted(self._slots.items(),
                       key=lambda x: (x[1].priority, -x[1].last_access))
        for key, slot in items:
            if freed >= need_bytes:
                break
            self._slots.pop(key)
            freed += slot.allocated_bytes
            logger.info(f"Evicted {key} (priority={slot.priority}, idle={time.time()-slot.last_access:.0f}s)")
        return freed

    def get_loaded_models(self, gpu_id: int) -> List[str]:
        return [key for key, s in self._slots.items() if s.gpu_id == gpu_id]

    def get_all_loaded(self) -> Dict[str, str]:
        return {key: f"GPU {s.gpu_id}" for key, s in self._slots.items()}

    async def shutdown(self):
        async with self._lock:
            self._slots.clear()
            logger.info("All models evicted")
