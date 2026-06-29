# Solra LLM Router

智能 LLM 推理路由与调度服务。根据请求复杂度、token 数量和后端可用性，自动选择最优的推理后端（端侧/云端/回退）。

## 功能

- **智能路由**: 根据任务复杂度自动选择端侧 SLM 或云端 LLM
- **三种策略**: `cost_optimized`（默认）、`latency_first`、`quality_first`
- **自动回退**: 主后端失败时自动切换到备用后端
- **流式响应**: 支持 Server-Sent Events (SSE) 流式聊天
- **速率限制**: Token Bucket 算法保护 API 免于过载
- **健康检查**: 实时监控各后端可用性
- **Prometheus 指标**: 完整的可观测性支持

## API

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/v1/chat` | 聊天补全（智能路由） |
| `POST` | `/api/v1/chat/stream` | 流式聊天补全 (SSE) |
| `GET` | `/api/v1/backends` | 列出所有后端及状态 |
| `POST` | `/api/v1/backends/health-check` | 触发健康检查 |
| `GET` | `/api/v1/rate-limits/{user_id}` | 查询用户速率限制状态 |
| `GET` | `/health` | K8s 兼容健康检查 |
| `GET` | `/metrics` | Prometheus 指标 |

## 运行

```bash
pip install -e .
uvicorn src.main:app --reload
```

## 测试

```bash
PYTHONPATH=src pytest src/tests/ -v
```
