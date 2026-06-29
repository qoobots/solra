"""Tests for TTS Service API routes."""

import pytest
import base64
import wave
import io
from fastapi.testclient import TestClient

from main import app
from api.dependencies import get_engine
from core.tts_engine import TTSEngine


@pytest.fixture
def engine():
    """Create a mock TTS engine for testing."""
    eng = TTSEngine(model_name="mock-tts", device="cpu", sample_rate=24000)
    eng.load_model()
    return eng


@pytest.fixture
def client(engine):
    """Create TestClient with mocked engine."""
    app.dependency_overrides[get_engine] = lambda: engine
    with TestClient(app) as c:
        yield c
    app.dependency_overrides.clear()


class TestHealthEndpoint:
    def test_health_returns_ok(self, client):
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "ok"
        assert data["service"] == "tts-service"
        assert data["model_loaded"] is True


class TestSynthesizeEndpoint:
    def test_synthesize_basic(self, client):
        response = client.post("/api/v1/synthesize", json={
            "text": "你好世界",
            "voice": "female_warm",
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data["audio_data"]) > 0
        assert data["format"] == "wav"
        assert data["sample_rate"] == 24000
        assert data["duration_sec"] > 0
        assert data["text_length"] == 4
        assert data["processing_time_ms"] >= 0
        assert data["model_name"] == "mock-tts"

        # Verify base64 decodes to valid WAV
        audio_bytes = base64.b64decode(data["audio_data"])
        with wave.open(io.BytesIO(audio_bytes), 'rb') as wf:
            assert wf.getnchannels() == 1

    def test_synthesize_different_voice(self, client):
        response = client.post("/api/v1/synthesize", json={
            "text": "测试",
            "voice": "male_deep",
            "speed": 1.2,
            "pitch": 0.9,
        })
        assert response.status_code == 200
        data = response.json()
        assert len(data["audio_data"]) > 0

    def test_synthesize_custom_sample_rate(self, client):
        response = client.post("/api/v1/synthesize", json={
            "text": "测试",
            "sample_rate": 16000,
        })
        assert response.status_code == 200
        assert response.json()["sample_rate"] == 16000

    def test_synthesize_empty_text(self, client):
        response = client.post("/api/v1/synthesize", json={
            "text": "",
            "voice": "female_warm",
        })
        assert response.status_code == 422

    def test_synthesize_invalid_speed(self, client):
        response = client.post("/api/v1/synthesize", json={
            "text": "测试",
            "speed": 3.0,
        })
        assert response.status_code == 422

    def test_synthesize_invalid_voice(self, client):
        response = client.post("/api/v1/synthesize", json={
            "text": "测试",
            "voice": "invalid_voice",
        })
        assert response.status_code == 422


class TestStreamEndpoint:
    def test_synthesize_stream(self, client):
        response = client.post("/api/v1/synthesize/stream", json={
            "text": "流式合成测试文本",
            "voice": "female_warm",
        })
        assert response.status_code == 200
        assert response.headers["content-type"].startswith("text/event-stream")

    def test_synthesize_stream_short_text(self, client):
        response = client.post("/api/v1/synthesize/stream", json={
            "text": "你好",
        })
        assert response.status_code == 200


class TestModelStatusEndpoint:
    def test_get_model_status(self, client):
        response = client.get("/api/v1/model/status")
        assert response.status_code == 200
        data = response.json()
        assert data["model_name"] == "mock-tts"
        assert data["is_loaded"] is True
        assert len(data["available_voices"]) == 6


class TestVoicesEndpoint:
    def test_list_voices(self, client):
        response = client.get("/api/v1/voices")
        assert response.status_code == 200
        data = response.json()
        assert len(data["voices"]) == 6
        voice_ids = {v["voice_id"] for v in data["voices"]}
        assert "female_warm" in voice_ids
        assert "male_deep" in voice_ids
