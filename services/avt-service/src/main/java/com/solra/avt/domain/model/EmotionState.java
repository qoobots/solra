package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * EmotionState 值对象 — PAD三维情感模型 (Pleasure-Arousal-Dominance)。
 * 支持离散情感类别 + 连续维度。
 */
public class EmotionState {

    private EmotionCategory primaryEmotion = EmotionCategory.NEUTRAL;
    private float intensity = 0.5f;
    private List<EmotionDimension> dimensions;
    private Instant detectedAt;

    public EmotionState() {
        this.detectedAt = Instant.now();
    }

    public EmotionState(EmotionCategory primaryEmotion, float intensity) {
        this.primaryEmotion = primaryEmotion;
        this.intensity = clamp(intensity, 0f, 1f);
        this.detectedAt = Instant.now();
    }

    /** 根据文本情感分析结果更新 */
    public void updateFromText(String sentiment, float confidence) {
        this.primaryEmotion = mapSentiment(sentiment);
        this.intensity = clamp(confidence, 0f, 1f);
        this.detectedAt = Instant.now();
    }

    public boolean isIntense() { return intensity >= 0.7f; }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private EmotionCategory mapSentiment(String sentiment) {
        if (sentiment == null) return EmotionCategory.NEUTRAL;
        return switch (sentiment.toLowerCase()) {
            case "joy", "happy", "positive" -> EmotionCategory.JOY;
            case "sadness", "sad" -> EmotionCategory.SADNESS;
            case "anger", "angry" -> EmotionCategory.ANGER;
            case "fear", "scared" -> EmotionCategory.FEAR;
            case "surprise" -> EmotionCategory.SURPRISE;
            case "disgust" -> EmotionCategory.DISGUST;
            case "trust" -> EmotionCategory.TRUST;
            case "anticipation" -> EmotionCategory.ANTICIPATION;
            default -> EmotionCategory.NEUTRAL;
        };
    }

    // ---- getters / setters ----
    public EmotionCategory getPrimaryEmotion() { return primaryEmotion; }
    public void setPrimaryEmotion(EmotionCategory primaryEmotion) { this.primaryEmotion = primaryEmotion; }
    public float getIntensity() { return intensity; }
    public void setIntensity(float intensity) { this.intensity = clamp(intensity, 0f, 1f); }
    public List<EmotionDimension> getDimensions() { return dimensions; }
    public void setDimensions(List<EmotionDimension> dimensions) { this.dimensions = dimensions; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
}
