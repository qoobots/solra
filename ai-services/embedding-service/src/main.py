"""嵌入服务主入口。文本/多模态向量嵌入与相似度计算。"""
import uvicorn
from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI(title="Solra Embedding Service", version="0.1.0")
Instrumentator().instrument(app).expose(app)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "embedding-service"}


# TODO: P1 — 集成 text2vec-large-chinese / multilingual-e5 模型
# TODO: gRPC server 暴露 EmbeddingService


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8101)
