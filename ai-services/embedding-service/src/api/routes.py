"""API routes for text embedding, multimodal embedding, and similarity computation."""

import time
import base64
import logging

from fastapi import APIRouter, Depends, HTTPException

from models.schemas import (
    TextEmbeddingRequest,
    TextEmbeddingResponse,
    ImageEmbeddingRequest,
    ImageEmbeddingResponse,
    MultimodalEmbeddingRequest,
    MultimodalEmbeddingResponse,
    SimilarityRequest,
    SimilarityResponse,
    SimilarityResult,
    CrossModalSimilarityRequest,
    CrossModalSimilarityResponse,
    CrossModalSimilarityResult,
    BatchEmbeddingRequest,
    BatchEmbeddingResponse,
    ModelInfo,
    VisionModelInfo,
)
from core.embedding_engine import EmbeddingEngine
from api.dependencies import get_engine

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["embedding"])


def _decode_base64_images(images_b64: list) -> list:
    """Decode base64-encoded images to bytes."""
    result = []
    for b64_str in images_b64:
        try:
            result.append(base64.b64decode(b64_str))
        except Exception:
            raise HTTPException(status_code=400, detail="Invalid base64-encoded image")
    return result


@router.post(
    "/embeddings",
    response_model=TextEmbeddingResponse,
    summary="Generate text embeddings",
)
async def generate_embeddings(
    request: TextEmbeddingRequest,
    engine: EmbeddingEngine = Depends(get_engine),
):
    """
    Generate embedding vectors for the given texts.

    Supports batch processing of up to 128 texts per request.
    """
    start = time.perf_counter()
    try:
        embeddings = engine.encode(
            texts=request.texts,
            normalize=request.normalize,
        )
        elapsed = (time.perf_counter() - start) * 1000

        return TextEmbeddingResponse(
            embeddings=embeddings.tolist(),
            model_name=engine.model_name,
            dimensions=engine.dimensions,
            processing_time_ms=round(elapsed, 2),
        )
    except Exception as e:
        logger.error(f"Embedding generation failed: {e}")
        raise HTTPException(status_code=500, detail=f"Embedding generation failed: {str(e)}")


