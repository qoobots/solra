"""Pydantic data models for Embedding Service."""

from typing import List, Optional, Union
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


class SimilarityRequest(BaseModel):
    """Request for similarity computation between texts."""
    query: str = Field(..., min_length=1, description="Query text")
    candidates: List[str] = Field(..., min_length=1, max_length=1000, description="Candidate texts")
    top_k: int = Field(10, ge=1, le=100, description="Number of top results to return")


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


class ModelInfo(BaseModel):
    """Information about a loaded embedding model."""
    name: str
    dimensions: int
    max_seq_length: int
    device: str
    is_loaded: bool


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
