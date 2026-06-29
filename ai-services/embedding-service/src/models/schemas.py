"""Pydantic data models for Embedding Service (text + multimodal)."""

from typing import List, Optional
from pydantic import BaseModel, Field


class TextEmbeddingRequest(BaseModel):
    """Request for text embedding generation."""
    texts: List[str] = Field(..., min_length=1, max_length=128, description="Texts to embed")
    model_name: Optional[str] = Field(None, description="Override default model")
    normalize: bool = Field(True, description="L2-normalize output vectors")


class TextEmbeddingResponse(BaseModel):
    """Response containing embedding vectors."""
    embeddings: List[List[float]] = Field(..., description="Embedding vectors (N x dim)")
    model_name: str = Field(..., description="Model used for embedding")
    dimensions: int = Field(..., description="Embedding vector dimensions")
    processing_time_ms: float = Field(0.0, description="Processing time in milliseconds")


class ImageEmbeddingRequest(BaseModel):
    """Request for image embedding generation."""
    images_base64: List[str] = Field(
        ..., min_length=1, max_length=64,
        description="Base64-encoded image strings (JPEG/PNG)"
    )
    normalize: bool = Field(True, description="L2-normalize output vectors")


class ImageEmbeddingResponse(BaseModel):
    """Response containing image embedding vectors."""
    embeddings: List[List[float]] = Field(..., description="Embedding vectors (N x vision_dim)")
    model_name: str = Field(..., description="Vision model used")
    dimensions: int = Field(..., description="Vision embedding dimensions")
    processing_time_ms: float = Field(0.0)


class MultimodalEmbeddingRequest(BaseModel):
    """Request for multimodal (text + image) embedding."""
    texts: Optional[List[str]] = Field(None, max_length=128, description="Texts to embed")
    images_base64: Optional[List[str]] = Field(
        None, max_length=64, description="Base64-encoded images"
    )
    normalize: bool = Field(True, description="L2-normalize output vectors")


class MultimodalEmbeddingResponse(BaseModel):
    """Response containing multimodal embedding vectors."""
    embeddings: List[List[float]] = Field(..., description="Unified embedding vectors")
    text_count: int = Field(0, description="Number of text embeddings")
    image_count: int = Field(0, description="Number of image embeddings")
    model_name: str = Field(..., description="Model used")
    dimensions: int = Field(..., description="Embedding dimensions")
    processing_time_ms: float = Field(0.0)


class SimilarityRequest(BaseModel):
    """Request for similarity computation between texts."""
    query: str = Field(..., min_length=1, description="Query text")
    candidates: List[str] = Field(..., min_length=1, max_length=1000, description="Candidate texts")
    top_k: int = Field(10, ge=1, le=100, description="Number of top results to return")


class CrossModalSimilarityRequest(BaseModel):
    """Request for cross-modal (text-to-image) similarity search."""
    query: str = Field(..., min_length=1, description="Text query")
    image_candidates_base64: List[str] = Field(
        ..., min_length=1, max_length=500,
        description="Base64-encoded image candidates"
    )
    top_k: int = Field(10, ge=1, le=100, description="Number of top results")


class CrossModalSimilarityResult(BaseModel):
    """A single cross-modal similarity result."""
    index: int = Field(..., description="Original image candidate index")
    score: float = Field(..., description="Similarity score")


class CrossModalSimilarityResponse(BaseModel):
    """Response for cross-modal similarity search."""
    results: List[CrossModalSimilarityResult]
    query: str
    total_candidates: int
    processing_time_ms: float = 0.0


class SimilarityResult(BaseModel):
    """A single similarity result."""
    index: int = Field(..., description="Original candidate index")
    text: str = Field(..., description="Candidate text")
    score: float = Field(..., description="Cosine similarity score [-1, 1]")


class SimilarityResponse(BaseModel):
    """Response containing similarity search results."""
    results: List[SimilarityResult] = Field(..., description="Top-K similarity results")
    query: str = Field(..., description="Original query text")
    total_candidates: int = Field(..., description="Total number of candidates evaluated")
    processing_time_ms: float = Field(0.0, description="Processing time in milliseconds")


class VisionModelInfo(BaseModel):
    """Vision model sub-info."""
    name: str
    dimensions: int
    is_loaded: bool


class ModelInfo(BaseModel):
    """Information about a loaded embedding model."""
    name: str
    dimensions: int
    max_seq_length: int
    device: str
    is_loaded: bool
    vision_model: Optional[VisionModelInfo] = None


class BatchEmbeddingRequest(BaseModel):
    """Request for batch embedding generation (for large-scale processing)."""
    texts: List[str] = Field(..., min_length=1, max_length=10000, description="Batch of texts to embed")
    batch_size: int = Field(32, ge=1, le=256, description="Internal batch size for processing")
    normalize: bool = Field(True, description="L2-normalize output vectors")


class BatchEmbeddingResponse(BaseModel):
    """Response for batch embedding generation."""
    embeddings: List[List[float]] = Field(..., description="Embedding vectors (N x dim)")
    model_name: str
    dimensions: int
    count: int = Field(..., description="Number of embeddings generated")
    processing_time_ms: float = 0.0