@router.post(
    "/embeddings/image",
    response_model=ImageEmbeddingResponse,
    summary="Generate image embeddings",
)
async def generate_image_embeddings(
    request: ImageEmbeddingRequest,
    engine: EmbeddingEngine = Depends(get_engine),
):
    """
    Generate embedding vectors for images using the vision model.

    Accepts base64-encoded images (JPEG/PNG), up to 64 per request.
    """
    start = time.perf_counter()
    try:
        image_bytes = _decode_base64_images(request.images_base64)
        embeddings = engine.encode_image(
            images=image_bytes,
            normalize=request.normalize,
        )
        elapsed = (time.perf_counter() - start) * 1000

        return ImageEmbeddingResponse(
            embeddings=embeddings.tolist(),
            model_name=engine.vision_model_name,
            dimensions=engine.vision_dimensions,
            processing_time_ms=round(elapsed, 2),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Image embedding failed: {e}")
        raise HTTPException(status_code=500, detail=f"Image embedding failed: {str(e)}")


@router.post(
    "/embeddings/multimodal",
    response_model=MultimodalEmbeddingResponse,
    summary="Generate multimodal (text + image) embeddings",
)
async def generate_multimodal_embeddings(
    request: MultimodalEmbeddingRequest,
    engine: EmbeddingEngine = Depends(get_engine),
):
    """
    Generate unified embeddings for mixed text and image inputs.

    At least one of texts or images_base64 must be provided.
    Text embeddings are projected to the vision embedding space for cross-modal compatibility.
    """
    if not request.texts and not request.images_base64:
        raise HTTPException(status_code=400, detail="At least one of texts or images_base64 required")

    start = time.perf_counter()
    try:
        image_bytes = _decode_base64_images(request.images_base64) if request.images_base64 else None
        embeddings = engine.encode_multimodal(
            texts=request.texts,
            images=image_bytes,
            normalize=request.normalize,
        )
        elapsed = (time.perf_counter() - start) * 1000

        return MultimodalEmbeddingResponse(
            embeddings=embeddings.tolist(),
            text_count=len(request.texts) if request.texts else 0,
            image_count=len(request.images_base64) if request.images_base64 else 0,
            model_name=engine.vision_model_name,
            dimensions=engine.vision_dimensions,
            processing_time_ms=round(elapsed, 2),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Multimodal embedding failed: {e}")
        raise HTTPException(status_code=500, detail=f"Multimodal embedding failed: {str(e)}")


@router.post(
    "/similarity/cross-modal",
    response_model=CrossModalSimilarityResponse,
    summary="Cross-modal similarity: text query → image candidates",
)
async def cross_modal_similarity(
    request: CrossModalSimilarityRequest,
    engine: EmbeddingEngine = Depends(get_engine),
):
    """
    Compute similarity between a text query and image candidates (text-to-image search).

    Uses the vision model to compare text queries against image embeddings.
    """
    start = time.perf_counter()
    try:
        image_bytes = _decode_base64_images(request.image_candidates_base64)
        results = engine.cross_modal_similarity(
            query_text=request.query,
            image_candidates=image_bytes,
            top_k=request.top_k,
        )
        elapsed = (time.perf_counter() - start) * 1000

        return CrossModalSimilarityResponse(
            results=[
                CrossModalSimilarityResult(index=idx, score=round(score, 6))
                for idx, score in results
            ],
            query=request.query,
            total_candidates=len(request.image_candidates_base64),
            processing_time_ms=round(elapsed, 2),
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Cross-modal similarity failed: {e}")
        raise HTTPException(status_code=500, detail=f"Cross-modal similarity failed: {str(e)}")


@router.post(
    "/embeddings/batch",
    response_model=BatchEmbeddingResponse,
    summary="Batch generate embeddings for large text collections",
)
async def batch_embeddings(
    request: BatchEmbeddingRequest,
    engine: EmbeddingEngine = Depends(get_engine),
):
    """
    Generate embeddings for large batches (up to 10,000 texts).

    Uses configurable internal batch_size for efficient processing.
    """
    start = time.perf_counter()
    try:
        embeddings = engine.encode(
            texts=request.texts,
            batch_size=request.batch_size,
            normalize=request.normalize,
        )
        elapsed = (time.perf_counter() - start) * 1000

        return BatchEmbeddingResponse(
            embeddings=embeddings.tolist(),
            model_name=engine.model_name,
            dimensions=engine.dimensions,
            count=len(request.texts),
            processing_time_ms=round(elapsed, 2),
        )
    except Exception as e:
        logger.error(f"Batch embedding failed: {e}")
        raise HTTPException(status_code=500, detail=f"Batch embedding failed: {str(e)}")


@router.post(
    "/similarity",
    response_model=SimilarityResponse,
    summary="Compute text similarity",
)
async def compute_similarity(
    request: SimilarityRequest,
    engine: EmbeddingEngine = Depends(get_engine),
):
    """
    Compute cosine similarity between a query and candidate texts.

    Returns top-K most similar candidates with scores.
    """
    start = time.perf_counter()
    try:
        results = engine.similarity(
            query=request.query,
            candidates=request.candidates,
            top_k=request.top_k,
        )
        elapsed = (time.perf_counter() - start) * 1000

        similarity_results = [
            SimilarityResult(index=idx, text=text, score=round(score, 6))
            for idx, text, score in results
        ]

        return SimilarityResponse(
            results=similarity_results,
            query=request.query,
            total_candidates=len(request.candidates),
            processing_time_ms=round(elapsed, 2),
        )
    except Exception as e:
        logger.error(f"Similarity computation failed: {e}")
        raise HTTPException(status_code=500, detail=f"Similarity computation failed: {str(e)}")


@router.get(
    "/model",
    response_model=ModelInfo,
    summary="Get embedding model information",
)
async def get_model_info(
    engine: EmbeddingEngine = Depends(get_engine),
):
    """Get information about the currently loaded embedding model (text + vision)."""
    info = engine.get_model_info()
    vision_info = info.pop("vision_model", {})
    return ModelInfo(
        **info,
        vision_model=VisionModelInfo(**vision_info) if vision_info else None,
    )
