package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PreloadManager — SPC-005 空间预测性预加载引擎。
 *
 * 基于用户行为预测下一个空间，后台预加载完成率>80%。
 * 策略：基于用户浏览/停留/跳过历史，预测下一个可能进入的空间。
 */
public class PreloadManager {

    private static final Logger log = LoggerFactory.getLogger(PreloadManager.class);

    /** 预加载缓存：userId -> 预加载条目 */
    private final Map<String, PreloadEntry> preloadCache = new ConcurrentHashMap<>();

    /** 预加载缓存最大条数 */
    private static final int MAX_CACHE_SIZE = 500;

    /** 预加载缓存过期时间（分钟） */
    private static final int CACHE_TTL_MINUTES = 10;

    /** 预加载目标数 */
    private static final int PRELOAD_TARGET_COUNT = 3;

    private final SpaceRepository spaceRepo;
    private final UserActionRepository actionRepo;
    private final StreamingLoader streamingLoader;

    public PreloadManager(SpaceRepository spaceRepo, UserActionRepository actionRepo,
                           StreamingLoader streamingLoader) {
        this.spaceRepo = spaceRepo;
        this.actionRepo = actionRepo;
        this.streamingLoader = streamingLoader;
    }

    /**
     * Predict and preload the next spaces a user is likely to enter.
     * Returns the predicted space IDs for client-side pre-warming.
     */
    public PreloadPrediction predict(String userId, String currentSpaceId, int count) {
        cleanExpiredCache();

        // 1. Analyze recent user actions
        List<UserAction> recentActions = actionRepo.findByUserId(userId, 0, 50);
        Set<String> visitedSpaceIds = new HashSet<>();
        Map<String, Long> dwellByCategory = new HashMap<>();

        for (UserAction action : recentActions) {
            visitedSpaceIds.add(action.getSpaceId());
            if (action.getActionType() == UserActionType.DWELL) {
                spaceRepo.findById(action.getSpaceId()).ifPresent(space -> {
                    if (space.getMeta() != null && space.getMeta().getCategory() != null) {
                        dwellByCategory.merge(space.getMeta().getCategory().name(),
                                action.getDwellDurationMs(), Long::sum);
                    }
                });
            }
        }

        // 2. Find preferred categories (top 3 by dwell time)
        List<String> preferredCategories = dwellByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 3. Fetch candidates: popular spaces in preferred categories, excluding visited
        List<Space> candidates = new ArrayList<>();
        for (String catName : preferredCategories) {
            try {
                SpaceCategory cat = SpaceCategory.valueOf(catName);
                List<Space> spaces = spaceRepo.findPublished(0, 20, List.of(cat), "popular");
                for (Space s : spaces) {
                    if (!visitedSpaceIds.contains(s.getSpaceId())
                            && !s.getSpaceId().equals(currentSpaceId)) {
                        candidates.add(s);
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        // 4. If not enough, add trending spaces
        if (candidates.size() < count) {
            List<Space> trending = spaceRepo.findPublished(0, 20, List.of(), "trending");
            for (Space s : trending) {
                if (!visitedSpaceIds.contains(s.getSpaceId())
                        && !s.getSpaceId().equals(currentSpaceId)) {
                    candidates.add(s);
                }
                if (candidates.size() >= count * 2) break;
            }
        }

        // 5. Score and sort candidates
        List<ScoredCandidate> scored = new ArrayList<>();
        for (Space space : candidates) {
            double score = computePredictionScore(space, recentActions, preferredCategories);
            scored.add(new ScoredCandidate(space, score));
        }
        scored.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());

        // 6. Select top N
        List<String> predictedIds = scored.stream()
                .limit(Math.min(count, PRELOAD_TARGET_COUNT))
                .map(s -> s.space.getSpaceId())
                .toList();

        // 7. Estimate preload times
        Map<String, Long> loadTimeEstimates = new LinkedHashMap<>();
        for (String id : predictedIds) {
            long estimate = streamingLoader.estimateLoadTimeMs(id, "wifi");
            loadTimeEstimates.put(id, estimate);
        }

        // 8. Cache the prediction
        PreloadEntry entry = new PreloadEntry(predictedIds, Instant.now());
        preloadCache.put(userId, entry);

        log.debug("SPC-005 preload predicted: user={} spaces={}", userId, predictedIds);
        return new PreloadPrediction(predictedIds, loadTimeEstimates,
                predictedIds.size() > 0 ? (double) predictedIds.size() / PRELOAD_TARGET_COUNT : 0);
    }

    /**
     * Get the current cached preload prediction without recomputing.
     */
    public Optional<PreloadPrediction> getCachedPrediction(String userId) {
        cleanExpiredCache();
        PreloadEntry entry = preloadCache.get(userId);
        if (entry == null) return Optional.empty();

        Map<String, Long> estimates = new LinkedHashMap<>();
        for (String id : entry.spaceIds()) {
            estimates.put(id, streamingLoader.estimateLoadTimeMs(id, "wifi"));
        }
        return Optional.of(new PreloadPrediction(entry.spaceIds(), estimates,
                entry.spaceIds().size() > 0 ? (double) entry.spaceIds().size() / PRELOAD_TARGET_COUNT : 0));
    }

    /**
     * Get preload statistics.
     */
    public PreloadStats getStats() {
        cleanExpiredCache();
        int activeUsers = preloadCache.size();
        long totalPredictions = preloadCache.values().stream()
                .mapToLong(e -> e.spaceIds().size())
                .sum();
        return new PreloadStats(activeUsers, totalPredictions);
    }

    private double computePredictionScore(Space space, List<UserAction> recentActions,
                                           List<String> preferredCategories) {
        double score = 0;

        // Category preference
        if (space.getMeta() != null && space.getMeta().getCategory() != null) {
            int catIdx = preferredCategories.indexOf(space.getMeta().getCategory().name());
            if (catIdx >= 0) {
                score += (3 - catIdx) * 2.0; // Higher for top preferred category
            }
        }

        // Popularity
        if (space.getStats() != null) {
            score += Math.log1p(space.getStats().getViewCount()) * 0.5;
            score += Math.log1p(space.getStats().getLikeCount()) * 0.3;
        }

        // Similar tags to recently viewed
        Set<String> recentTags = new HashSet<>();
        for (UserAction action : recentActions) {
            spaceRepo.findById(action.getSpaceId()).ifPresent(s -> {
                if (s.getTags() != null) recentTags.addAll(s.getTags());
            });
        }
        if (space.getTags() != null) {
            long commonTags = space.getTags().stream()
                    .filter(recentTags::contains)
                    .count();
            score += commonTags * 1.5;
        }

        return score;
    }

    private void cleanExpiredCache() {
        Instant cutoff = Instant.now().minusSeconds(CACHE_TTL_MINUTES * 60L);
        preloadCache.entrySet().removeIf(e -> e.getValue().timestamp().isBefore(cutoff));
        if (preloadCache.size() > MAX_CACHE_SIZE) {
            // Remove oldest entries
            List<String> toRemove = preloadCache.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(
                            Comparator.comparing(PreloadEntry::timestamp)))
                    .limit(preloadCache.size() - MAX_CACHE_SIZE)
                    .map(Map.Entry::getKey)
                    .toList();
            toRemove.forEach(preloadCache::remove);
        }
    }

    // -- Inner types --

    public record PreloadPrediction(List<String> predictedSpaceIds,
                                     Map<String, Long> loadTimeEstimatesMs,
                                     double preloadCoverageRate) {}

    public record PreloadStats(int activeUsers, long totalPredictions) {}

    private record PreloadEntry(List<String> spaceIds, Instant timestamp) {}

    private record ScoredCandidate(Space space, double score) {}
}
