"""GPU Monitor — NVML 硬件状态采集"""

from __future__ import annotations

import logging
from typing import Dict

logger = logging.getLogger(__name__)


class GpuMonitor:
    """封装 pynvml 调用，采集 GPU 显存/利用率/温度"""

    def __init__(self):
        self._use_nvml = False
        try:
            import pynvml
            pynvml.nvmlInit()
            self._nvml = pynvml
            self._use_nvml = True
            logger.info(f"NVML initialized, GPU count: {self._nvml.nvmlDeviceGetCount()}")
        except Exception:
            logger.warning("NVML not available, using mock GPU stats")
            self._nvml = None

    def snapshot(self) -> Dict[int, dict]:
        """返回 {gpu_id: {name, total_vram_bytes, vram_used_bytes, utilization_pct}}"""
        if self._use_nvml and self._nvml:
            return self._real_snapshot()
        return self._mock_snapshot()

    def _real_snapshot(self) -> Dict[int, dict]:
        result = {}
        count = self._nvml.nvmlDeviceGetCount()
        for i in range(count):
            handle = self._nvml.nvmlDeviceGetHandleByIndex(i)
            mem_info = self._nvml.nvmlDeviceGetMemoryInfo(handle)
            util = self._nvml.nvmlDeviceGetUtilizationRates(handle)
            name = self._nvml.nvmlDeviceGetName(handle)
            result[i] = {
                "name": name,
                "total_vram_bytes": mem_info.total,
                "vram_used_bytes": mem_info.used,
                "utilization_pct": util.gpu,
            }
        return result

    def _mock_snapshot(self) -> Dict[int, dict]:
        """开发环境 Mock — 模拟单卡 A10 24GB"""
        return {
            0: {
                "name": "NVIDIA A10 (mock)",
                "total_vram_bytes": 24 * 1024 ** 3,
                "vram_used_bytes": 2 * 1024 ** 3,
                "utilization_pct": 5,
            }
        }

    def recommend_gpu(self, required_bytes: int) -> int | None:
        """选择空闲显存最多的 GPU"""
        best_gpu = None
        best_free = 0
        for gpu_id, stats in self.snapshot().items():
            free = stats["total_vram_bytes"] - stats["vram_used_bytes"]
            if free >= required_bytes and free > best_free:
                best_free = free
                best_gpu = gpu_id
        return best_gpu
