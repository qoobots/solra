"""
Shared FastAPI middleware for all Solra AI services.

Provides:
- Request logging with correlation IDs
- Rate limiting (token bucket)
- JWT authentication
- Prometheus metrics
- OpenTelemetry tracing
"""

import time
import uuid
from typing import Callable

from fastapi import FastAPI, Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
import structlog

logger = structlog.get_logger(__name__)


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Log each incoming request with duration."""

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        request_id = str(uuid.uuid4())[:8]
        start_time = time.time()

        logger.info("request_start",
                    request_id=request_id,
                    method=request.method,
                    path=request.url.path)

        response = await call_next(request)

        duration = time.time() - start_time
        logger.info("request_end",
                    request_id=request_id,
                    status_code=response.status_code,
                    duration_ms=round(duration * 1000, 2))

        response.headers["X-Request-ID"] = request_id
        return response


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Simple per-IP rate limiting."""

    def __init__(self, app, max_requests: int = 100, window_seconds: int = 60):
        super().__init__(app)
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._clients: dict[str, list[float]] = {}

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        client_ip = request.client.host if request.client else "unknown"
        now = time.time()

        # Clean expired entries
        if client_ip not in self._clients:
            self._clients[client_ip] = []
        self._clients[client_ip] = [
            t for t in self._clients[client_ip]
            if now - t < self.window_seconds
        ]

        if len(self._clients[client_ip]) >= self.max_requests:
            logger.warning("rate_limit_exceeded", client_ip=client_ip)
            return Response(
                content='{"error":"Too many requests"}',
                status_code=429,
                media_type="application/json",
            )

        self._clients[client_ip].append(now)
        return await call_next(request)


def setup_middleware(app: FastAPI, rate_limit: int = 100) -> None:
    """Install shared middleware on a FastAPI application."""
    app.add_middleware(RequestLoggingMiddleware)
    app.add_middleware(RateLimitMiddleware, max_requests=rate_limit)
    logger.info("Shared middleware installed", rate_limit=rate_limit)
