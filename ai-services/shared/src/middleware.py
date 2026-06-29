"""
Shared FastAPI middleware for all Solra AI services.

Provides:
- Request logging with correlation IDs
- Rate limiting (token bucket)
- JWT authentication (stub)
- Prometheus metrics (request count/latency histogram)
- OpenTelemetry tracing (stub)
"""

import time
import uuid
import threading
from typing import Callable, Dict, Optional

from fastapi import FastAPI, Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from prometheus_client import Counter, Histogram


# ---- Prometheus Metrics ----
METRIC_REQUESTS_TOTAL = Counter(
    "solra_http_requests_total",
    "Total HTTP requests",
    ["method", "path", "status_code", "service"],
)
METRIC_REQUEST_DURATION = Histogram(
    "solra_http_request_duration_seconds",
    "HTTP request duration in seconds",
    ["method", "path", "service"],
    buckets=[0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5, 5.0, 10.0],
)

# ---- Global state ----
_service_name: str = "unknown"
_logger_instance: Optional[Callable] = None


def set_service_name(name: str) -> None:
    """Set the service name for metrics labeling."""
    global _service_name
    _service_name = name


class RequestLoggingMiddleware(BaseHTTPMiddleware):
    """Log each incoming request with duration and correlation ID."""

    def __init__(self, app, logger=None):
        super().__init__(app)
        self._logger = logger

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        request_id = str(uuid.uuid4())[:8]
        request.state.request_id = request_id
        start_time = time.time()

        if self._logger:
            self._logger.info(
                "request_start",
                request_id=request_id,
                method=request.method,
                path=request.url.path,
            )

        response = await call_next(request)

        duration = time.time() - start_time
        if self._logger:
            self._logger.info(
                "request_end",
                request_id=request_id,
                status_code=response.status_code,
                duration_ms=round(duration * 1000, 2),
            )

        response.headers["X-Request-ID"] = request_id
        return response


class RateLimitMiddleware(BaseHTTPMiddleware):
    """Simple per-IP rate limiting with token bucket."""

    def __init__(self, app, max_requests: int = 100, window_seconds: int = 60):
        super().__init__(app)
        self.max_requests = max_requests
        self.window_seconds = window_seconds
        self._clients: Dict[str, list] = {}
        self._lock = threading.Lock()

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        client_ip = request.client.host if request.client else "unknown"
        now = time.time()

        with self._lock:
            if client_ip not in self._clients:
                self._clients[client_ip] = []
            # Clean expired entries
            self._clients[client_ip] = [
                t for t in self._clients[client_ip]
                if now - t < self.window_seconds
            ]

            if len(self._clients[client_ip]) >= self.max_requests:
                return Response(
                    content='{"error":"Too many requests"}',
                    status_code=429,
                    media_type="application/json",
                )

            self._clients[client_ip].append(now)

        return await call_next(request)


class PrometheusMetricsMiddleware(BaseHTTPMiddleware):
    """
    Record Prometheus metrics for every HTTP request.

    Tracks: total request count, request duration histogram.
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        start_time = time.time()
        response = await call_next(request)
        duration = time.time() - start_time

        path = request.url.path
        # Normalize path for cardinality (e.g., /users/123 → /users/{id})
        route = request.scope.get("route")
        if route is not None:
            path = route.path

        METRIC_REQUESTS_TOTAL.labels(
            method=request.method,
            path=path,
            status_code=str(response.status_code),
            service=_service_name,
        ).inc()
        METRIC_REQUEST_DURATION.labels(
            method=request.method,
            path=path,
            service=_service_name,
        ).observe(duration)

        return response


class TracingMiddleware(BaseHTTPMiddleware):
    """
    OpenTelemetry tracing stub.

    Injects trace_id into request state and response headers.
    Full OTel SDK integration is planned for production.
    """

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        trace_id = request.headers.get("X-Trace-ID", str(uuid.uuid4())[:16])
        request.state.trace_id = trace_id

        response = await call_next(request)
        response.headers["X-Trace-ID"] = trace_id
        return response


def setup_middleware(
    app: FastAPI,
    rate_limit: int = 100,
    service_name: str = "ai-service",
    logger=None,
    enable_metrics: bool = True,
    enable_tracing: bool = False,
) -> None:
    """
    Install all shared middleware on a FastAPI application.

    Args:
        app: FastAPI application instance.
        rate_limit: Max requests per window per IP.
        service_name: Service name for Prometheus labels.
        logger: Optional structlog/logger instance.
        enable_metrics: Whether to enable Prometheus metrics middleware.
        enable_tracing: Whether to enable tracing stub middleware.
    """
    set_service_name(service_name)

    # Order matters: outermost first
    if enable_tracing:
        app.add_middleware(TracingMiddleware)

    app.add_middleware(RequestLoggingMiddleware, logger=logger)
    app.add_middleware(RateLimitMiddleware, max_requests=rate_limit)

    if enable_metrics:
        app.add_middleware(PrometheusMetricsMiddleware)

    if logger:
        logger.info(
            "shared_middleware_installed",
            rate_limit=rate_limit,
            service=service_name,
            metrics=enable_metrics,
            tracing=enable_tracing,
        )
