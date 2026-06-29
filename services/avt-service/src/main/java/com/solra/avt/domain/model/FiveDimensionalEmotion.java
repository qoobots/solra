package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * FiveDimensionalEmotion — AVT-004 虚拟人5维情感模型。
 *
 * 5个维度：Joy(开心) / Curiosity(好奇) / Coldness(冷淡) / Jealousy(吃醋) / Sadness(失落)
 * 每个维度 0.0 ~ 1.0，总和不一定为1（允许复杂混合情感）。
 *
 * 支持情感衰减(decay)、事件驱动变化和状态机转移。
 */
public class FiveDimensionalEmotion {

    private float joy = 0.5f;
    private float curiosity = 0.5f;
    private float coldness = 0.1f;
    private float jealousy = 0.0f;
    private float sadness = 0.1f;
    private Instant lastUpdated;
    private String dominantDimension;    // 当前主导情感维度
    private String currentMood;          // 可解释的情绪状态描述

    public FiveDimensionalEmotion() {
        this.lastUpdated = Instant.now();
        this.currentMood = "neutral";
        recomputeMood();
    }

    /**
     * Apply an emotional event — shifts the 5D vector based on event type and intensity.
     */
    public void applyEvent(EmotionEventType eventType, float intensity) {
        float clampedIntensity = Math.max(0f, Math.min(1f, intensity));
        switch (eventType) {
            case USER_COMPLIMENT -> { joy += 0.2f * clampedIntensity; curiosity += 0.1f * clampedIntensity; sadness = Math.max(0, sadness - 0.1f); }
            case USER_INSULT -> { sadness += 0.3f * clampedIntensity; coldness += 0.15f * clampedIntensity; joy = Math.max(0, joy - 0.1f); }
            case USER_IGNORED -> { coldness += 0.2f * clampedIntensity; sadness += 0.15f * clampedIntensity; }
            case USER_RETURNED -> { joy += 0.3f * clampedIntensity; curiosity += 0.2f * clampedIntensity; coldness = Math.max(0, coldness - 0.2f); }
            case USER_SHARED_SECRET -> { curiosity += 0.25f * clampedIntensity; joy += 0.15f * clampedIntensity; }
            case USER_TALKED_TO_OTHER -> { jealousy += 0.25f * clampedIntensity; sadness += 0.1f * clampedIntensity; }
            case USER_GAVE_GIFT -> { joy += 0.35f * clampedIntensity; jealousy = Math.max(0, jealousy - 0.1f); }
            case TIME_PASSED -> decay(0.05f * clampedIntensity); // natural decay
            case SURPRISE_EVENT -> { curiosity += 0.4f * clampedIntensity; joy += 0.2f * clampedIntensity; }
        }
        normalize();
        recomputeMood();
        this.lastUpdated = Instant.now();
    }

    /**
     * Decay all emotions towards neutral over time (called periodically).
     */
    public void decay(float rate) {
        joy = lerp(joy, 0.5f, rate);
        curiosity = lerp(curiosity, 0.5f, rate);
        coldness = Math.max(0, coldness - rate);
        jealousy = Math.max(0, jealousy - rate * 0.5f);
        sadness = Math.max(0, sadness - rate);
        recomputeMood();
        this.lastUpdated = Instant.now();
    }

    /**
     * Get the dominant emotional dimension name.
     */
    public String getDominantDimension() {
        recomputeMood();
        return dominantDimension;
    }

    /**
     * Get a human-readable mood description.
     */
    public String getCurrentMood() {
        return currentMood;
    }

    /**
     * Check if a specific emotion is dominant.
     */
    public boolean isDominatedBy(String dimension) {
        return dimension.equals(dominantDimension);
    }

    /**
     * Get overall emotional intensity (max of all dimensions).
     */
    public float getOverallIntensity() {
        return Math.max(joy, Math.max(curiosity,
                Math.max(coldness, Math.max(jealousy, sadness))));
    }

    private void normalize() {
        joy = clamp(joy);
        curiosity = clamp(curiosity);
        coldness = clamp(coldness);
        jealousy = clamp(jealousy);
        sadness = clamp(sadness);
    }

    private void recomputeMood() {
        // Find dominant dimension
        Map<String, Float> dims = new HashMap<>();
        dims.put("joy", joy);
        dims.put("curiosity", curiosity);
        dims.put("coldness", coldness);
        dims.put("jealousy", jealousy);
        dims.put("sadness", sadness);

        dominantDimension = dims.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");

        // Map to human-readable mood
        if (joy >= 0.7f) currentMood = "cheerful";
        else if (joy >= 0.5f) currentMood = "pleasant";
        else if (curiosity >= 0.7f) currentMood = "intrigued";
        else if (curiosity >= 0.5f) currentMood = "curious";
        else if (coldness >= 0.6f) currentMood = "aloof";
        else if (coldness >= 0.4f) currentMood = "distant";
        else if (jealousy >= 0.6f) currentMood = "jealous";
        else if (jealousy >= 0.3f) currentMood = "slightly_envious";
        else if (sadness >= 0.6f) currentMood = "melancholy";
        else if (sadness >= 0.4f) currentMood = "downcast";
        else currentMood = "neutral";
    }

    private float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // -- Getters --
    public float getJoy() { return joy; }
    public float getCuriosity() { return curiosity; }
    public float getColdness() { return coldness; }
    public float getJealousy() { return jealousy; }
    public float getSadness() { return sadness; }
    public Instant getLastUpdated() { return lastUpdated; }

    /**
     * Emotion event types for the state machine.
     */
    public enum EmotionEventType {
        USER_COMPLIMENT,
        USER_INSULT,
        USER_IGNORED,
        USER_RETURNED,
        USER_SHARED_SECRET,
        USER_TALKED_TO_OTHER,
        USER_GAVE_GIFT,
        TIME_PASSED,
        SURPRISE_EVENT
    }
}
