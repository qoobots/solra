"""Abstract base classifier interface for all modalities."""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import List, Dict, Any, Optional


@dataclass
class CategoryResult:
    """Single-category classification result."""
    category: str
    score: float               # 0.0 = unsafe, 1.0 = safe
    above_threshold: bool       # True if below violation threshold
    label: str = ""             # human-readable label
    detail: Optional[Dict[str, Any]] = None


@dataclass
class SafetyVerdict:
    """Complete safety classification verdict."""
    content_id: str
    passed: bool                          # overall pass/fail
    overall_score: float                  # aggregate safety 0-1
    categories: List[CategoryResult] = field(default_factory=list)
    violations: List[Dict[str, Any]] = field(default_factory=list)
    processing_time_ms: float = 0.0
    model_version: str = "unknown"
    raw_logits: Optional[Dict[str, float]] = None


class BaseClassifier(ABC):
    """Abstract classifier — pluggable for text / image / audio."""

    @abstractmethod
    async def load(self) -> None:
        """Load model weights into memory."""
        ...

    @abstractmethod
    async def predict(self, content: Any, **kwargs) -> List[CategoryResult]:
        """Run inference on a single piece of content."""
        ...

    @abstractmethod
    def supported_categories(self) -> List[str]:
        """Return category names this classifier handles."""
        ...

    @abstractmethod
    def model_version(self) -> str:
        """Return model version string."""
        ...

    async def unload(self) -> None:
        """Optional: release model from memory."""
        pass
