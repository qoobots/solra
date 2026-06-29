"""API routes for Text-to-Speech synthesis."""

import json
import base64
import time
import logging

from fastapi import APIRouter, Depends, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.responses import StreamingResponse

from models.schemas import (
    TTSRequest,
    TTSResponse,
    ModelStatus,
    AudioFormat,
)
from core.tts_engine import TTSEngine
from api.dependencies import get_engine

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["tts"])


@router.post(
    "/synthesize",
    response_model=TTSResponse,
    summary="Synthesize text to speech",
)
async def synthesize(
    request: TTSRequest,
    engine: TTSEngine = Depends(get_engine),
):
    """
    Convert text to speech audio.

    Returns base64-encoded audio data in the requested format.
    """
    start = time.perf_counter()
    try:
        wav_bytes, duration = engine.synthesize(
            text=request.text,
            voice=request.voice.value,
            speed=request.speed,
            pitch=request.pitch,
            sample_rate=request.sample_rate,
        )
        elapsed = (time.perf_counter() - start) * 1000

        audio_b64 = base64.b64encode(wav_bytes).decode("utf-8")

        return TTSResponse(
            audio_data=audio_b64,
            format=request.format,
            sample_rate=request.sample_rate,
            duration_sec=round(duration, 2),
            text_length=len(request.text),
            processing_time_ms=round(elapsed, 2),
            model_name=engine.model_name,
        )
    except Exception as e:
        logger.error(f"TTS synthesis failed: {e}")
        raise HTTPException(status_code=500, detail=f"Synthesis failed: {str(e)}")


@router.post(
    "/synthesize/stream",
    summary="Stream text-to-speech audio via SSE",
)
async def synthesize_stream(
    request: TTSRequest,
    engine: TTSEngine = Depends(get_engine),
):
    """
    Stream TTS audio via Server-Sent Events.

    Each event contains a base64-encoded audio chunk.
    """
    async def event_generator():
        try:
            for chunk_idx, audio_bytes, is_final in engine.synthesize_stream(
                text=request.text,
                voice=request.voice.value,
                speed=request.speed,
                pitch=request.pitch,
                sample_rate=request.sample_rate,
            ):
                chunk_b64 = base64.b64encode(audio_bytes).decode("utf-8")
                event = {
                    "chunk_index": chunk_idx,
                    "audio_chunk": chunk_b64,
                    "is_final": is_final,
                }
                yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

            yield "data: [DONE]\n\n"

        except Exception as e:
            logger.error(f"Streaming TTS error: {e}")
            error_event = {"error": str(e), "is_final": True}
            yield f"data: {json.dumps(error_event, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


@router.get(
    "/model/status",
    response_model=ModelStatus,
    summary="Get TTS model status and voice list",
)
async def get_model_status(
    engine: TTSEngine = Depends(get_engine),
):
    """Get current model status and available voice presets."""
    return ModelStatus(**engine.get_model_status())


@router.get(
    "/voices",
    summary="List available voice presets",
)
async def list_voices(
    engine: TTSEngine = Depends(get_engine),
):
    """List all available voice presets with metadata."""
    status = engine.get_model_status()
    return {"voices": status["available_voices"]}


@router.websocket("/ws/synthesize")
async def websocket_synthesize(
    websocket: WebSocket,
):
    """
    WebSocket endpoint for real-time TTS synthesis.

    Accepts JSON messages with TTSRequest fields and streams audio chunks back.
    """
    await websocket.accept()
    engine = get_engine()

    try:
        while True:
            data = await websocket.receive_json()

            text = data.get("text", "")
            voice = data.get("voice", "female_warm")
            speed = float(data.get("speed", 1.0))
            pitch = float(data.get("pitch", 1.0))
            sample_rate = int(data.get("sample_rate", 24000))

            if not text:
                await websocket.send_json({"error": "text is required"})
                continue

            try:
                for chunk_idx, audio_bytes, is_final in engine.synthesize_stream(
                    text=text, voice=voice, speed=speed,
                    pitch=pitch, sample_rate=sample_rate,
                ):
                    chunk_b64 = base64.b64encode(audio_bytes).decode("utf-8")
                    await websocket.send_json({
                        "chunk_index": chunk_idx,
                        "audio_chunk": chunk_b64,
                        "is_final": is_final,
                    })

                await websocket.send_json({"type": "done"})

            except Exception as e:
                await websocket.send_json({"error": str(e)})

    except WebSocketDisconnect:
        logger.info("WebSocket client disconnected")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
