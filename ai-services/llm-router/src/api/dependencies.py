"""FastAPI dependency injection for shared services."""

from functools import lru_cache

from core.router import RoutingEngine
from core.rate_limiter import TokenBucketRateLimiter
from inference.clients import BackendRegistry


# Singleton registry — shared across all requests
_registry: BackendRegistry | None = None
_routing_engine: RoutingEngine | None = None
_rate_limiter: TokenBucketRateLimiter | None = None


@lru_cache()
def get_registry() -> BackendRegistry:
    """Get or create the singleton backend registry."""
    global _registry
    if _registry is None:
        _registry = BackendRegistry()
    return _registry


@lru_cache()
def get_routing_engine() -> RoutingEngine:
    """Get or create the singleton routing engine."""
    global _routing_engine
    if _routing_engine is None:
        _routing_engine = RoutingEngine(get_registry())
    return _routing_engine


@lru_cache()
def get_rate_limiter() -> TokenBucketRateLimiter:
    """Get or create the singleton rate limiter."""
    global _rate_limiter
    if _rate_limiter is None:
        _rate_limiter = TokenBucketRateLimiter()
    return _rate_limiter

