# Solra GPU Scheduler

多模型 CUDA 显存管理与负载均衡服务。

## 架构

```
AI 推理服务群 (LLM/TTS/Embedding/Safety)
         │  POST /api/v1/models/load
         ▼
    GPU Scheduler (本服务)
         │  NVML 实时监控
         ▼
    NVIDIA GPU(s)
```

## 核心功能

- **NVML 监控**：实时采集 GPU 显存/利用率
- **智能分配**：选择空闲显存最多的 GPU
- **LRU 淘汰**：显存不足时淘汰低优先级/最久未使用的模型
- **安全水位**：80% 显存占用上限，预留缓冲

## API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/metrics` | GET | Prometheus 指标 |
| `/api/v1/models/load` | POST | 加载模型 |
| `/api/v1/models/{id}/unload` | POST | 卸载模型 |
| `/api/v1/gpus` | GET | GPU 状态列表 |
| `/api/v1/models` | GET | 已加载模型列表 |

## 运行

```bash
# 本地 (Mock GPU)
cd ai-services/gpu-scheduler
pip install -e ".[dev]"
uvicorn src.main:app --reload --port 8100

# Docker
docker build -t solra-gpu-scheduler .
docker run --gpus all -p 8100:8100 solra-gpu-scheduler
```
