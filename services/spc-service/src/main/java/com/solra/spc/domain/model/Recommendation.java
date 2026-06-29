package com.solra.spc.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Recommendation 实体 — 推荐条目（含评分和理由）。
 */
public class Recommendation {
    private String spaceId;
    private RecommendScore score;
    private List<String> recommendReasons;
    private Instant generatedAt;

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public RecommendScore getScore() { return score; }
    public void setScore(RecommendScore score) { this.score = score; }
    public List<String> getRecommendReasons() { return recommendReasons; }
    public void setRecommendReasons(List<String> recommendReasons) { this.recommendReasons = recommendReasons; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
}
