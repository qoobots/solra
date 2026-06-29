"""LLM backend clients — on-device, cloud, and fallback backends."""

import time
from abc import ABC, abstractmethod
from datetime import datetime
from typing import List, Optional
import httpx
import structlog

from config import settings
from models.schemas import LLMBackendConfig, BackendInfo, ChatRequest, ChatResponse

logger = structlog.get_logger(__name__)


class BaseLLMClient(ABC):
    """Abstract base class for LLM backend clients."""

    def __init__(self, config: LLMBackendConfig):
        self.config = config
        self._healthy = True
        self._last_health_check: Optional[datetime] = None
        self._avg_latency_ms: float = 0.0

    @property
    def is_healthy(self) -> bool:
        return self._healthy

    @property
    def avg_latency_ms(self) -> float:
        return self._avg_latency_ms

    async def health_check(self) -> bool:
        """Check if the backend is healthy."""
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                start = time.time()
                resp = await client.get(f"{self.config.endpoint}/health")
                latency = (time.time() - start) * 1000
                self._avg_latency_ms = latency
                self._last_health_check = datetime.utcnow()
                self._healthy = resp.status_code == 200
                return self._healthy
        except Exception as e:
            logger.warning("health_check_failed", backend=self.config.name, error=str(e))
            self._healthy = False
            self._last_health_check = datetime.utcnow()
            return False

    @abstractmethod
    async def chat(self, request: ChatRequest, turn_id: str) -> ChatResponse:
        """Send a chat completion request to this backend."""
        ...

    def to_info(self) -> BackendInfo:
        return BackendInfo(
            name=self.config.name,
            type=self.config.type,
            endpoint=self.config.endpoint,
            max_tokens=self.config.max_tokens,
            priority=self.config.priority,
            is_healthy=self._healthy,
            last_health_check=self._last_health_check,
            avg_latency_ms=self._avg_latency_ms,
        )


class OnDeviceClient(BaseLLMClient):
    """Client for on-device SLM (Small Language Model) inference."""

    async def chat(self, request: ChatRequest, turn_id: str) -> ChatResponse:
        start = time.time()

        # Build the prompt from messages
        messages = [{"role": m.role, "content": m.content} for m in request.messages]

        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(
                    f"{self.config.endpoint}/v1/chat/completions",
                    json={
                        "messages": messages,
                        "max_tokens": min(request.max_tokens, self.config.max_tokens),
                        "temperature": request.temperature,
                        "stream": False,
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                tokens = data.get("usage", {}).get("total_tokens", 0)

        except httpx.HTTPStatusError as e:
            logger.error("on_device_error", status=e.response.status_code, detail=str(e))
            raise RuntimeError(f"On-device LLM returned error: {e.response.status_code}") from e
        except Exception as e:
            logger.error("on_device_exception", error=str(e))
            raise

        latency = (time.time() - start) * 1000
        self._avg_latency_ms = latency

        return ChatResponse(
            conversation_id=request.conversation_id,
            turn_id=turn_id,
            content=content,
            model_used="on_device",
            tokens_used=tokens,
            finish_reason=data["choices"][0].get("finish_reason", "stop"),
            latency_ms=round(latency, 2),
        )


class CloudLLMClient(BaseLLMClient):
    """Client for cloud LLM inference (e.g., Qwen2.5-7B on vLLM)."""

    async def chat(self, request: ChatRequest, turn_id: str) -> ChatResponse:
        start = time.time()

        messages = [{"role": m.role, "content": m.content} for m in request.messages]

        try:
            async with httpx.AsyncClient(timeout=60.0) as client:
                resp = await client.post(
                    f"{self.config.endpoint}/v1/chat/completions",
                    json={
                        "messages": messages,
                        "max_tokens": min(request.max_tokens, self.config.max_tokens),
                        "temperature": request.temperature,
                        "stream": False,
                    },
                )
                resp.raise_for_status()
                data = resp.json()
                content = data["choices"][0]["message"]["content"]
                tokens = data.get("usage", {}).get("total_tokens", 0)

        except httpx.HTTPStatusError as e:
            logger.error("cloud_llm_error", status=e.response.status_code, detail=str(e))
            raise RuntimeError(f"Cloud LLM returned error: {e.response.status_code}") from e
        except Exception as e:
            logger.error("cloud_llm_exception", error=str(e))
            raise

        latency = (time.time() - start) * 1000
        self._avg_latency_ms = latency

        return ChatResponse(
            conversation_id=request.conversation_id,
            turn_id=turn_id,
            content=content,
            model_used="cloud",
            tokens_used=tokens,
            finish_reason=data["choices"][0].get("finish_reason", "stop"),
            latency_ms=round(latency, 2),
        )


class FallbackClient(BaseLLMClient):
    """Fallback LLM client for emergency scenarios."""

    async def chat(self, request: ChatRequest, turn_id: str) -> ChatResponse:
        # Fallback generates a simple response when all primary backends are down
        logger.warning("using_fallback_backend",
                       conversation_id=request.conversation_id,
                       user_id=request.user_id)

        content = (
            "I'm sorry, I'm having trouble connecting to my knowledge base right now. "
            "Please try again in a moment."
        )

        return ChatResponse(
            conversation_id=request.conversation_id,
            turn_id=turn_id,
            content=content,
            model_used="fallback",
            tokens_used=0,
            finish_reason="error",
            latency_ms=0.0,
        )


class BackendRegistry:
    """Manages all registered LLM backends and their health status."""

    def __init__(self):
        self._backends: dict[str, BaseLLMClient] = {}
        self._initialize_defaults()

    def _initialize_defaults(self):
        """Initialize backends from settings."""
        self.register(LLMBackendConfig(
            name="on_device",
            endpoint=settings.ON_DEVICE_LLM_ENDPOINT,
            type="on_device",
            max_tokens=settings.MAX_ON_DEVICE_TOKENS,
            priority=1,
        ))

        self.register(LLMBackendConfig(
            name="cloud",
            endpoint=settings.CLOUD_LLM_ENDPOINT,
            type="cloud",
            max_tokens=settings.MAX_CLOUD_TOKENS,
            priority=2,
        ))

        self.register(LLMBackendConfig(
            name="fallback",
            endpoint=settings.FALLBACK_LLM_ENDPOINT,
            type="fallback",
            max_tokens=256,
            priority=9,
        ))

    def register(self, config: LLMBackendConfig):
        """Register a new backend."""
        if config.type == "cloud":
            client_cls = CloudLLMClient
        elif config.type == "fallback":
            client_cls = FallbackClient
        else:
            client_cls = OnDeviceClient

        self._backends[config.name] = client_cls(config)
        logger.info("backend_registered", name=config.name, type=config.type, endpoint=config.endpoint)

    def get(self, name: str) -> Optional[BaseLLMClient]:
        return self._backends.get(name)

    def list_all(self) -> List[BaseLLMClient]:
        return list(self._backends.values())

    def list_healthy(self) -> List[BaseLLMClient]:
        return sorted(
            [b for b in self._backends.values() if b.is_healthy],
            key=lambda b: b.config.priority,
        )

    async def check_all_health(self) -> dict[str, bool]:
        results = {}
        for name, client in self._backends.items():
            results[name] = await client.health_check()
        return results

    def get_infos(self) -> List[BackendInfo]:
        return [c.to_info() for c in self._backends.values()]
