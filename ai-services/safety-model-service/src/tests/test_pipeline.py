"""
Tests for safety model service — unit + integration.

Run: pytest src/tests/ -v
"""

import pytest
from ..core.base_classifier import CategoryResult, SafetyVerdict
from ..core.text_classifier import TextClassifier
from ..core.image_classifier import ImageClassifier
from ..core.audio_classifier import AudioClassifier
from ..core.pipeline import SafetyPipeline


@pytest.mark.asyncio
class TestTextClassifier:
    async def test_load_mock(self):
        cls = TextClassifier(use_real_model=False)
        await cls.load()
        assert cls._loaded

    async def test_safe_text(self):
        cls = TextClassifier(use_real_model=False)
        await cls.load()
        results = await cls.predict("Hello, how are you today?")
        assert all(r.above_threshold for r in results)
        assert all(r.score > 0.9 for r in results)

    async def test_hate_speech_detection(self):
        cls = TextClassifier(use_real_model=False)
        await cls.load()
        results = await cls.predict("You are a racist bigot")
        hate = next(r for r in results if r.category == "hate_speech")
        assert hate.score < 0.5

    async def test_violence_detection(self):
        cls = TextClassifier(use_real_model=False)
        await cls.load()
        results = await cls.predict("I will murder you tonight")
        violence = next(r for r in results if r.category == "violence")
        assert violence.score < 0.5

    async def test_self_harm_detection(self):
        cls = TextClassifier(use_real_model=False)
        await cls.load()
        results = await cls.predict("I want to end my life")
        sh = next(r for r in results if r.category == "self_harm")
        assert sh.score < 0.5

    async def test_clean_text_all_pass(self):
        cls = TextClassifier(use_real_model=False)
        await cls.load()
        clean = "The weather is beautiful today and I enjoyed a nice walk in the park."
        results = await cls.predict(clean)
        assert all(r.above_threshold for r in results)


@pytest.mark.asyncio
class TestImageClassifier:
    async def test_load_mock(self):
        cls = ImageClassifier(use_real_model=False)
        await cls.load()
        assert cls._loaded

    async def test_mock_all_safe(self):
        cls = ImageClassifier(use_real_model=False)
        await cls.load()
        results = await cls.predict(b"fake-image-bytes")
        assert len(results) == 3
        assert all(r.above_threshold for r in results)
        assert all(r.score == 1.0 for r in results)


@pytest.mark.asyncio
class TestAudioClassifier:
    async def test_load_mock(self):
        cls = AudioClassifier(use_real_model=False)
        await cls.load()
        assert cls._loaded

    async def test_mock_all_safe(self):
        cls = AudioClassifier(use_real_model=False)
        await cls.load()
        results = await cls.predict(b"fake-audio-bytes")
        assert len(results) == 4
        assert all(r.above_threshold for r in results)


@pytest.mark.asyncio
class TestSafetyPipeline:
    async def test_load_and_shutdown(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        assert pipe.is_loaded
        assert pipe.model_info()["text"] == "solra-text-v1-mock"
        await pipe.shutdown()
        assert not pipe.is_loaded

    async def test_classify_text_safe(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        verdict = await pipe.classify(
            content_id="test-001",
            content_type="text",
            content="Hello, this is a safe message.",
        )
        assert verdict.passed
        assert verdict.overall_score > 0.9
        assert len(verdict.violations) == 0
        assert verdict.processing_time_ms >= 0

    async def test_classify_text_unsafe(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        verdict = await pipe.classify(
            content_id="test-002",
            content_type="text",
            content="kill yourself you worthless piece of trash",
        )
        assert not verdict.passed
        assert verdict.overall_score < 0.5
        assert len(verdict.violations) > 0

    async def test_classify_image_mock(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        verdict = await pipe.classify(
            content_id="test-003",
            content_type="image",
            content=b"fake-image",
        )
        assert verdict.passed
        assert verdict.model_version == "solra-image-v1-mock"

    async def test_classify_all_modalities(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        results = await pipe.classify_all_modalities(
            content_id="test-004",
            text="A nice day",
        )
        assert "text" in results
        assert results["text"].passed

    async def test_filter_level_strict(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        verdict = await pipe.classify(
            content_id="test-005",
            content_type="text",
            content="buy now limited offer click here casino",
            filter_level="strict",
        )
        # spam detection should still work
        spam_results = [c for c in verdict.categories if c.category == "spam"]
        assert any(not c.above_threshold for c in spam_results)

    async def test_invalid_content_type(self):
        pipe = SafetyPipeline(device="cpu", use_real_models=False)
        await pipe.load_all()
        with pytest.raises(ValueError, match="Unsupported"):
            await pipe.classify("test-006", "video", "content")
