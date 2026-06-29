package com.solra.spc.domain.service;

import com.solra.spc.domain.model.Space;
import com.solra.spc.domain.model.SpaceCategory;
import com.solra.spc.domain.repository.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SpaceSearchService — SPC-004 空间搜索与分类浏览。
 *
 * 支持多维度筛选（分类/标签/排序）和关键词搜索。
 * 当前为内存实现，生产环境对接 Elasticsearch。
 */
public class SpaceSearchService {

    private static final Logger log = LoggerFactory.getLogger(SpaceSearchService.class);

    private final SpaceRepository spaceRepo;

    public SpaceSearchService(SpaceRepository spaceRepo) {
        this.spaceRepo = spaceRepo;
    }

    /**
     * Search spaces by keyword across title, description, and tags.
     * Results are ranked by relevance (keyword match) + popularity.
     */
    public SearchResult search(String keyword, List<SpaceCategory> categories,
                                String sortBy, int offset, int limit) {
        List<Space> all = spaceRepo.findPublished(0, 1000, categories, sortBy);

        List<ScoredSpace> scored = new ArrayList<>();
        for (Space space : all) {
            double relevance = computeRelevance(space, keyword);
            if (relevance > 0) {
                scored.add(new ScoredSpace(space, relevance));
            }
        }

        // Sort by relevance desc, then by specified sort
        if ("popular".equals(sortBy)) {
            scored.sort(Comparator.comparingDouble((ScoredSpace s) -> s.relevance).reversed()
                    .thenComparingLong(s -> s.space.getStats() != null ? s.space.getStats().getViewCount() : 0));
        } else if ("newest".equals(sortBy)) {
            scored.sort(Comparator.comparingDouble((ScoredSpace s) -> s.relevance).reversed()
                    .thenComparing((ScoredSpace s) -> s.space.getCreatedAt(),
                            Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            // Default: relevance
            scored.sort(Comparator.comparingDouble((ScoredSpace s) -> s.relevance).reversed());
        }

        int total = scored.size();
        List<Space> page = scored.stream()
                .skip(offset)
                .limit(limit)
                .map(s -> s.space)
                .collect(Collectors.toList());

        log.debug("SPC-004 search: keyword={} results={}", keyword, total);
        return new SearchResult(page, total);
    }

    /**
     * Browse spaces by category without keyword.
     */
    public List<Space> browseByCategory(List<SpaceCategory> categories,
                                          String sortBy, int offset, int limit) {
        return spaceRepo.findPublished(offset, limit, categories, sortBy);
    }

    /**
     * Get available filter facets (category counts, popular tags).
     */
    public SearchFacets getFacets() {
        List<Space> all = spaceRepo.findPublished(0, 1000, List.of(), "popular");

        Map<SpaceCategory, Long> categoryCounts = new EnumMap<>(SpaceCategory.class);
        Map<String, Long> tagCounts = new HashMap<>();

        for (Space space : all) {
            if (space.getMeta() != null && space.getMeta().getCategory() != null) {
                categoryCounts.merge(space.getMeta().getCategory(), 1L, Long::sum);
            }
            if (space.getTags() != null) {
                for (String tag : space.getTags()) {
                    tagCounts.merge(tag.toLowerCase(), 1L, Long::sum);
                }
            }
        }

        // Top 20 tags
        List<Map.Entry<String, Long>> topTags = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .toList();

        return new SearchFacets(categoryCounts, topTags.stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    private double computeRelevance(Space space, String keyword) {
        if (keyword == null || keyword.isBlank()) return 0;
        String lower = keyword.toLowerCase();
        String[] words = lower.split("\\s+");

        double score = 0;
        String title = space.getMeta() != null ? space.getMeta().getTitle() : "";
        String desc = space.getMeta() != null ? space.getMeta().getDescription() : "";
        List<String> tags = space.getTags() != null ? space.getTags() : List.of();

        String titleLower = title.toLowerCase();
        String descLower = desc.toLowerCase();

        for (String word : words) {
            // Title match: highest weight
            if (titleLower.contains(word)) score += 3.0;
            // Description match
            if (descLower.contains(word)) score += 1.5;
            // Tag match
            for (String tag : tags) {
                if (tag.toLowerCase().contains(word)) score += 2.0;
            }
            // Partial match bonus
            if (titleLower.startsWith(word)) score += 1.0;
        }

        // Popularity boost
        if (space.getStats() != null) {
            score += Math.log1p(space.getStats().getViewCount()) * 0.1;
        }

        return score;
    }

    // -- Inner types --

    public record SearchResult(List<Space> spaces, int total) {}

    public record SearchFacets(Map<SpaceCategory, Long> categoryCounts,
                                Map<String, Long> topTags) {}

    private record ScoredSpace(Space space, double relevance) {}
}
