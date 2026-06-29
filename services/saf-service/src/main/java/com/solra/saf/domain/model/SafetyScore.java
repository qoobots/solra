package com.solra.saf.domain.model;

import java.util.List;

/**
 * Value object — safety score for a piece of content.
 */
public class SafetyScore {

    private float overallScore;
    private List<CategoryScore> categoryScores;
    private String modelVersion;

    private SafetyScore() {}

    public static SafetyScore safe(String modelVersion) {
        SafetyScore s = new SafetyScore();
        s.overallScore = 1.0f;
        s.categoryScores = List.of();
        s.modelVersion = modelVersion;
        return s;
    }

    public static SafetyScore unsafe(float score, List<CategoryScore> scores, String modelVersion) {
        SafetyScore s = new SafetyScore();
        s.overallScore = Math.max(0, Math.min(1, score));
        s.categoryScores = scores;
        s.modelVersion = modelVersion;
        return s;
    }

    public boolean isSafe(float threshold) {
        return overallScore >= threshold;
    }

    public float getOverallScore() { return overallScore; }
    public List<CategoryScore> getCategoryScores() { return categoryScores; }
    public String getModelVersion() { return modelVersion; }

    public record CategoryScore(String category, float score) {}
}
