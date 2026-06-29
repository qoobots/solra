"""API routes for LLM routing and backend management."""

import json
import structlog
from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from models.schemas import ChatRequest, ChatResponse, BackendStatus, RoutingDecision
from inference.clients import BackendRegistry, BaseLLMClient
from api.dependencies import get_registry, get_routing_engine, get_rate_limiter
from core.rate_limiter import TokenBucketRateLimiter

logger = structlog.get_logger(__name__)
router = APIRouter()


@router.post("/chat", response_model=ChatResponse, summary="Route a chat completion request")
async def chat_completion(
    request: ChatRequest,
    registry: BackendRegistry = Depends(get_registry),
    routing_engine=Depends(get_routing_engine),
    rate_limiter: TokenBucketRateLimiter = Depends(get_rate_limiter),
):
    """
    Intelligently route a chat request to the best available LLM backend.

    Routing logic:
    - Simple/short tasks → on-device SLM (low latency, free)
    - Complex/long tasks → cloud LLM (higher quality)
    - Primary failure → automatic fallback
    """
    # Rate limiting check
    rate_key = f"{request.user_id}:{request.conversation_id}"
    if not rate_limiter.allow(rate_key):
        raise HTTPException(
            status_code=429,
            detail="Too many requests. Please slow down.",
        )

    # Step 1: Make routing decision
    try:
        decision: RoutingDecision = await routing_engine.decide(request)
        logger.info("routing_decision",
                    conversation_id=request.conversation_id,
                    backend=decision.backend,
                    strategy=decision.strategy,
                    reason=decision.reason)
    except RuntimeError as e:
        logger.error("routing_failed", conversation_id=request.conversation_id, error=str(e))
        raise HTTPException(status_code=503, detail="No healthy LLM backend available")

    # Step 2: Execute on primary backend
    primary_client: BaseLLMClient = registry.get(decision.backend)
    if primary_client is None:
        raise HTTPException(status_code=500, detail=f"Backend '{decision.backend}' not found in registry")

    try:
        response = await primary_client.chat(request, decision.backend)
        return response
    except Exception as primary_error:
        logger.warning("primary_backend_failed",
                       backend=decision.backend,
                       error=str(primary_error),
                       fallback_available=decision.fallback_available)

        # Step 3: Fallback to next available backend
        if decision.fallback_available:
            healthy = registry.list_healthy()
            # Skip the failed primary
            fallback_clients = [b for b in healthy if b.config.name != decision.backend]
            if fallback_clients:
                fallback_client = fallback_clients[0]
                try:
                    response = await fallback_client.chat(request, decision.backend)
                    response.model_used = f"fallback({fallback_client.config.name})"
                    return response
                except Exception as fallback_error:
                    logger.error("fallback_failed", error=str(fallback_error))

        raise HTTPException(status_code=502, detail=f"All LLM backends failed: {primary_error}")


@router.get("/backends", response_model=BackendStatus, summary="List available LLM backends")
async def list_backends(registry: BackendRegistry = Depends(get_registry)):
    """Get information about all registered LLM backends and their health status."""
    infos = registry.get_infos()
    healthy_count = sum(1 for b in infos if b.is_healthy)

    return BackendStatus(
        backends=infos,
        routing_strategy=(
            get_routing_engine().strategy if get_routing_engine() else "cost_optimized"
        ),
        total_backends=len(infos),
        healthy_backends=healthy_count,
    )


@router.post("/backends/health-check", summary="Trigger health check for all backends")
async def check_backend_health(registry: BackendRegistry = Depends(get_registry)):
    """Manually trigger health checks for all registered backends."""
    results = await registry.check_all_health()
    return {"status": "completed", "results": results}


@router.get("/rate-limits/{user_id}", summary="Get rate limit status for a user")
async def get_rate_limit_status(
    user_id: str,
    rate_limiter: TokenBucketRateLimiter = Depends(get_rate_limiter),
):
    """Get the current rate limit status for a specific user."""
    remaining = rate_limiter.get_remaining(user_id)
    return {
        "user_id": user_id,
        "remaining_tokens": round(remaining, 2),
        "rate_limit": rate_limiter.rate,
        "burst_limit": rate_limiter.burst,
    }


@router.post("/chat/stream", summary="Route a streaming chat completion request")
async def chat_completion_stream(
    request: ChatRequest,
    registry: BackendRegistry = Depends(get_registry),
    routing_engine=Depends(get_routing_engine),
    rate_limiter: TokenBucketRateLimiter = Depends(get_rate_limiter),
):
    """
    Intelligently route a chat request and return a Server-Sent Events (SSE) stream.

    Same routing logic as /chat but streams the response token by token.
    """
    # Rate limiting check
    rate_key = f"{request.user_id}:{request.conversation_id}"
    if not rate_limiter.allow(rate_key):
        raise HTTPException(
            status_code=429,
            detail="Too many requests. Please slow down.",
        )

    # Make routing decision
    try:
        decision: RoutingDecision = await routing_engine.decide(request)
        logger.info("streaming_routing_decision",
                    conversation_id=request.conversation_id,
                    backend=decision.backend,
                    strategy=decision.strategy)
    except RuntimeError as e:
        logger.error("streaming_routing_failed", conversation_id=request.conversation_id, error=str(e))
        raise HTTPException(status_code=503, detail="No healthy LLM backend available")

    primary_client: BaseLLMClient = registry.get(decision.backend)
    if primary_client is None:
        raise HTTPException(status_code=500, detail=f"Backend '{decision.backend}' not found in registry")

    # Set streaming flag
    request.stream = True

    async def event_generator():
        try:
            response = await primary_client.chat(request, decision.backend)
            # Send the full response as SSE chunks (simulating token-by-token streaming)
            words = response.content.split()
            for i, word in enumerate(words):
                chunk = {
                    "conversation_id": response.conversation_id,
                    "turn_id": response.turn_id,
                    "content": word + (" " if i < len(words) - 1 else ""),
                    "model_used": response.model_used,
                    "finish_reason": None if i < len(words) - 1 else response.finish_reason,
                }
                yield f"data: {json.dumps(chunk, ensure_ascii=False)}\n\n"

            # Send the final [DONE] event
            yield "data: [DONE]\n\n"

        except Exception as e:
            logger.error("streaming_backend_error", error=str(e))
            error_chunk = {
                "error": str(e),
                "finish_reason": "error",
            }
            yield f"data: {json.dumps(error_chunk, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )

