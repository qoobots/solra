"""Audio safety classifier — speech content safety via ASR+NLP.

Real mode : Whisper → TextClassifier pipeline
Mock mode: duration-based pass-through for offline CI/testing.
"""

import time
from typing import List, Dict, Any, Optional

import structlog

from .base_classifier import BaseClassifier, CategoryResult

logger = structlog.get_logger(__name__)

AUDIO_CATEGORIES = [
    "hate_speech",
    "harassment",
    "violence",
    "nsfw",
]

DEFAULT_AUDIO_THRESHOLDS: Dict[str, float] = {
    "hate_speech": 0.5,
    "harassment": 0.6,
    "violence": 0.7,
    "nsfw": 0.7,
}


class AudioClassifier(BaseClassifier):
    """Audio safety classifier — ASR + text safety pipeline."""

    def __init__(
        self,
        model_path: str = "models/safety-classifier/audio",
        device: str = "cpu",
        use_real_model: bool = False,
        thresholds: Optional[Dict[str, float]] = None,
    ):
        self._model_path = model_path
        self._device = device
        self._use_real = use_real_model
        self._thresholds = thresholds or DEFAULT_AUDIO_THRESHOLDS
        self._whisper_model = None
        self._whisper_processor = None
        self._text_classifier = None  # injected at pipeline level
        self._loaded = False

    async def load(self) -> None:
        if self._use_real:
            try:
                from transformers import WhisperProcessor, WhisperForConditionalGeneration
                self._whisper_processor = WhisperProcessor.from_pretrained(self._model_path)
                self._whisper_model = WhisperForConditionalGeneration.from_pretrained(
                    self._model_path
                ).to(self._device)
                self._whisper_model.eval()
                logger.info("audio_classifier.real_loaded",
                            model=self._model_path, device=self._device)
            except Exception as e:
                logger.warning("audio_classifier.real_load_failed", error=str(e))
                self._use_real = False

        self._loaded = True
        logger.info("audio_classifier.ready", mode="real" if self._use_real else "mock")

    def set_text_classifier(self, text_cls: BaseClassifier) -> None:
        """Link text classifier for ASR → NLP pipeline."""
        self._text_classifier = text_cls

    def supported_categories(self) -> List[str]:
        return AUDIO_CATEGORIES

    def model_version(self) -> str:
        return "solra-audio-v1" + ("-real" if self._use_real else "-mock")

    async def predict(self, content: Any, **kwargs) -> List[CategoryResult]:
        """content: audio file path, bytes, or numpy waveform."""
        if self._use_real:
            return await self._predict_real(content)
        return self._predict_mock(content)

    async def _predict_real(self, audio) -> List[CategoryResult]:
        """Whisper transcribe → text classifier."""
        import torch
        t0 = time.monotonic()

        if isinstance(audio, str):
            import librosa
            waveform, sr = librosa.load(audio, sr=16000)
        else:
            waveform = audio
            sr = 16000

        inputs = self._whisper_processor(
            waveform, sampling_rate=sr, return_tensors="pt"
        ).to(self._device)
        with torch.no_grad():
            generated_ids = self._whisper_model.generate(inputs.input_features)
        transcription = self._whisper_processor.batch_decode(
            generated_ids, skip_special_tokens=True
        )[0]

        if self._text_classifier:
            results = await self._text_classifier.predict(transcription)
        else:
            results = []

        elapsed = (time.monotonic() - t0) * 1000
        logger.debug("audio_classifier.real_predict",
                     duration_sec=len(waveform)/sr if hasattr(waveform, '__len__') else 0,
                     transcription_len=len(transcription),
                     elapsed_ms=round(elapsed, 1))

        # Filter to audio-relevant categories
        audio_cats = set(AUDIO_CATEGORIES)
        return [r for r in results if r.category in audio_cats]

    def _predict_mock(self, _audio) -> List[CategoryResult]:
        """Mock always-pass for CI."""
        t0 = time.monotonic()
        results = [
            CategoryResult(category=cat, score=1.0, above_threshold=True,
                           label=cat.replace("_", " ").title())
            for cat in AUDIO_CATEGORIES
        ]
        elapsed = (time.monotonic() - t0) * 1000
        logger.debug("audio_classifier.mock_predict", elapsed_ms=round(elapsed, 2))
        return results
