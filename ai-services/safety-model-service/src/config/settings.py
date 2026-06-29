"""Extended settings for safety model service."""

from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # server
    HOST: str = "0.0.0.0"
    PORT: int = 9094
    GRPC_PORT: int = 9095

    # model paths
    MODEL_PATH: str = "models/safety-classifier"
    TEXT_MODEL_PATH: str = "models/safety-classifier/text"
    IMAGE_MODEL_PATH: str = "models/safety-classifier/image"
    AUDIO_MODEL_PATH: str = "models/safety-classifier/audio"

    # runtime
    DEVICE: str = "cpu"           # cpu | cuda | mps
    USE_REAL_MODELS: bool = False # set True when weights available

    # classifier thresholds (0.0 = most strict, 1.0 = most lenient)
    NSFW_THRESHOLD: float = 0.7
    HATE_SPEECH_THRESHOLD: float = 0.5
    HARASSMENT_THRESHOLD: float = 0.6
    VIOLENCE_THRESHOLD: float = 0.7
    SELF_HARM_THRESHOLD: float = 0.3
    SPAM_THRESHOLD: float = 0.6
    FRAUD_THRESHOLD: float = 0.6
    MINOR_SAFETY_THRESHOLD: float = 0.3
    PERSONAL_INFO_THRESHOLD: float = 0.5

    # observability
    LOG_LEVEL: str = "INFO"
    OTEL_EXPORTER_ENDPOINT: str = "http://localhost:4317"
    METRICS_ENABLED: bool = True

    # rate limiting
    MAX_REQUESTS_PER_SECOND: int = 100
    MAX_IMAGE_SIZE_MB: int = 20
    MAX_AUDIO_DURATION_SEC: int = 300

    model_config = {
        "env_prefix": "SAFETY_MODEL_",
        "case_sensitive": True,
    }


settings = Settings()
