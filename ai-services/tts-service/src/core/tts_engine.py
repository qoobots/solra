"""TTS Engine: text-to-speech synthesis with multiple voice presets.

Supports mock synthesis for development and integration with real TTS models
like XTTS-v2, CosyVoice 2, or ChatTTS for production.
"""

import io
import wave
import time
import struct
import logging
import base64
import math
from typing import List, Dict, Optional, Generator, Tuple

import numpy as np

logger = logging.getLogger(__name__)


class TTSEngine:
    """
    Text-to-Speech synthesis engine.

    Provides mock synthesis with configurable voice presets.
    Ready for integration with real models (XTTS-v2 / CosyVoice 2).
    """

    # Voice presets: (base_freq, formant_shift, timbre_seed)
    VOICE_CONFIGS = {
        "female_warm": (220.0, 1.0, 42),
        "female_clear": (260.0, 1.1, 43),
        "male_deep": (110.0, 0.7, 44),
        "male_bright": (150.0, 0.9, 45),
        "child": (280.0, 1.3, 46),
        "elder": (140.0, 0.8, 47),
    }

    VOICE_METADATA = {
        "female_warm": {"name": "温暖女声", "gender": "female", "description": "温柔知性女声", "sample_text": "你好，我是你的AI助手"},
        "female_clear": {"name": "清澈女声", "gender": "female", "description": "清晰明亮女声", "sample_text": "今天天气真不错"},
        "male_deep":   {"name": "深沉男声", "gender": "male", "description": "沉稳磁性男声", "sample_text": "欢迎来到索拉空间"},
        "male_bright": {"name": "明亮男声", "gender": "male", "description": "阳光活力男声", "sample_text": "让我们一起探索吧"},
        "child":       {"name": "童声", "gender": "neutral", "description": "天真可爱童声", "sample_text": "你好呀，很高兴认识你"},
        "elder":       {"name": "长者之声", "gender": "neutral", "description": "慈祥和蔼长者声", "sample_text": "慢慢来，不着急"},
    }

    def __init__(
        self,
        model_name: str = "mock-tts",
        device: str = "cpu",
        sample_rate: int = 24000,
    ):
        self.model_name = model_name
        self.device = device
        self.sample_rate = sample_rate
        self._is_loaded = False
        self._real_model = None

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded

    def load_model(self) -> None:
        """Load the TTS model into memory."""
        try:
            # Attempt to load real model if available
            # For now, use mock mode
            self._is_loaded = True
            logger.info(f"TTS engine ready (mock mode): {self.model_name}, sr={self.sample_rate}")
        except Exception as e:
            logger.warning(f"Failed to load TTS model: {e}. Using mock synthesis.")
            self._is_loaded = True

    def _generate_mock_audio(
        self,
        text: str,
        voice: str,
        speed: float,
        pitch: float,
    ) -> bytes:
        """
        Generate mock audio using simple sine wave synthesis.

        Creates a WAV file with sine wave tones modulated by text characteristics.
        """
        config = self.VOICE_CONFIGS.get(voice, self.VOICE_CONFIGS["female_warm"])
        base_freq, formant_shift, timbre_seed = config

        # Calculate duration based on text length and speed
        # Approximate: ~4 chars per second for Chinese, adjusted by speed
        chars_per_second = 4.0 * speed
        duration = max(len(text) / chars_per_second, 0.5)

        num_samples = int(self.sample_rate * duration)
        rng = np.random.RandomState(hash(text) % 2**31)

        # Generate audio samples
        t = np.arange(num_samples) / self.sample_rate

        # Multi-harmonic synthesis for more natural sound
        audio = np.zeros(num_samples, dtype=np.float32)
        harmonics = [1.0, 2.0, 3.0, 0.5, 1.5]
        amplitudes = [0.6, 0.2, 0.1, 0.05, 0.05]

        for h, amp in zip(harmonics, amplitudes):
            freq = base_freq * h * formant_shift * pitch
            # Add slight frequency modulation for naturalness
            mod = 1.0 + 0.02 * np.sin(2 * np.pi * 5.0 * t + timbre_seed)
            audio += amp * np.sin(2 * np.pi * freq * t * mod)

        # Apply envelope (fade in/out)
        fade_len = min(int(0.01 * self.sample_rate), num_samples // 4)
        if fade_len > 0:
            fade_in = np.linspace(0, 1, fade_len)
            fade_out = np.linspace(1, 0, fade_len)
            audio[:fade_len] *= fade_in
            audio[-fade_len:] *= fade_out

        # Normalize
        max_val = np.max(np.abs(audio))
        if max_val > 0:
            audio = audio / max_val * 0.9

        # Convert to WAV bytes
        return self._numpy_to_wav(audio)

    def _numpy_to_wav(self, audio: np.ndarray) -> bytes:
        """Convert numpy audio array to WAV file bytes."""
        buf = io.BytesIO()
        with wave.open(buf, 'wb') as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)  # 16-bit
            wf.setframerate(self.sample_rate)

            # Convert float32 [-1, 1] to int16
            int_audio = (audio * 32767).astype(np.int16)
            wf.writeframes(int_audio.tobytes())

        return buf.getvalue()

    def synthesize(
        self,
        text: str,
        voice: str = "female_warm",
        speed: float = 1.0,
        pitch: float = 1.0,
        sample_rate: int = 24000,
    ) -> Tuple[bytes, float]:
        """
        Synthesize text to speech audio.

        Args:
            text: Input text to synthesize.
            voice: Voice preset name.
            speed: Speech speed multiplier.
            pitch: Pitch adjustment multiplier.
            sample_rate: Output sample rate.

        Returns:
            Tuple of (wav_bytes, duration_seconds).
        """
        start = time.perf_counter()

        original_sr = self.sample_rate
        self.sample_rate = sample_rate
        try:
            wav_bytes = self._generate_mock_audio(text, voice, speed, pitch)
            # Calculate duration from WAV
            with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
                duration = wf.getnframes() / wf.getframerate()
        finally:
            self.sample_rate = original_sr

        elapsed = (time.perf_counter() - start) * 1000
        logger.debug(f"TTS synthesized in {elapsed:.1f}ms, duration={duration:.1f}s")

        return wav_bytes, duration

    def synthesize_stream(
        self,
        text: str,
        voice: str = "female_warm",
        speed: float = 1.0,
        pitch: float = 1.0,
        sample_rate: int = 24000,
        chunk_duration: float = 0.5,
    ) -> Generator[Tuple[int, bytes, bool], None, None]:
        """
        Stream TTS audio in chunks via SSE.

        Args:
            text: Input text.
            voice: Voice preset.
            speed: Speech speed.
            pitch: Pitch adjustment.
            sample_rate: Output sample rate.
            chunk_duration: Duration per chunk in seconds.

        Yields:
            Tuple of (chunk_index, audio_chunk_bytes, is_final).
        """
        wav_bytes, total_duration = self.synthesize(
            text=text, voice=voice, speed=speed, pitch=pitch, sample_rate=sample_rate,
        )

        # Split into chunks
        with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
            frame_rate = wf.getframerate()
            frames_per_chunk = int(frame_rate * chunk_duration)
            chunk_index = 0

            while True:
                frames = wf.readframes(frames_per_chunk)
                if not frames:
                    break

                # Wrap chunk as standalone WAV
                chunk_buf = io.BytesIO()
                with wave.open(chunk_buf, 'wb') as chunk_wf:
                    chunk_wf.setnchannels(1)
                    chunk_wf.setsampwidth(2)
                    chunk_wf.setframerate(frame_rate)
                    chunk_wf.writeframes(frames)

                is_final = wf.tell() >= wf.getnframes() * 2  # 16-bit = 2 bytes per sample
                yield chunk_index, chunk_buf.getvalue(), is_final
                chunk_index += 1

    def get_model_status(self) -> dict:
        """Get current model and voice information."""
        voices = [
            {
                "name": meta["name"],
                "voice_id": vid,
                "gender": meta["gender"],
                "description": meta["description"],
                "sample_text": meta["sample_text"],
            }
            for vid, meta in self.VOICE_METADATA.items()
        ]
        return {
            "model_name": self.model_name,
            "is_loaded": self._is_loaded,
            "device": self.device,
            "supported_formats": ["wav", "mp3", "pcm", "ogg"],
            "supported_sample_rates": [8000, 16000, 22050, 24000, 44100, 48000],
            "available_voices": voices,
        }

    def unload_model(self) -> None:
        """Unload the TTS model."""
        self._real_model = None
        self._is_loaded = False
        logger.info(f"TTS model {self.model_name} unloaded")
