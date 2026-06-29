"""Text safety classifier — NLP-based toxicity/safety scoring.

Real mode : HuggingFace transformers (e.g. shieldgemma-2b, multilingual-toxicity)
Mock mode: keyword + regex pattern matching for offline CI/testing.
"""

import re
import time
from typing import List, Dict, Any, Optional

import structlog

from .base_classifier import BaseClassifier, CategoryResult

logger = structlog.get_logger(__name__)

# ── DEFAULT CATEGORIES ──────────────────────────────────────────────
TEXT_CATEGORIES = [
    "nsfw",
    "hate_speech",
    "harassment",
    "violence",
    "self_harm",
    "spam",
    "fraud",
    "minor_safety",
    "personal_info",
]

# ── KEYWORD / REGEX PATTERNS (mock fallback) ────────────────────────
_KW_NSFW = [
    r"\b(?:porn|sex|nude|nsfw|xxx)\b",
]
_KW_HATE = [
    r"\b(?:racist|xenophob|bigot)\b",
]
_KW_HARASS = [
    r"\b(?:kill yourself|go die|f[*\u2025]ck you)\b",
]
_KW_VIOLENCE = [
    r"\b(?:murder|kill|bomb|terror|massacre|genocide)\b",
]
_KW_SELF_HARM = [
    r"\b(?:suicide|self[\s-]?harm|cut myself|end my life)\b",
]
_KW_SPAM = [
    r"(?:buy now|click here|limited offer|earn money fast|viagra|casino)",
]
_KW_FRAUD = [
    r"\b(?:phish|account verify|password reset.*click)\b",
]
_KW_MINOR = [
    r"\b(?:underage|child.*(?:porn|abuse)|minor.*explicit)\b",
]
_KW_PII = [
    r"\b\d{3}[-.]?\d{2}[-.]?\d{4}\b",   # SSN-like
    r"\b(?:\d{16})|(?:\d{4}[-\s]\d{4}[-\s]\d{4}[-\s]\d{4})\b",  # CC
]

CATEGORY_PATTERNS: Dict[str, List[str]] = {
    "nsfw": _KW_NSFW,
    "hate_speech": _KW_HATE,
    "harassment": _KW_HARASS,
    "violence": _KW_VIOLENCE,
    "self_harm": _KW_SELF_HARM,
    "spam": _KW_SPAM,
    "fraud": _KW_FRAUD,
    "minor_safety": _KW_MINOR,
    "personal_info": _KW_PII,
}

# ── THRESHOLDS (lower = stricter) ──────────────────────────────────
DEFAULT_THRESHOLDS: Dict[str, float] = {
    "nsfw": 0.7,
    "hate_speech": 0.5,
    "harassment": 0.6,
    "violence": 0.7,
    "self_harm": 0.3,
    "spam": 0.6,
    "fraud": 0.6,
    "minor_safety": 0.3,
    "personal_info": 0.5,
}


class TextClassifier(BaseClassifier):
    """Text safety classifier with mock/replaceable backend."""

    def __init__(
        self,
        model_path: str = "models/safety-classifier/text",
        device: str = "cpu",
        use_real_model: bool = False,
        thresholds: Optional[Dict[str, float]] = None,
    ):
        self._model_path = model_path
        self._device = device
        self._use_real = use_real_model
        self._thresholds = thresholds or DEFAULT_THRESHOLDS
        self._model = None
        self._tokenizer = None
        self._loaded = False

    # ── lifecycle ────────────────────────────────────────────────
    async def load(self) -> None:
        if self._use_real:
            try:
                from transformers import AutoTokenizer, AutoModelForSequenceClassification
                self._tokenizer = AutoTokenizer.from_pretrained(self._model_path)
                self._model = AutoModelForSequenceClassification.from_pretrained(
                    self._model_path
                ).to(self._device)
                self._model.eval()
                logger.info("text_classifier.real_loaded",
                            model=self._model_path, device=self._device)
            except Exception as e:
                logger.warning("text_classifier.real_load_failed", error=str(e))
                self._use_real = False  # fallback to mock

        self._loaded = True
        logger.info("text_classifier.ready", mode="real" if self._use_real else "mock")

    def supported_categories(self) -> List[str]:
        return TEXT_CATEGORIES

    def model_version(self) -> str:
        return "solra-text-v1" + ("-real" if self._use_real else "-mock")

    # ── inference ─────────────────────────────────────────────────
    async def predict(self, content: str, **kwargs) -> List[CategoryResult]:
        if self._use_real:
            return await self._predict_real(content)
        return self._predict_mock(content)

    async def _predict_real(self, text: str) -> List[CategoryResult]:
        """HuggingFace pipeline inference."""
        import torch

        t0 = time.monotonic()
        inputs = self._tokenizer(
            text, return_tensors="pt", truncation=True, max_length=512
        ).to(self._device)
        with torch.no_grad():
            outputs = self._model(**inputs)
        logits = outputs.logits[0].softmax(dim=-1).cpu().tolist()
        elapsed = (time.monotonic() - t0) * 1000

        results = []
        id2label = getattr(self._model.config, "id2label", {})
        for i, (label_id, label) in enumerate(id2label.items()):
            cat = label.lower().replace(" ", "_")
            if cat in self._thresholds:
                score = 1.0 - logits[i]  # invert: high logit → low safety
                threshold = self._thresholds[cat]
                results.append(CategoryResult(
                    category=cat,
                    score=round(max(0.0, min(1.0, score)), 4),
                    above_threshold=score >= threshold,
                    label=label,
                ))
        logger.debug("text_classifier.real_predict",
                     chars=len(text), elapsed_ms=round(elapsed, 1))
        return results

    def _predict_mock(self, text: str) -> List[CategoryResult]:
        """Fast keyword/regex fallback — ~0.1 ms."""
        t0 = time.monotonic()
        text_lower = text.lower()
        results: List[CategoryResult] = []

        for cat in TEXT_CATEGORIES:
            patterns = CATEGORY_PATTERNS.get(cat, [])
            score = 1.0
            for pat in patterns:
                if re.search(pat, text_lower):
                    score = 0.15  # strong signal
                    break
            threshold = self._thresholds.get(cat, 0.6)
            results.append(CategoryResult(
                category=cat,
                score=round(score, 4),
                above_threshold=score >= threshold,
                label=cat.replace("_", " ").title(),
            ))

        elapsed = (time.monotonic() - t0) * 1000
        logger.debug("text_classifier.mock_predict",
                     chars=len(text), elapsed_ms=round(elapsed, 2))
        return results
