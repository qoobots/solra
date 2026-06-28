"""Application configuration."""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    HOST: str = "0.0.0.0"
    PORT: int = 9094
    GRPC_PORT: int = 9094

    MODEL_PATH: str = "models/safety-classifier"
    MODEL_NAME: str = "solra-safety-v1"
    DEVICE: str = "cuda"  # cuda | cpu | mps (Apple Silicon)

    # Classification thresholds
    NSFW_THRESHOLD: float = 0.7
    HATE_SPEECH_THRESHOLD: float = 0.5
    VIOLENCE_THRESHOLD: float = 0.7

    LOG_LEVEL: str = "INFO"
    OTEL_EXPORTER_ENDPOINT: str = "http://localhost:4317"

    model_config = {"env_prefix": "SAFETY_MODEL_", "case_sensitive": True}


settings = Settings()
