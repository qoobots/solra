"""Unit tests for the LLM routing engine."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from ..models.schemas import ChatRequest, Message
from ..core.router import RoutingEngine
from ..inference.clients import BackendRegistry


@pytest.fixture
def simple_request():
    return ChatRequest(
        conversation_id="conv-001",
        user_id="user-001",
        space_id="space-001",
        messages=[Message(role="user", content="Hello!")],
        max_tokens=256,
        temperature=0.7,
    )


@pytest.fixture
def complex_request():
    return ChatRequest(
        conversation_id="conv-002",
        user_id="user-002",
        space_id="space-002",
        messages=[
            Message(role="system", content="You are a creative writing assistant."),
            Message(role="user", content="Write a detailed story about an AI explorer in a virtual world. " * 10),
        ],
        max_tokens=2048,
        temperature=0.9,
    )


@pytest.fixture
def streaming_request():
    return ChatRequest(
        conversation_id="conv-003",
        user_id="user-003",
        space_id="space-003",
        messages=[Message(role="user", content="Hi!")],
        max_tokens=128,
        stream=True,
    )


@pytest.fixture
def force_cloud_request():
    return ChatRequest(
        conversation_id="conv-004",
        user_id="user-004",
        space_id="space-004",
        messages=[Message(role="user", content="Generate a complex 3D scene description")],
        max_tokens=256,
        metadata={"force_cloud": True},
    )


@pytest.fixture
def registry():
    return BackendRegistry()


@pytest.fixture
def engine(registry):
    return RoutingEngine(registry)


class TestRoutingEngine:
    """Tests for the RoutingEngine class."""

    @pytest.mark.asyncio
    async def test_cost_optimized_simple_task_to_on_device(self, engine, simple_request):
        """Simple tasks should route to on-device backend."""
        decision = await engine.decide(simple_request)
        assert decision.backend == "on_device"
        assert "on_device" in decision.reason.lower()
        assert decision.fallback_available is True

    @pytest.mark.asyncio
    async def test_cost_optimized_complex_task_to_cloud(self, engine, complex_request):
        """Complex tasks should route to cloud backend."""
        decision = await engine.decide(complex_request)
        assert decision.backend == "cloud"
        assert decision.fallback_available is True

    @pytest.mark.asyncio
    async def test_cost_optimized_streaming_to_on_device(self, engine, streaming_request):
        """Streaming short tasks should route to on-device."""
        decision = await engine.decide(streaming_request)
        assert decision.backend == "on_device"

    @pytest.mark.asyncio
    async def test_cost_optimized_force_cloud(self, engine, force_cloud_request):
        """Force cloud flag should override simple task routing."""
        decision = await engine.decide(force_cloud_request)
        assert decision.backend == "cloud"

    @pytest.mark.asyncio
    async def test_latency_first_to_on_device(self, registry, simple_request):
        """Latency-first should prefer on-device."""
        engine = RoutingEngine(registry)
        engine.strategy = "latency_first"
        decision = await engine.decide(simple_request)
        assert decision.backend == "on_device"

    @pytest.mark.asyncio
    async def test_quality_first_to_cloud(self, registry, simple_request):
        """Quality-first should prefer cloud even for simple tasks."""
        engine = RoutingEngine(registry)
        engine.strategy = "quality_first"
        decision = await engine.decide(simple_request)
        assert decision.backend == "cloud"

    @pytest.mark.asyncio
    async def test_no_healthy_backends_raises(self, engine, simple_request):
        """Should raise when no backends are healthy."""
        engine.registry._backends.clear()
        with pytest.raises(RuntimeError, match="No healthy"):
            await engine.decide(simple_request)

    @pytest.mark.asyncio
    async def test_only_fallback_available(self, registry, simple_request):
        """When only fallback is healthy, it should be used."""
        engine = RoutingEngine(registry)
        # Mark both primary backends as unhealthy
        registry._backends["on_device"]._healthy = False
        registry._backends["cloud"]._healthy = False
        decision = await engine.decide(simple_request)
        assert decision.backend == "fallback"
        assert decision.fallback_available is False


class TestBackendRegistry:
    """Tests for the BackendRegistry class."""

    def test_registry_initialization(self, registry):
        """Registry should have 3 default backends."""
        all_backends = registry.list_all()
        assert len(all_backends) == 3
        names = {b.config.name for b in all_backends}
        assert names == {"on_device", "cloud", "fallback"}

    def test_registry_healthy(self, registry):
        """Default backends should be healthy."""
        healthy = registry.list_healthy()
        assert len(healthy) == 3

    def test_registry_get_by_name(self, registry):
        """Should retrieve backend by name."""
        backend = registry.get("cloud")
        assert backend is not None
        assert backend.config.type == "cloud"

    def test_registry_infos(self, registry):
        """Should return backend info list."""
        infos = registry.get_infos()
        assert len(infos) == 3
        assert all(i.name in {"on_device", "cloud", "fallback"} for i in infos)
