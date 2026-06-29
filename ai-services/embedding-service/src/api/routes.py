"""API routes for text embedding and similarity computation."""

import time
import logging

from fastapi import APIRouter, Depends, HTTPException

from models.schemas import (
    TextEmbeddingRequest,
    TextEmbeddingResponse,
    SimilarityRequest,
    SimilarityResponse,
    SimilarityResult,
    BatchEmbeddingRequest,
    BatchEmbeddingResponse,
    ModelInfo,
)
from core.embedding_engine import EmbeddingEngine
from api.dependencies import get_engine

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1", tags=["embedding"])


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
    """Get information about the currently loaded embedding model."""
    info = engine.get_model_info()
    return ModelInfo(**info)
