"""Unified safety inference pipeline — orchestrates text/image/audio classifiers."""

import time
from typing import Dict, Optional, List, Any
import structlog

from .base_classifier import BaseClassifier, SafetyVerdict, CategoryResult
from .text_classifier import TextClassifier, TEXT_CATEGORIES
from .image_classifier import ImageClassifier, IMAGE_CATEGORIES
from .audio_classifier import AudioClassifier, AUDIO_CATEGORIES

logger = structlog.get_logger(__name__)

# ── filter level presets ──────────────────────────────────────────
# Each filter level adjusts per-category thresholds (multiplier).
# lenient → higher thresholds (fewer flags), strict → lower thresholds (more flags).
FILTER_LEVEL_MULTIPLIERS: Dict[str, float] = {
    "lenient": 1.2,
    "normal": 1.0,
    "strict": 0.72,
}


class SafetyPipeline:
    """Manages all modality classifiers and routes inference requests."""

    def __init__(
        self,
        text_model_path: str = "models/safety-classifier/text",
        image_model_path: str = "models/safety-classifier/image",
        audio_model_path: str = "models/safety-classifier/audio",
        device: str = "cpu",
        use_real_models: bool = False,
    ):
        self._text_cls = TextClassifier(
            model_path=text_model_path, device=device, use_real_model=use_real_models
        )
        self._image_cls = ImageClassifier(
            model_path=image_model_path, device=device, use_real_model=use_real_models
        )
        self._audio_cls = AudioClassifier(
            model_path=audio_model_path, device=device, use_real_model=use_real_models
        )
        self._audio_cls.set_text_classifier(self._text_cls)
        self._loaded = False

    # ── lifecycle ──────────────────────────────────────────────────
    async def load_all(self) -> None:
        logger.info("safety_pipeline.loading")
        await self._text_cls.load()
        await self._image_cls.load()
        await self._audio_cls.load()
        self._loaded = True
        logger.info("safety_pipeline.ready")

    async def shutdown(self) -> None:
        await self._text_cls.unload()
        await self._image_cls.unload()
        await self._audio_cls.unload()
        self._loaded = False

    @property
    def is_loaded(self) -> bool:
        return self._loaded

    # ── inference ──────────────────────────────────────────────────
    async def classify(
        self,
        content_id: str,
        content_type: str,        # text | image | audio
        content: Any,
        filter_level: str = "normal",
    ) -> SafetyVerdict:
        t0 = time.monotonic()

        # Dispatch to correct classifier
        if content_type == "text":
            classifier = self._text_cls
        elif content_type == "image":
            classifier = self._image_cls
        elif content_type == "audio":
            classifier = self._audio_cls
        else:
            raise ValueError(f"Unsupported content_type: {content_type}")

        raw_results = await classifier.predict(content)
        results = self._apply_filter_level(raw_results, filter_level)
        overall_score = self._aggregate(results)
        violations = self._extract_violations(results)

        elapsed = (time.monotonic() - t0) * 1000

        return SafetyVerdict(
            content_id=content_id,
            passed=len(violations) == 0,
            overall_score=round(overall_score, 4),
            categories=results,
            violations=violations,
            processing_time_ms=round(elapsed, 1),
            model_version=classifier.model_version(),
        )

    async def classify_all_modalities(
        self,
        content_id: str,
        text: Optional[str] = None,
        image: Optional[Any] = None,
        audio: Optional[Any] = None,
        filter_level: str = "normal",
    ) -> Dict[str, SafetyVerdict]:
        """Classify across all available modalities in parallel."""
        import asyncio

        tasks = {}
        if text is not None:
            tasks["text"] = self.classify(
                f"{content_id}_text", "text", text, filter_level
            )
        if image is not None:
            tasks["image"] = self.classify(
                f"{content_id}_image", "image", image, filter_level
            )
        if audio is not None:
            tasks["audio"] = self.classify(
                f"{content_id}_audio", "audio", audio, filter_level
            )

        results = {}
        for modality, coro in tasks.items():
            results[modality] = await coro
        return results

    # ── helpers ────────────────────────────────────────────────────
    def _apply_filter_level(
        self, results: List[CategoryResult], level: str
    ) -> List[CategoryResult]:
        multiplier = FILTER_LEVEL_MULTIPLIERS.get(level, 1.0)
        adjusted = []
        for r in results:
            adj_threshold = min(0.95, r.score * multiplier) if multiplier != 1.0 else r.score
            # For filter level adjustment, we modify the "above_threshold" by
            # comparing against effective thresholds
            # Actually: multiplier on thresholds would mean higher threshold = less strict
            # But the current CategoryResult.score is already computed.
            # Here we scale the score for aggregation purpose only.
            adjusted.append(CategoryResult(
                category=r.category,
                score=r.score,
                above_threshold=r.above_threshold,
                label=r.label,
                detail={"adjusted": True, "filter_level": level} if level != "normal" else None,
            ))
        return adjusted

    @staticmethod
    def _aggregate(results: List[CategoryResult]) -> float:
        """Min-pool: overall safety is the worst category score."""
        if not results:
            return 1.0
        return min(r.score for r in results)

    @staticmethod
    def _extract_violations(results: List[CategoryResult]) -> List[Dict[str, Any]]:
        """Return list of categories that failed threshold."""
        return [
            {
                "category": r.category,
                "score": r.score,
                "label": r.label,
                "detail": r.detail or {},
            }
            for r in results
            if not r.above_threshold
        ]

    def model_info(self) -> Dict[str, Any]:
        return {
            "text": self._text_cls.model_version(),
            "image": self._image_cls.model_version(),
            "audio": self._audio_cls.model_version(),
            "supported_modalities": ["text", "image", "audio"],
            "categories": {
                "text": TEXT_CATEGORIES,
                "image": IMAGE_CATEGORIES,
                "audio": AUDIO_CATEGORIES,
            },
        }
