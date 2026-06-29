package com.solra.avt.domain.model;

/**
 * EmotionDimension 值对象 — PAD二维单维度（Pleasure / Arousal / Dominance）。
 */
public class EmotionDimension {
    private String dimensionName;
    private float value; // -1.0 .. 1.0

    public EmotionDimension() {}

    public EmotionDimension(String dimensionName, float value) {
        this.dimensionName = dimensionName;
        this.value = Math.max(-1f, Math.min(1f, value));
    }

    public String getDimensionName() { return dimensionName; }
    public void setDimensionName(String dimensionName) { this.dimensionName = dimensionName; }
    public float getValue() { return value; }
    public void setValue(float value) { this.value = Math.max(-1f, Math.min(1f, value)); }
}
