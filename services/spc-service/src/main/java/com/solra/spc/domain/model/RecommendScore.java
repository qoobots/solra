package com.solra.spc.domain.model;

/**
 * RecommendScore 值对象 — 推荐评分（相关性/热门度/新鲜度/综合）。
 */
public class RecommendScore {
    private float relevance;
    private float popularity;
    private float freshness;
    private float overall;

    public RecommendScore() {}

    public RecommendScore(float relevance, float popularity, float freshness) {
        this.relevance = relevance;
        this.popularity = popularity;
        this.freshness = freshness;
        this.overall = 0.4f * relevance + 0.35f * popularity + 0.25f * freshness;
    }

    public float getRelevance() { return relevance; }
    public void setRelevance(float relevance) { this.relevance = relevance; }
    public float getPopularity() { return popularity; }
    public void setPopularity(float popularity) { this.popularity = popularity; }
    public float getFreshness() { return freshness; }
    public void setFreshness(float freshness) { this.freshness = freshness; }
    public float getOverall() { return overall; }
    public void setOverall(float overall) { this.overall = overall; }
}
