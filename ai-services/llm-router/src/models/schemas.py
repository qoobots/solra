"""Pydantic data models for LLM Router."""

from datetime import datetime
from typing import List, Optional, Dict, Any
from pydantic import BaseModel, Field


class Message(BaseModel):
    """A single message in a conversation."""
    role: str = Field(..., pattern="^(system|user|assistant)$")
    content: str


class ChatRequest(BaseModel):
    """Incoming chat completion request."""
    conversation_id: str = Field(..., description="Conversation session ID")
    user_id: str = Field(..., description="Unique user identifier")
    space_id: str = Field(..., description="Current space context")
    messages: List[Message] = Field(..., min_length=1)
    avatar_id: Optional[str] = Field(None, description="Target avatar identifier")
    max_tokens: int = Field(512, ge=1, le=4096, description="Maximum tokens to generate")
    temperature: float = Field(0.7, ge=0.0, le=2.0, description="Sampling temperature")
    stream: bool = Field(False, description="Enable streaming response")
    metadata: Dict[str, Any] = Field(default_factory=dict)

    @property
    def estimated_input_tokens(self) -> int:
        """Rough estimate of input token count."""
        return sum(len(m.content) for m in self.messages) // 3


class ChatResponse(BaseModel):
    """Chat completion response."""
    conversation_id: str
    turn_id: str = Field(..., description="Unique turn identifier")
    content: str
    model_used: str = Field(..., description="Model backend used (on_device/cloud/fallback)")
    tokens_used: int = Field(0)
    finish_reason: str = Field("stop", description="stop/length/content_filter")
    latency_ms: float = Field(0.0, description="Inference latency in milliseconds")


class LLMBackendConfig(BaseModel):
    """Configuration for an LLM backend."""
    name: str
    endpoint: str
    type: str = Field(..., pattern="^(on_device|cloud|fallback)$")
    max_tokens: int = 512
    priority: int = Field(0, ge=0)
    is_healthy: bool = True


class BackendInfo(BaseModel):
    """Information about a registered LLM backend."""
    name: str
    type: str
    endpoint: str
    max_tokens: int
    priority: int
    is_healthy: bool
    last_health_check: Optional[datetime] = None
    avg_latency_ms: Optional[float] = None


class BackendStatus(BaseModel):
    """Aggregate backend status response."""
    backends: List[BackendInfo]
    routing_strategy: str
    total_backends: int
    healthy_backends: int


class RoutingDecision(BaseModel):
    """Result of routing decision."""
    backend: str
    endpoint: str
    strategy: str
    reason: str
    fallback_available: bool = False
