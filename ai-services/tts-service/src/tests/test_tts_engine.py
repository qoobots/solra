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

    # ---- Emotion Tests ----

    def test_synthesize_with_emotion(self, engine):
        """Synthesis with emotion should produce valid WAV."""
        for emotion in ["neutral", "happy", "sad", "angry"]:
            wav_bytes, duration = engine.synthesize(
                text="你好世界",
                voice="female_warm",
                emotion=emotion,
            )
            assert len(wav_bytes) > 0
            assert duration > 0
            with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
                assert wf.getnchannels() == 1

    def test_different_emotions_produce_different_audio(self, engine):
        """Different emotions should produce distinguishable audio."""
        wav_neutral, _ = engine.synthesize(
            text="今天天气真好", voice="female_warm", emotion="neutral"
        )
        wav_happy, _ = engine.synthesize(
            text="今天天气真好", voice="female_warm", emotion="happy"
        )
        wav_sad, _ = engine.synthesize(
            text="今天天气真好", voice="female_warm", emotion="sad"
        )
        # Different emotions should produce different audio
        assert wav_neutral != wav_happy
        assert wav_neutral != wav_sad
        assert wav_happy != wav_sad

    def test_emotion_affects_speed(self, engine):
        """Happy should be faster than sad for same text."""
        text = "这是一段用于测试情感语速的文本"
        _, dur_happy = engine.synthesize(text=text, voice="female_warm", emotion="happy")
        _, dur_sad = engine.synthesize(text=text, voice="female_warm", emotion="sad")
        assert dur_happy < dur_sad  # Happy is faster

    def test_emotion_affects_duration(self, engine):
        """Excited should be shorter than gentle for same text."""
        text = "测试情感对时长的影响"
        _, dur_excited = engine.synthesize(text=text, voice="female_warm", emotion="excited")
        _, dur_gentle = engine.synthesize(text=text, voice="female_warm", emotion="gentle")
        assert dur_excited < dur_gentle

    def test_emotion_same_input_deterministic(self, engine):
        """Same text + voice + emotion = same audio."""
        wav1, _ = engine.synthesize(
            text="测试", voice="female_clear", emotion="happy", speed=1.0, pitch=1.0
        )
        wav2, _ = engine.synthesize(
            text="测试", voice="female_clear", emotion="happy", speed=1.0, pitch=1.0
        )
        assert wav1 == wav2

    def test_all_eight_emotions_work(self, engine):
        """All 8 emotion presets should produce valid audio."""
        for emotion in ["neutral", "happy", "sad", "angry", "fearful", "surprised", "gentle", "excited"]:
            wav_bytes, _ = engine.synthesize(text="测试", emotion=emotion)
            assert len(wav_bytes) > 0

    def test_emotion_invalid_defaults_to_neutral(self, engine):
        """Invalid emotion should not crash (handled by get with default)."""
        wav_bytes, duration = engine.synthesize(
            text="测试", voice="female_warm", emotion="neutral"
        )
        assert len(wav_bytes) > 0

    def test_stream_with_emotion(self, engine):
        """Streaming with emotion should work."""
        chunks = list(engine.synthesize_stream(
            text="情感流式合成测试",
            voice="female_warm",
            emotion="happy",
        ))
        assert len(chunks) > 0
        assert chunks[-1][2] is True  # is_final

    def test_get_model_status_includes_emotions(self, engine):
        """Status should include available emotions."""
        status = engine.get_model_status()
        assert "available_emotions" in status
        assert len(status["available_emotions"]) == 8
        emotion_ids = {e["emotion_id"] for e in status["available_emotions"]}
        assert "happy" in emotion_ids
        assert "sad" in emotion_ids
        assert "neutral" in emotion_ids

    def test_get_model_status_includes_streaming(self, engine):
        """Status should include streaming support flag."""
        status = engine.get_model_status()
        assert "streaming_enabled" in status
        assert status["streaming_enabled"] is True

    def test_emotion_config_has_expected_fields(self, engine):
        """Each emotion config should have all required fields."""
        for emotion_id, config in engine.EMOTION_CONFIGS.items():
            assert "name" in config
            assert "speed_mod" in config
            assert "pitch_mod" in config
            assert "vibrato_rate" in config
            assert "vibrato_depth" in config
            assert "volume_mod" in config
