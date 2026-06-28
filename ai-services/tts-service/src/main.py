"""语音合成服务主入口。文本转语音，支持流式输出。"""
import uvicorn
from fastapi import FastAPI
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI(title="Solra TTS Service", version="0.1.0")
Instrumentator().instrument(app).expose(app)


@app.get("/health")
async def health():
    return {"status": "ok", "service": "tts-service"}


# TODO: P1 — VITS / ChatTTS / CosyVoice 模型集成
# TODO: P1 — 流式音频输出 WebSocket endpoint
# TODO: gRPC server 暴露 TTSService


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8103)
