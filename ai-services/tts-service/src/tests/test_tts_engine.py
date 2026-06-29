"""Tests for TTS Engine core logic."""

import pytest
import wave
import io
from core.tts_engine import TTSEngine


class TestTTSEngine:
    """Unit tests for TTSEngine (mock mode)."""

    @pytest.fixture
    def engine(self):
        """Create a TTS engine in mock mode."""
        eng = TTSEngine(model_name="mock-tts", device="cpu", sample_rate=24000)
        eng.load_model()
        return eng

    def test_load_model(self, engine):
        assert engine.is_loaded is True

    def test_synthesize_returns_wav_bytes(self, engine):
        wav_bytes, duration = engine.synthesize(
            text="你好世界",
            voice="female_warm",
        )
        assert len(wav_bytes) > 0
        assert duration > 0

        # Verify it's a valid WAV file
        with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
            assert wf.getnchannels() == 1
            assert wf.getsampwidth() == 2  # 16-bit
            assert wf.getframerate() == 24000

    def test_synthesize_different_voices(self, engine):
        """Different voices should produce different audio."""
        wav1, _ = engine.synthesize(text="测试", voice="female_warm")
        wav2, _ = engine.synthesize(text="测试", voice="male_deep")
        assert wav1 != wav2

    def test_synthesize_different_text(self, engine):
        """Different text should produce different audio."""
        wav1, _ = engine.synthesize(text="你好", voice="female_warm")
        wav2, _ = engine.synthesize(text="世界", voice="female_warm")
        assert wav1 != wav2

    def test_synthesize_same_input_deterministic(self, engine):
        """Same input should produce same output."""
        wav1, _ = engine.synthesize(text="测试文本", voice="female_clear", speed=1.0, pitch=1.0)
        wav2, _ = engine.synthesize(text="测试文本", voice="female_clear", speed=1.0, pitch=1.0)
        assert wav1 == wav2

    def test_synthesize_duration_scales_with_text_length(self, engine):
        _, dur1 = engine.synthesize(text="短", voice="female_warm")
        _, dur2 = engine.synthesize(text="这是一段比较长的文本用于测试", voice="female_warm")
        assert dur2 >= dur1

    def test_synthesize_speed_affects_duration(self, engine):
        text = "测试文本内容"
        _, dur_slow = engine.synthesize(text=text, voice="female_warm", speed=0.5)
        _, dur_fast = engine.synthesize(text=text, voice="female_warm", speed=2.0)
        assert dur_slow > dur_fast

    def test_synthesize_custom_sample_rate(self, engine):
        wav_bytes, _ = engine.synthesize(
            text="测试", voice="female_warm", sample_rate=16000,
        )
        with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
            assert wf.getframerate() == 16000

    def test_synthesize_stream(self, engine):
        chunks = list(engine.synthesize_stream(
            text="这是一段测试文本用于流式合成",
            voice="female_warm",
        ))
        assert len(chunks) > 0

        for chunk_idx, audio_bytes, is_final in chunks:
            assert isinstance(chunk_idx, int)
            assert len(audio_bytes) > 0
            assert isinstance(is_final, bool)

        # Last chunk should be final
        assert chunks[-1][2] is True

    def test_synthesize_stream_single_chunk(self, engine):
        """Short text should produce at least one chunk."""
        chunks = list(engine.synthesize_stream(
            text="你好", voice="female_warm", chunk_duration=10.0,
        ))
        assert len(chunks) >= 1

    def test_get_model_status(self, engine):
        status = engine.get_model_status()
        assert status["model_name"] == "mock-tts"
        assert status["is_loaded"] is True
        assert status["device"] == "cpu"
        assert "wav" in status["supported_formats"]
        assert 24000 in status["supported_sample_rates"]
        assert len(status["available_voices"]) == 6

    def test_voice_metadata(self, engine):
        status = engine.get_model_status()
        voices = status["available_voices"]
        voice_ids = {v["voice_id"] for v in voices}
        expected = {"female_warm", "female_clear", "male_deep", "male_bright", "child", "elder"}
        assert voice_ids == expected

    def test_unload_model(self, engine):
        engine.unload_model()
        assert engine.is_loaded is False
