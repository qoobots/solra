"""API routes for LLM Router."""

from fastapi import APIRouter, HTTPException, BackgroundTasks
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
import structlog

router = APIRouter(tags=["llm"])
logger = structlog.get_logger(__name__)


class ChatRequest(BaseModel):
    conversation_id: str
    user_id: str
    space_id: str
    messages: List[Dict[str, str]]  # [{"role": "user/system/assistant", "content": "..."}]
    avatar_id: Optional[str] = None
    max_tokens: int = Field(default=512, ge=1, le=4096)
    temperature: float = Field(default=0.7, ge=0.0, le=2.0)
    stream: bool = False
    metadata: Optional[Dict[str, Any]] = None


class ChatResponse(BaseModel):
    conversation_id: str
    turn_id: str
    content: str
    model_used: str  # "on_device" | "cloud" | "fallback"
    tokens_used: int
    finish_reason: str  # "stop" | "length" | "error"


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Route a chat completion request to the appropriate LLM backend.

    Decision logic:
    1. Short/edge tasks → on-device (low latency, free)
    2. Complex/creative tasks → cloud (higher quality)
    3. On-device failure → fallback to cloud
    """
    logger.info("Chat request received",
                conversation_id=request.conversation_id,
                message_count=len(request.messages))

    # TODO: Implement routing logic
    # - Analyze request complexity
    # - Check on-device availability
    # - Route to appropriate backend
    # - Handle fallback

    return ChatResponse(
        conversation_id=request.conversation_id,
        turn_id="stub-turn-001",
        content="[Stub] LLM Router is operational but not connected to backends.",
        model_used="stub",
        tokens_used=0,
        finish_reason="stop",
    )


@router.get("/backends")
async def list_backends():
    """List available LLM backends and their status."""
    return {
        "backends": [
            {"name": "on_device", "status": "not_connected", "type": "llama.cpp"},
            {"name": "cloud", "status": "not_connected", "type": "qwen2.5-7b"},
            {"name": "fallback", "status": "not_connected", "type": "qwen2.5-1.5b"},
        ]
    }
