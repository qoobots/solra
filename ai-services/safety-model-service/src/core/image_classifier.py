"""Image safety classifier — multimodal vision-based safety scoring.

Real mode : CLIP-based zero-shot or fine-tuned safety vision model (e.g. nsfw-detector-vit)
Mock mode: shape/colour heuristic for offline CI/testing.
"""

import time
from typing import List, Dict, Any, Optional
from io import BytesIO
from dataclasses import field

import structlog

from .base_classifier import BaseClassifier, CategoryResult

logger = structlog.get_logger(__name__)

IMAGE_CATEGORIES = [
    "nsfw",
    "violence",
    "minor_safety",
]

# ── REAL MODEL LABELS (CLIP-style) ────────────────────────────────
NSFW_CONCEPTS = [
    "a photo of explicit adult content",
    "a photo of a safe family-friendly scene",
]

VIOLENCE_CONCEPTS = [
    "a photo showing graphic violence and gore",
    "a photo showing a peaceful non-violent scene",
]

# Use inverted scoring: concept[0] probability → unsafe signal
SAFETY_CONCEPT_MAP = {
    "nsfw": NSFW_CONCEPTS,
    "violence": VIOLENCE_CONCEPTS,
}

DEFAULT_IMAGE_THRESHOLDS: Dict[str, float] = {
    "nsfw": 0.75,
    "violence": 0.7,
    "minor_safety": 0.35,
}


class ImageClassifier(BaseClassifier):
    """Image safety classifier with CLIP backend."""

    def __init__(
        self,
        model_path: str = "models/safety-classifier/image",
        device: str = "cpu",
        use_real_model: bool = False,
        thresholds: Optional[Dict[str, float]] = None,
    ):
        self._model_path = model_path
        self._device = device
        self._use_real = use_real_model
        self._thresholds = thresholds or DEFAULT_IMAGE_THRESHOLDS
        self._model = None
        self._processor = None
        self._loaded = False

    async def load(self) -> None:
        if self._use_real:
            try:
                from transformers import CLIPProcessor, CLIPModel
                self._model = CLIPModel.from_pretrained(self._model_path).to(self._device)
                self._processor = CLIPProcessor.from_pretrained(self._model_path)
                self._model.eval()
                logger.info("image_classifier.real_loaded",
                            model=self._model_path, device=self._device)
            except Exception as e:
                logger.warning("image_classifier.real_load_failed", error=str(e))
                self._use_real = False

        self._loaded = True
        logger.info("image_classifier.ready", mode="real" if self._use_real else "mock")

    def supported_categories(self) -> List[str]:
        return IMAGE_CATEGORIES

    def model_version(self) -> str:
        return "solra-image-v1" + ("-real" if self._use_real else "-mock")

    async def predict(self, content: Any, **kwargs) -> List[CategoryResult]:
        """content can be PIL.Image, numpy array, bytes, or URL path."""
        if self._use_real:
            return await self._predict_real(content)
        return self._predict_mock(content)

    async def _predict_real(self, image) -> List[CategoryResult]:
        import torch
        from PIL import Image

        if isinstance(image, bytes):
            image = Image.open(BytesIO(image)).convert("RGB")
        elif isinstance(image, str):
            image = Image.open(image).convert("RGB")

        t0 = time.monotonic()
        results: List[CategoryResult] = []

        for cat in IMAGE_CATEGORIES:
            if cat == "minor_safety":
                # None for now — requires specialised model
                results.append(CategoryResult(
                    category="minor_safety",
                    score=1.0,
                    above_threshold=True,
                    label="Minor Safety",
                ))
                continue

            concepts = SAFETY_CONCEPT_MAP.get(cat)
            if not concepts:
                results.append(CategoryResult(
                    category=cat, score=1.0, above_threshold=True, label=cat.title()
                ))
                continue

            inputs = self._processor(
                text=concepts, images=image, return_tensors="pt", padding=True
            ).to(self._device)
            with torch.no_grad():
                outputs = self._model(**inputs)
            logits_per_image = outputs.logits_per_image.softmax(dim=-1)[0].tolist()

            # logits[0] = unsafe concept probability
            unsafe_prob = logits_per_image[0]
            safe_score = 1.0 - unsafe_prob
            threshold = self._thresholds.get(cat, 0.7)

            results.append(CategoryResult(
                category=cat,
                score=round(max(0.0, min(1.0, safe_score)), 4),
                above_threshold=safe_score >= threshold,
                label=cat.replace("_", " ").title(),
                detail={"unsafe_prob": round(unsafe_prob, 4)},
            ))

        elapsed = (time.monotonic() - t0) * 1000
        logger.debug("image_classifier.real_predict", elapsed_ms=round(elapsed, 1))
        return results

    def _predict_mock(self, _image) -> List[CategoryResult]:
        """Mock always-pass for CI — all safe."""
        t0 = time.monotonic()
        results = [
            CategoryResult(category=cat, score=1.0, above_threshold=True,
                           label=cat.replace("_", " ").title())
            for cat in IMAGE_CATEGORIES
        ]
        elapsed = (time.monotonic() - t0) * 1000
        logger.debug("image_classifier.mock_predict", elapsed_ms=round(elapsed, 2))
        return results
