"""Embedding Service configuration management."""
import os


class Config:
    HOST = os.getenv("EMBEDDING_HOST", "0.0.0.0")
    PORT = int(os.getenv("EMBEDDING_PORT", "8101"))
    MODEL_NAME = os.getenv("EMBEDDING_MODEL", "text2vec-large-chinese")
    BATCH_SIZE = int(os.getenv("EMBEDDING_BATCH_SIZE", "32"))
    MAX_LENGTH = int(os.getenv("EMBEDDING_MAX_LENGTH", "512"))
    DEVICE = os.getenv("EMBEDDING_DEVICE", "cpu")
    NORMALIZE = os.getenv("EMBEDDING_NORMALIZE", "true").lower() == "true"

    # Real model path (local directory or HF Hub model ID)
    # Set to a local path to load models from disk instead of downloading from HF Hub
    MODEL_PATH = os.getenv("EMBEDDING_MODEL_PATH", "")  # e.g., "models/embedding/text2vec-large-chinese"


config = Config()
