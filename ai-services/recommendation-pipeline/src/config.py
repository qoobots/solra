"""配置管理。"""
import os


class Config:
    HOST = os.getenv("REC_HOST", "0.0.0.0")
    PORT = int(os.getenv("REC_PORT", "8102"))
    MODEL_DIR = os.getenv("REC_MODEL_DIR", "/models/recommendation")
    CANDIDATE_POOL_SIZE = int(os.getenv("REC_CANDIDATE_POOL", "500"))
    REFRESH_INTERVAL_MINUTES = int(os.getenv("REC_REFRESH_INTERVAL", "30"))
    N_FACTORS = int(os.getenv("REC_N_FACTORS", "50"))


config = Config()
