"""TTS Engine: text-to-speech synthesis with multiple voice presets and emotion control.

Supports mock synthesis for development and integration with real TTS models
like CosyVoice 2 for production. The engine supports dual-mode operation:
  - Mock mode: sine-wave synthesis with emotion modulation (no GPU required)
  - Real mode: CosyVoice 2 model via HuggingFace (requires GPU for real-time)

Toggle via environment: TTS_USE_REAL_MODELS=true
"""

import io
import os
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
    Text-to-Speech synthesis engine with emotion control and dual-mode operation.

    Mock mode: Deterministic sine-wave synthesis with emotion modulation.
    Real mode: CosyVoice 2 model via HuggingFace Transformers.

    Environment variables:
        TTS_USE_REAL_MODELS: Set to "true" to load real CosyVoice 2 model.
        TTS_MODEL_PATH: Path to local model files (default: models/tts/cosyvoice2-0.5b).
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

    # Emotion presets: maps emotion labels to (speed_mod, pitch_mod, vibrato_rate, vibrato_depth, volume_mod)
    EMOTION_CONFIGS = {
        "neutral":   {"name": "中性",  "speed_mod": 1.0,  "pitch_mod": 1.0,  "vibrato_rate": 5.0,  "vibrato_depth": 0.01, "volume_mod": 1.0},
        "happy":     {"name": "开心",  "speed_mod": 1.15, "pitch_mod": 1.08, "vibrato_rate": 6.5,  "vibrato_depth": 0.02, "volume_mod": 1.1},
        "sad":       {"name": "悲伤",  "speed_mod": 0.75, "pitch_mod": 0.92, "vibrato_rate": 3.5,  "vibrato_depth": 0.03, "volume_mod": 0.8},
        "angry":     {"name": "愤怒",  "speed_mod": 1.25, "pitch_mod": 1.12, "vibrato_rate": 8.0,  "vibrato_depth": 0.015, "volume_mod": 1.2},
        "fearful":   {"name": "恐惧",  "speed_mod": 1.3,  "pitch_mod": 1.15, "vibrato_rate": 9.0,  "vibrato_depth": 0.025, "volume_mod": 0.7},
        "surprised": {"name": "惊讶",  "speed_mod": 1.2,  "pitch_mod": 1.18, "vibrato_rate": 4.0,  "vibrato_depth": 0.01, "volume_mod": 1.15},
        "gentle":    {"name": "温柔",  "speed_mod": 0.85, "pitch_mod": 0.95, "vibrato_rate": 4.5,  "vibrato_depth": 0.015, "volume_mod": 0.9},
        "excited":   {"name": "激动",  "speed_mod": 1.3,  "pitch_mod": 1.15, "vibrato_rate": 7.0,  "vibrato_depth": 0.02, "volume_mod": 1.25},
    }

    # Supported streaming formats
    STREAMING_CHUNK_SIZES = {
        "tiny": 0.25,    # 250ms chunks
        "small": 0.5,    # 500ms (default)
        "normal": 1.0,   # 1s
        "large": 2.0,    # 2s
    }

    # Real model mapping for CosyVoice 2
    REAL_MODEL_MAPPING = {
        "cosyvoice2-0.5b": "FunAudioLLM/CosyVoice2-0.5B",
    }

    def __init__(
        self,
        model_name: str = "mock-tts",
        device: str = "cpu",
        sample_rate: int = 24000,
        model_path: Optional[str] = None,
        use_real_models: bool = False,
    ):
        self.model_name = model_name
        self.device = device
        self.sample_rate = sample_rate
        self._is_loaded = False
        self._real_model = None
        self._real_processor = None
        self._streaming_enabled = True
        self._use_real_models = use_real_models
        self._model_path = model_path or os.environ.get(
            "TTS_MODEL_PATH",
            os.path.join(os.path.dirname(__file__), "..", "..", "models", "tts", "cosyvoice2-0.5b"),
        )

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded

    @property
    def use_real_models(self) -> bool:
        return self._use_real_models and self._real_model is not None

    def load_model(self) -> None:
        """Load the TTS model into memory. Attempts real model first if enabled, falls back to mock."""
        if self._use_real_models:
            try:
                self._load_real_model()
                return
            except Exception as e:
                logger.warning(
                    f"Failed to load real TTS model: {e}. Falling back to mock synthesis."
                )
                self._use_real_models = False

        # Mock mode
        self._is_loaded = True
        logger.info(f"TTS engine ready (mock mode): {self.model_name}, sr={self.sample_rate}")

    def _load_real_model(self) -> None:
        """Load CosyVoice 2 real model from local path or HuggingFace Hub."""
        from pathlib import Path

        model_path = Path(self._model_path)

        # Determine model source: local path or HuggingFace Hub
        if model_path.exists() and (model_path / "manifest.json").exists():
            model_id = str(model_path)
            logger.info(f"Loading CosyVoice 2 from local path: {model_id}")
        else:
            # Fall back to HuggingFace Hub
            model_id = self.REAL_MODEL_MAPPING.get(
                self.model_name, "FunAudioLLM/CosyVoice2-0.5B"
            )
            logger.info(f"Loading CosyVoice 2 from HuggingFace Hub: {model_id}")

        try:
            from transformers import AutoModel, AutoProcessor

            self._real_processor = AutoProcessor.from_pretrained(model_id, trust_remote_code=True)
            self._real_model = AutoModel.from_pretrained(
                model_id,
                trust_remote_code=True,
                torch_dtype="auto",
                device_map=self.device if self.device != "cpu" else None,
            )
            if self.device != "cpu":
                self._real_model = self._real_model.to(self.device)
            self._real_model.eval()

            self._is_loaded = True
            self._model_version = f"solra-tts-v1-real-{self.model_name}"
            logger.info(
                f"TTS engine ready (real mode): {self.model_name}, "
                f"device={self.device}, sr={self.sample_rate}"
            )

        except ImportError as e:
            raise RuntimeError(
                f"Real TTS mode requires 'transformers' and 'torch' packages. "
                f"Install with: pip install transformers torch. Error: {e}"
            )
        except Exception as e:
            raise RuntimeError(f"Failed to load CosyVoice 2 model: {e}")

    def _synthesize_real(
        self,
        text: str,
        voice: str = "female_warm",
        speed: float = 1.0,
        pitch: float = 1.0,
        emotion: str = "neutral",
    ) -> bytes:
        """Synthesize using real CosyVoice 2 model."""
        if self._real_model is None or self._real_processor is None:
            raise RuntimeError("Real TTS model not loaded")

        import torch

        voice_desc = self.VOICE_METADATA.get(voice, self.VOICE_METADATA["female_warm"])
        emotion_desc = self.EMOTION_CONFIGS.get(emotion, self.EMOTION_CONFIGS["neutral"])

        # Build prompt for CosyVoice 2
        prompt_text = (
            f"[{voice_desc['name']}][{emotion_desc['name']}][speed={speed:.1f}][pitch={pitch:.1f}]"
        )
        full_text = f"{prompt_text} {text}"

        with torch.no_grad():
            inputs = self._real_processor(text=full_text, return_tensors="pt")
            if self.device != "cpu":
                inputs = {k: v.to(self.device) for k, v in inputs.items()}

            speech_output = self._real_model.generate(**inputs)

        # Convert tensor output to WAV bytes
        if isinstance(speech_output, torch.Tensor):
            audio_np = speech_output.cpu().numpy().squeeze()
        else:
            audio_np = np.array(speech_output).squeeze()

        # Ensure float32 [-1, 1] range
        if audio_np.dtype != np.float32:
            audio_np = audio_np.astype(np.float32)
        max_val = np.max(np.abs(audio_np))
        if max_val > 0:
            audio_np = audio_np / max_val * 0.9

        return self._numpy_to_wav(audio_np)

    def _apply_emotion_to_audio(
        self,
        audio: np.ndarray,
        emotion: str,
        sample_rate: int,
    ) -> np.ndarray:
        """
        Apply emotional characteristics to generated audio.

        Modifies speed, pitch contour, vibrato, and volume based on emotion config.
        """
        if emotion not in self.EMOTION_CONFIGS:
            return audio

        emo = self.EMOTION_CONFIGS[emotion]
        num_samples = len(audio)
        t = np.arange(num_samples) / sample_rate

        # Apply vibrato (frequency modulation)
        if emo["vibrato_depth"] > 0:
            vibrato = 1.0 + emo["vibrato_depth"] * np.sin(
                2 * np.pi * emo["vibrato_rate"] * t
            )
            # Resample with vibrato by stretching/compressing time
            cumsum = np.cumsum(vibrato)
            cumsum = cumsum / cumsum[-1] * (num_samples - 1)
            audio = np.interp(cumsum, np.arange(num_samples), audio)

        # Apply volume modulation envelope
        # Sad/fearful: softer; happy/excited: louder
        audio = audio * emo["volume_mod"]

        # Re-normalize
        max_val = np.max(np.abs(audio))
        if max_val > 0:
            audio = audio / max_val * 0.9

        return audio.astype(np.float32)

    def _generate_mock_audio(
        self,
        text: str,
        voice: str,
        speed: float,
        pitch: float,
        emotion: str = "neutral",
    ) -> bytes:
        """
        Generate mock audio using simple sine wave synthesis with emotion.

        Creates a WAV file with sine wave tones modulated by text characteristics
        and emotional style.
        """
        config = self.VOICE_CONFIGS.get(voice, self.VOICE_CONFIGS["female_warm"])
        base_freq, formant_shift, timbre_seed = config

        # Apply emotion speed/pitch modifiers
        emo_config = self.EMOTION_CONFIGS.get(emotion, self.EMOTION_CONFIGS["neutral"])
        effective_speed = speed * emo_config["speed_mod"]
        effective_pitch = pitch * emo_config["pitch_mod"]

        # Calculate duration based on text length and speed
        chars_per_second = 4.0 * effective_speed
        duration = max(len(text) / chars_per_second, 0.5)

        num_samples = int(self.sample_rate * duration)
        rng = np.random.RandomState(hash(text + emotion) % 2**31)

        # Generate audio samples
        t = np.arange(num_samples) / self.sample_rate

        # Use text hash to create variation between different texts
        text_seed = hash(text + emotion) % 1000 / 1000.0

        # Multi-harmonic synthesis for more natural sound
        audio = np.zeros(num_samples, dtype=np.float32)
        harmonics = [1.0, 2.0, 3.0, 0.5, 1.5]
        amplitudes = [0.6, 0.2, 0.1, 0.05, 0.05]

        for h, amp in zip(harmonics, amplitudes):
            freq = base_freq * h * formant_shift * effective_pitch
            # Add slight frequency modulation for naturalness, with text-dependent variation
            mod = 1.0 + (0.02 + text_seed * 0.01) * np.sin(2 * np.pi * (5.0 + text_seed * 3.0) * t + timbre_seed + text_seed * np.pi)
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

        # Apply emotion effects (vibrato, volume modulation)
        if emotion != "neutral":
            audio = self._apply_emotion_to_audio(audio, emotion, self.sample_rate)

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
        emotion: str = "neutral",
        sample_rate: int = 24000,
    ) -> Tuple[bytes, float]:
        """
        Synthesize text to speech audio with emotion control.

        Args:
            text: Input text to synthesize.
            voice: Voice preset name.
            speed: Speech speed multiplier.
            pitch: Pitch adjustment multiplier.
            emotion: Emotion style (neutral/happy/sad/angry/fearful/surprised/gentle/excited).
            sample_rate: Output sample rate.

        Returns:
            Tuple of (wav_bytes, duration_seconds).
        """
        start = time.perf_counter()

        if self.use_real_models:
            wav_bytes = self._synthesize_real(text, voice, speed, pitch, emotion)
        else:
            original_sr = self.sample_rate
            self.sample_rate = sample_rate
            try:
                wav_bytes = self._generate_mock_audio(text, voice, speed, pitch, emotion)
            finally:
                self.sample_rate = original_sr

        # Calculate duration from WAV
        with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
            duration = wf.getnframes() / wf.getframerate()

        elapsed = (time.perf_counter() - start) * 1000
        mode = "real" if self.use_real_models else "mock"
        logger.debug(
            f"TTS synthesized ({mode}) in {elapsed:.1f}ms, duration={duration:.1f}s, "
            f"voice={voice}, emotion={emotion}"
        )

        return wav_bytes, duration

    def synthesize_stream(
        self,
        text: str,
        voice: str = "female_warm",
        speed: float = 1.0,
        pitch: float = 1.0,
        emotion: str = "neutral",
        sample_rate: int = 24000,
        chunk_duration: float = 0.5,
    ) -> Generator[Tuple[int, bytes, bool], None, None]:
        """
        Stream TTS audio in chunks via SSE with emotion support.

        Args:
            text: Input text.
            voice: Voice preset.
            speed: Speech speed.
            pitch: Pitch adjustment.
            emotion: Emotion style.
            sample_rate: Output sample rate.
            chunk_duration: Duration per chunk in seconds.

        Yields:
            Tuple of (chunk_index, audio_chunk_bytes, is_final).
        """
        wav_bytes, total_duration = self.synthesize(
            text=text, voice=voice, speed=speed, pitch=pitch,
            emotion=emotion, sample_rate=sample_rate,
        )

        # Split into chunks
        with wave.open(io.BytesIO(wav_bytes), 'rb') as wf:
            frame_rate = wf.getframerate()
            frames_per_chunk = int(frame_rate * chunk_duration)
            chunk_index = 0
            total_frames = wf.getnframes()
            frames_read = 0

            while frames_read < total_frames:
                frames = wf.readframes(frames_per_chunk)
                if not frames:
                    break

                frames_read += len(frames) // (wf.getsampwidth() * wf.getnchannels())

                # Wrap chunk as standalone WAV
                chunk_buf = io.BytesIO()
                with wave.open(chunk_buf, 'wb') as chunk_wf:
                    chunk_wf.setnchannels(wf.getnchannels())
                    chunk_wf.setsampwidth(wf.getsampwidth())
                    chunk_wf.setframerate(frame_rate)
                    chunk_wf.writeframes(frames)

                is_final = frames_read >= total_frames
                yield chunk_index, chunk_buf.getvalue(), is_final
                chunk_index += 1

    def get_model_status(self) -> dict:
        """Get current model, voice, and emotion information."""
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
        emotions = [
            {"emotion_id": eid, "name": emo["name"]}
            for eid, emo in self.EMOTION_CONFIGS.items()
        ]
        return {
            "model_name": self.model_name,
            "model_version": getattr(self, "_model_version", "solra-tts-v1-mock"),
            "is_loaded": self._is_loaded,
            "use_real_models": self.use_real_models,
            "device": self.device,
            "streaming_enabled": self._streaming_enabled,
            "supported_formats": ["wav", "mp3", "pcm", "ogg"],
            "supported_sample_rates": [8000, 16000, 22050, 24000, 44100, 48000],
            "available_voices": voices,
            "available_emotions": emotions,
        }

    def unload_model(self) -> None:
        """Unload the TTS model and free GPU memory."""
        if self._real_model is not None:
            import torch
            self._real_model.cpu()
            del self._real_model
            torch.cuda.empty_cache() if torch.cuda.is_available() else None
        self._real_model = None
        self._real_processor = None
        self._is_loaded = False
        logger.info(f"TTS model {self.model_name} unloaded")
