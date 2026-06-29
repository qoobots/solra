"""Unit tests for the rate limiter module."""

import time
import pytest

from core.rate_limiter import TokenBucketRateLimiter


class TestTokenBucketRateLimiter:
    """Tests for TokenBucketRateLimiter."""

    def test_initial_allow(self):
        """First request should always be allowed."""
        limiter = TokenBucketRateLimiter(rate=10.0, burst=20)
        assert limiter.allow("user-1") is True

    def test_burst_allows_multiple_requests(self):
        """Within burst capacity, multiple requests should be allowed."""
        limiter = TokenBucketRateLimiter(rate=10.0, burst=20)
        for _ in range(20):
            assert limiter.allow("user-2") is True
        # 21st request should be rejected
        assert limiter.allow("user-2") is False

    def test_different_keys_independent(self):
        """Rate limits are per-key."""
        limiter = TokenBucketRateLimiter(rate=10.0, burst=5)
        for _ in range(5):
            assert limiter.allow("user-a") is True
        assert limiter.allow("user-a") is False
        # Different key should still be allowed
        assert limiter.allow("user-b") is True

    def test_get_remaining(self):
        """Should report remaining tokens correctly."""
        limiter = TokenBucketRateLimiter(rate=10.0, burst=20)
        assert limiter.allow("user-3") is True
        remaining = limiter.get_remaining("user-3")
        assert remaining <= 19.0
        assert remaining >= 18.0

    def test_reset_key(self):
        """Reset should clear rate limit for a key."""
        limiter = TokenBucketRateLimiter(rate=10.0, burst=3)
        for _ in range(3):
            limiter.allow("user-4")
        assert limiter.allow("user-4") is False
        limiter.reset("user-4")
        assert limiter.allow("user-4") is True

    def test_reset_all(self):
        """Reset all should clear all rate limits."""
        limiter = TokenBucketRateLimiter(rate=10.0, burst=2)
        for _ in range(2):
            limiter.allow("user-5")
        assert limiter.allow("user-5") is False
        limiter.reset()
        assert limiter.allow("user-5") is True

    def test_refill_over_time(self):
        """Tokens should refill over time."""
        limiter = TokenBucketRateLimiter(rate=100.0, burst=5)
        # Exhaust burst
        for _ in range(5):
            limiter.allow("user-6")
        assert limiter.allow("user-6") is False
        # Wait for refill
        time.sleep(0.02)  # ~2 tokens refilled at rate=100
        assert limiter.allow("user-6") is True

    def test_custom_rate_and_burst(self):
        """Custom rate and burst should work."""
        limiter = TokenBucketRateLimiter(rate=50.0, burst=100)
        assert limiter.rate == 50.0
        assert limiter.burst == 100
        for _ in range(100):
            assert limiter.allow("user-7") is True
        assert limiter.allow("user-7") is False
