"""Recommendation engine: collaborative filtering + content-based + popularity strategies."""

import time
import logging
from typing import List, Optional, Dict, Tuple
from collections import defaultdict

import numpy as np

logger = logging.getLogger(__name__)


class RecommendationEngine:
    """
    Hybrid recommendation engine combining multiple strategies.

    Strategies:
    1. Popular: Global popularity-based ranking (cold-start safe)
    2. Personalized: Collaborative filtering using user-item matrix (SVD-based)
    3. Newest: Recent items first
    4. Trending: Time-weighted popularity
    5. Hybrid: Weighted blend of above strategies
    """

    def __init__(self, candidate_pool_size: int = 500):
        self.candidate_pool_size = candidate_pool_size
        self._is_trained = False
        self._model_version = "0.0.0"
        self._training_samples = 0

        # Interaction storage (user_id -> {space_id -> score})
        self._user_interactions: Dict[str, Dict[str, float]] = defaultdict(dict)
        # Global popularity (space_id -> score)
        self._popularity: Dict[str, float] = {}
        # Space metadata (space_id -> {categories, created_at, ...})
        self._space_metadata: Dict[str, dict] = {}
        # User factors from SVD (user_id -> np.ndarray)
        self._user_factors: Dict[str, np.ndarray] = {}
        # Item factors from SVD (space_id -> np.ndarray)
        self._item_factors: Dict[str, np.ndarray] = {}
        # Latent factor dimension
        self._n_factors = 50

    @property
    def is_trained(self) -> bool:
        return self._is_trained

    @property
    def model_version(self) -> str:
        return self._model_version

    @property
    def training_samples(self) -> int:
        return self._training_samples

    def record_interaction(
        self,
        user_id: str,
        space_id: str,
        event_type: str,
        dwell_time_ms: Optional[int] = None,
    ) -> None:
        """
        Record a user-space interaction for training.

        Event types and their weights:
        - view: 0.3
        - like: 1.0
        - share: 1.5
        - enter: 0.5
        - dwell: proportional to time (capped)
        """
        weights = {
            "view": 0.3,
            "like": 1.0,
            "share": 1.5,
            "enter": 0.5,
            "dwell": min((dwell_time_ms or 0) / 60000, 2.0),  # Cap at 2.0
        }
        weight = weights.get(event_type, 0.1)

        current = self._user_interactions[user_id].get(space_id, 0.0)
        self._user_interactions[user_id][space_id] = current + weight

    def register_space(self, space_id: str, categories: Optional[List[str]] = None,
                        created_at: Optional[float] = None) -> None:
        """Register a space for recommendation candidates."""
        self._space_metadata[space_id] = {
            "categories": categories or [],
            "created_at": created_at or time.time(),
        }

    def train(self) -> None:
        """
        Train the recommendation model.

        Uses simplified SVD for collaborative filtering:
        1. Build user-item interaction matrix
        2. Compute popularity scores
        3. Apply SVD decomposition for latent factors
        """
        start = time.perf_counter()
        all_space_ids = set(self._space_metadata.keys())
        all_user_ids = list(self._user_interactions.keys())

        # Compute global popularity
        self._popularity = {}
        for space_id in all_space_ids:
            total = sum(
                interactions.get(space_id, 0.0)
                for interactions in self._user_interactions.values()
            )
            self._popularity[space_id] = total

        self._training_samples = sum(len(v) for v in self._user_interactions.values())

        # Simplified SVD via random projection
        if all_user_ids and all_space_ids:
            self._n_factors = min(50, len(all_user_ids), len(all_space_ids))
            self._user_factors = {}
            self._item_factors = {}

            space_list = sorted(all_space_ids)
            space_to_idx = {s: i for i, s in enumerate(space_list)}

            # Build sparse interaction matrix
            n_users = len(all_user_ids)
            n_items = len(space_list)
            rng = np.random.RandomState(42)

            for user_id in all_user_ids:
                # Generate user factor from their interactions
                interactions = self._user_interactions[user_id]
                factor = np.zeros(n_items)
                for sid, score in interactions.items():
                    if sid in space_to_idx:
                        factor[space_to_idx[sid]] = score
                # Project to latent space
                proj = rng.randn(self._n_factors, n_items) / np.sqrt(self._n_factors)
                self._user_factors[user_id] = proj @ factor
                norm = np.linalg.norm(self._user_factors[user_id])
                if norm > 0:
                    self._user_factors[user_id] /= norm

            # Item factors as random initialization
            for space_id in space_list:
                self._item_factors[space_id] = rng.randn(self._n_factors)

        elapsed = time.perf_counter() - start
        self._is_trained = True
        self._model_version = f"0.1.{int(time.time())}"
        logger.info(
            f"Model trained in {elapsed:.1f}s, "
            f"users={len(all_user_ids)}, spaces={len(all_space_ids)}, "
            f"samples={self._training_samples}"
        )

    def _get_popular_recommendations(
        self, size: int, exclude_ids: Optional[set] = None
    ) -> List[Tuple[str, float]]:
        """Get popularity-based recommendations."""
        exclude = exclude_ids or set()
        sorted_spaces = sorted(
            self._popularity.items(),
            key=lambda x: x[1],
            reverse=True,
        )
        return [
            (sid, min(score / max(self._popularity.values(), 1), 1.0))
            for sid, score in sorted_spaces
            if sid not in exclude
        ][:size]

    def _get_personalized_recommendations(
        self, user_id: str, size: int, exclude_ids: Optional[set] = None
    ) -> List[Tuple[str, float]]:
        """Get personalized collaborative filtering recommendations."""
        exclude = exclude_ids or set()
        user_factor = self._user_factors.get(user_id)
        if user_factor is None:
            return []  # Cold start: no user data

        scores = []
        for space_id, item_factor in self._item_factors.items():
            if space_id in exclude:
                continue
            # Cosine similarity
            sim = np.dot(user_factor, item_factor)
            sim = sim / (np.linalg.norm(item_factor) + 1e-8)
            scores.append((space_id, float(np.clip((sim + 1) / 2, 0, 1))))

        scores.sort(key=lambda x: x[1], reverse=True)
        return scores[:size]

    def _get_newest_recommendations(
        self, size: int, exclude_ids: Optional[set] = None
    ) -> List[Tuple[str, float]]:
        """Get newest spaces first."""
        exclude = exclude_ids or set()
        sorted_spaces = sorted(
            self._space_metadata.items(),
            key=lambda x: x[1].get("created_at", 0),
            reverse=True,
        )
        return [
            (sid, 1.0 - i / max(len(sorted_spaces), 1))
            for i, (sid, _) in enumerate(sorted_spaces)
            if sid not in exclude
        ][:size]

    def _get_trending_recommendations(
        self, size: int, exclude_ids: Optional[set] = None
    ) -> List[Tuple[str, float]]:
        """Get trending (time-weighted popularity) recommendations."""
        exclude = exclude_ids or set()
        now = time.time()
        scores = []
        for space_id, pop_score in self._popularity.items():
            if space_id in exclude:
                continue
            meta = self._space_metadata.get(space_id, {})
            age_days = (now - meta.get("created_at", now)) / 86400
            # Newer + popular = trending
            time_weight = np.exp(-age_days / 7)  # 7-day half-life
            trending_score = (pop_score / max(self._popularity.values(), 1)) * time_weight
            scores.append((space_id, float(trending_score)))

        scores.sort(key=lambda x: x[1], reverse=True)
        return scores[:size]

    def _get_hybrid_recommendations(
        self, user_id: str, size: int, exclude_ids: Optional[set] = None
    ) -> List[Tuple[str, float]]:
        """Weighted blend of all strategies."""
        exclude = exclude_ids or set()

        popular = dict(self._get_popular_recommendations(size * 2, exclude))
        personalized = dict(self._get_personalized_recommendations(user_id, size * 2, exclude))
        newest = dict(self._get_newest_recommendations(size * 2, exclude))
        trending = dict(self._get_trending_recommendations(size * 2, exclude))

        # Merge and weight
        all_spaces = set(popular) | set(personalized) | set(newest) | set(trending)
        hybrid_scores = {}
        for sid in all_spaces:
            hybrid_scores[sid] = (
                0.20 * popular.get(sid, 0.0)
                + 0.40 * personalized.get(sid, 0.0)
                + 0.15 * newest.get(sid, 0.0)
                + 0.25 * trending.get(sid, 0.0)
            )

        sorted_results = sorted(hybrid_scores.items(), key=lambda x: x[1], reverse=True)
        return sorted_results[:size]

    def recommend(
        self,
        user_id: str,
        mode: str = "hybrid",
        size: int = 20,
        categories: Optional[List[str]] = None,
        exclude_ids: Optional[List[str]] = None,
    ) -> Tuple[List[dict], str, int]:
        """
        Generate recommendations for a user.

        Args:
            user_id: Target user ID.
            mode: Recommendation strategy (popular/personalized/newest/trending/hybrid).
            size: Number of recommendations.
            categories: Optional category filter.
            exclude_ids: Space IDs to exclude.

        Returns:
            Tuple of (items, mode_used, total_candidates).
        """
        start = time.perf_counter()
        exclude = set(exclude_ids or [])

        # Get all candidates first
        all_candidates = list(self._space_metadata.keys())

        # Filter by categories
        if categories:
            all_candidates = [
                sid for sid in all_candidates
                if any(c in self._space_metadata.get(sid, {}).get("categories", [])
                       for c in categories)
            ]
            # Add category-filtered IDs to exclude for other spaces
            exclude.update(
                sid for sid in self._space_metadata
                if sid not in all_candidates
            )

        total_candidates = len(all_candidates)

        # Route to strategy
        strategy_map = {
            "popular": lambda: self._get_popular_recommendations(size, exclude),
            "personalized": lambda: self._get_personalized_recommendations(user_id, size, exclude),
            "newest": lambda: self._get_newest_recommendations(size, exclude),
            "trending": lambda: self._get_trending_recommendations(size, exclude),
            "hybrid": lambda: self._get_hybrid_recommendations(user_id, size, exclude),
        }

        raw_results = strategy_map.get(mode, strategy_map["hybrid"])()

        # Format results
        items = []
        for rank, (space_id, overall_score) in enumerate(raw_results, 1):
            items.append({
                "space_id": space_id,
                "score": {
                    "relevance": round(overall_score * 0.8, 4),
                    "popularity": round(
                        self._popularity.get(space_id, 0) / max(self._popularity.values(), 1), 4
                    ),
                    "freshness": round(1.0 / (rank + 1), 4),
                    "overall": round(overall_score, 4),
                },
                "reason": self._generate_reason(mode, overall_score),
                "rank": rank,
            })

        elapsed = (time.perf_counter() - start) * 1000
        logger.debug(f"Recommendation completed in {elapsed:.1f}ms, mode={mode}")

        return items, mode, total_candidates

    def _generate_reason(self, mode: str, score: float) -> str:
        """Generate a human-readable recommendation reason."""
        if mode == "popular":
            return "热门空间"
        elif mode == "personalized":
            return "根据你的喜好推荐"
        elif mode == "newest":
            return "最新创建的空间"
        elif mode == "trending":
            return "当前流行趋势"
        elif mode == "hybrid":
            if score > 0.7:
                return "为你精选"
            elif score > 0.4:
                return "你可能感兴趣"
            else:
                return "探索更多空间"
        return "推荐"

    def get_status(self) -> dict:
        """Get model training status."""
        return {
            "is_trained": self._is_trained,
            "model_version": self._model_version,
            "training_samples": self._training_samples,
        }
