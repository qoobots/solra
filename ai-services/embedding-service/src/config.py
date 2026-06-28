"""配置管理。"""
import os


class Config:
    HOST = os.getenv("EMBEDDING_HOST", "0.0.0.0")
    PORT = int(os.getenv("EMBEDDING_PORT", "8101"))
    MODEL_NAME = os.getenv("EMBEDDING_MODEL", "text2vec-large-chinese")
    BATCH_SIZE = int(os.getenv("EMBEDDING_BATCH_SIZE", "32"))
    MAX_LENGTH = int(os.getenv("EMBEDDING_MAX_LENGTH", "512"))


config = Config()
