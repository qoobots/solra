"""配置管理。"""
import os


class Config:
    HOST = os.getenv("TTS_HOST", "0.0.0.0")
    PORT = int(os.getenv("TTS_PORT", "8103"))
    MODEL_NAME = os.getenv("TTS_MODEL", "mock-tts")
    DEVICE = os.getenv("TTS_DEVICE", "cpu")
    SAMPLE_RATE = int(os.getenv("TTS_SAMPLE_RATE", "24000"))
    STREAMING_ENABLED = os.getenv("TTS_STREAMING", "true").lower() == "true"
    DEFAULT_VOICE = os.getenv("TTS_DEFAULT_VOICE", "female_warm")


config = Config()
