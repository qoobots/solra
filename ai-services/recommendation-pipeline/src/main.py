"""推荐流水线服务主入口。协同过滤 + 内容推荐 + 热门推荐混合策略。"""
import uvicorn
from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI(title="Solra Recommendation Pipeline", version="0.1.0")
Instrumentator().instrument(app).expose(app)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "recommendation-pipeline"}


# TODO: P1 — 协同过滤模型训练与推理
# TODO: P1 — 基于用户行为的实时推荐
# TODO: gRPC server 暴露 RecommendationService


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8102)
