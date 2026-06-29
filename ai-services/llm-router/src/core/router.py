"""Core business logic for LLM routing decisions."""

import uuid
from typing import Optional
import structlog

from config import settings
from models.schemas import ChatRequest, RoutingDecision, LLMBackendConfig
from inference.clients import BackendRegistry, BaseLLMClient

logger = structlog.get_logger(__name__)


def _backend_name(client: BaseLLMClient) -> str:
    return client.config.name

def _backend_endpoint(client: BaseLLMClient) -> str:
    return client.config.endpoint


class RoutingEngine:
    """
    Intelligent LLM routing engine.

    Decision strategies:
    - cost_optimized: Prefer on-device for simple tasks, cloud for complex ones
    - latency_first: Always prefer the lowest-latency backend
    - quality_first: Always prefer the highest-quality (cloud) backend
    """

    # Threshold for routing to on-device vs cloud
    SIMPLE_TASK_MAX_TOKENS = 512
    SIMPLE_TASK_ESTIMATED_INPUT_TOKENS = 1024

    def __init__(self, registry: BackendRegistry):
        self.registry = registry
        self.strategy = settings.ROUTING_STRATEGY

    async def decide(self, request: ChatRequest) -> RoutingDecision:
        """Make a routing decision for the given chat request."""
        backends = self.registry.list_healthy()

        if not backends:
            raise RuntimeError("No healthy LLM backends available")

        on_device = [b for b in backends if b.config.type == "on_device"]
        cloud = [b for b in backends if b.config.type == "cloud"]
        fallback = [b for b in backends if b.config.type == "fallback"]

        turn_id = str(uuid.uuid4())

        if self.strategy == "latency_first":
            return self._route_latency_first(request, on_device, cloud, fallback, turn_id)
        elif self.strategy == "quality_first":
            return self._route_quality_first(request, cloud, on_device, fallback, turn_id)
        else:
            return self._route_cost_optimized(request, on_device, cloud, fallback, turn_id)

    def _mk_decision(
        self, backend: BaseLLMClient, strategy: str, reason: str, fallback_available: bool
    ) -> RoutingDecision:
        return RoutingDecision(
            backend=_backend_name(backend),
            endpoint=_backend_endpoint(backend),
            strategy=strategy,
            reason=reason,
            fallback_available=fallback_available,
        )

    def _route_cost_optimized(
        self,
        request: ChatRequest,
        on_device: list,
        cloud: list,
        fallback: list,
        turn_id: str,
    ) -> RoutingDecision:
        """Cost-optimized: simple→on-device, complex→cloud."""
        is_simple = self._is_simple_task(request)
        has_fallback = len(fallback) > 0

        if is_simple and on_device:
            return self._mk_decision(
                on_device[0], "cost_optimized",
                f"Simple task ({request.estimated_input_tokens} input tokens, {request.max_tokens} max tokens) → on_device",
                bool(cloud or fallback),
            )

        if cloud:
            return self._mk_decision(
                cloud[0], "cost_optimized",
                f"Complex task ({request.estimated_input_tokens} input tokens) → cloud",
                has_fallback,
            )

        if on_device:
            return self._mk_decision(
                on_device[0], "cost_optimized",
                "No cloud backend available → on_device",
                has_fallback,
            )

        if fallback:
            return self._mk_decision(
                fallback[0], "cost_optimized",
                "All primary backends unavailable → fallback",
                False,
            )

        raise RuntimeError("No LLM backend available")

    def _route_latency_first(
        self, request, on_device, cloud, fallback, turn_id
    ) -> RoutingDecision:
        has_fallback = len(fallback) > 0
        if on_device:
            return self._mk_decision(on_device[0], "latency_first", "Lowest latency (on-device)", bool(cloud or fallback))
        if cloud:
            return self._mk_decision(cloud[0], "latency_first", "Cloud (no on-device)", has_fallback)
        if fallback:
            return self._mk_decision(fallback[0], "latency_first", "Fallback (no primary)", False)
        raise RuntimeError("No LLM backend available")

    def _route_quality_first(
        self, request, cloud, on_device, fallback, turn_id
    ) -> RoutingDecision:
        has_fallback = len(fallback) > 0
        if cloud:
            return self._mk_decision(cloud[0], "quality_first", "Highest quality (cloud)", bool(on_device or fallback))
        if on_device:
            return self._mk_decision(on_device[0], "quality_first", "On-device (no cloud)", has_fallback)
        if fallback:
            return self._mk_decision(fallback[0], "quality_first", "Fallback (no primary)", False)
        raise RuntimeError("No LLM backend available")

    def _is_simple_task(self, request: ChatRequest) -> bool:
        is_low_tokens = (
            request.max_tokens <= self.SIMPLE_TASK_MAX_TOKENS
            and request.estimated_input_tokens <= self.SIMPLE_TASK_ESTIMATED_INPUT_TOKENS
        )
        force_cloud = request.metadata.get("force_cloud", False)
        if force_cloud:
            return False
        if request.stream and is_low_tokens:
            return True
        return is_low_tokens
