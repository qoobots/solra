"""Application configuration."""

from pydantic_settings import BaseSettings
from typing import List


class Settings(BaseSettings):
    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 9091
    GRPC_PORT: int = 9091

    # CORS
    CORS_ORIGINS: List[str] = ["*"]

    # Redis (for request queue & rate limiting)
    REDIS_URL: str = "redis://localhost:6379/0"

    # Backend LLM endpoints
    ON_DEVICE_LLM_ENDPOINT: str = "http://localhost:9090"
    CLOUD_LLM_ENDPOINT: str = "http://localhost:9092"
    FALLBACK_LLM_ENDPOINT: str = "http://localhost:9099"

    # Routing strategy
    ROUTING_STRATEGY: str = "cost_optimized"  # cost_optimized | lowest_latency | highest_quality
    MAX_ON_DEVICE_TOKENS: int = 512
    MAX_CLOUD_TOKENS: int = 4096

    # Rate limiting
    RATE_LIMIT_REQUESTS_PER_SECOND: int = 100

    # Observability
    LOG_LEVEL: str = "INFO"
    OTEL_EXPORTER_ENDPOINT: str = "http://localhost:4317"

    model_config = {"env_prefix": "LLM_ROUTER_", "case_sensitive": True}


settings = Settings()
