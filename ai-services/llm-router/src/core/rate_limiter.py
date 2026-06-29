"""Rate limiting middleware for LLM Router API."""

import time
from collections import defaultdict
from typing import Dict, Tuple

import structlog

from config import settings

logger = structlog.get_logger(__name__)


class TokenBucketRateLimiter:
    """Token bucket algorithm-based rate limiter.

    Each unique key (e.g., user_id) gets its own token bucket.
    Tokens refill at a configurable rate up to a maximum burst capacity.
    """

    def __init__(
        self,
        rate: float = None,
        burst: int = None,
    ):
        self.rate = rate or settings.RATE_LIMIT_REQUESTS_PER_SECOND
        self.burst = burst or max(10, self.rate * 2)
        self._buckets: Dict[str, Tuple[float, float]] = defaultdict(
            lambda: (self.burst, time.monotonic())
        )

    def allow(self, key: str) -> bool:
        """Check if a request is allowed for the given key.

        Returns True if allowed, False if rate limited.
        """
        now = time.monotonic()
        tokens, last_refill = self._buckets[key]

        # Refill tokens based on elapsed time
        elapsed = now - last_refill
        new_tokens = min(self.burst, tokens + elapsed * self.rate)
        self._buckets[key] = (new_tokens, now)

        if new_tokens >= 1.0:
            self._buckets[key] = (new_tokens - 1.0, now)
            return True

        logger.warning("rate_limit_exceeded", key=key, tokens=new_tokens)
        return False

    def get_remaining(self, key: str) -> float:
        """Get remaining tokens for the given key."""
        tokens, last_refill = self._buckets[key]
        now = time.monotonic()
        elapsed = now - last_refill
        new_tokens = min(self.burst, tokens + elapsed * self.rate)
        return max(0.0, new_tokens)

    def reset(self, key: str = None):
        """Reset rate limiter for a specific key or all keys."""
        if key:
            self._buckets.pop(key, None)
        else:
            self._buckets.clear()
