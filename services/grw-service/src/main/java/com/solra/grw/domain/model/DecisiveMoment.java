package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * DecisiveMoment 实体 — 决定性时刻。
 * 记录用户旅程中的关键转化节点，从临时访客向活跃用户的不可逆转变。
 */
public class DecisiveMoment {

    private String momentId;
    private String userId;
    private DecisiveMomentType momentType;
    private Instant detectedAt;
    private Map<String, Object> userStateBefore;
    private Map<String, Object> userStateAfter;
    private double conversionValue;
    private boolean triggered;

    // ---- constructors ----
    public DecisiveMoment() {
        this.userStateBefore = new HashMap<>();
        this.userStateAfter = new HashMap<>();
    }

    public DecisiveMoment(String momentId, String userId, DecisiveMomentType momentType) {
        this();
        this.momentId = momentId;
        this.userId = userId;
        this.momentType = momentType;
        this.detectedAt = Instant.now();
        this.conversionValue = 0.0;
        this.triggered = false;
    }

    // ---- business methods ----

    /**
     * 触发该决定性时刻，记录触发时间和转化价值。
     *
     * @param conversionValue 转化价值 (0.0-1.0)
     */
    public void trigger(double conversionValue) {
        this.triggered = true;
        this.conversionValue = Math.min(1.0, Math.max(0.0, conversionValue));
        this.detectedAt = Instant.now();
    }

    /**
     * 设置转化前后的用户状态快照。
     */
    public void setStateSnapshot(Map<String, Object> before, Map<String, Object> after) {
        this.userStateBefore = before != null ? new HashMap<>(before) : new HashMap<>();
        this.userStateAfter = after != null ? new HashMap<>(after) : new HashMap<>();
    }

    /**
     * 判断该时刻是否已被触发。
     */
    public boolean isTriggered() {
        return triggered;
    }

    // ---- getters / setters ----

    public String getMomentId() { return momentId; }
    public void setMomentId(String momentId) { this.momentId = momentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public DecisiveMomentType getMomentType() { return momentType; }
    public void setMomentType(DecisiveMomentType momentType) { this.momentType = momentType; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }

    public Map<String, Object> getUserStateBefore() { return userStateBefore; }
    public void setUserStateBefore(Map<String, Object> userStateBefore) { this.userStateBefore = userStateBefore; }

    public Map<String, Object> getUserStateAfter() { return userStateAfter; }
    public void setUserStateAfter(Map<String, Object> userStateAfter) { this.userStateAfter = userStateAfter; }

    public double getConversionValue() { return conversionValue; }
    public void setConversionValue(double conversionValue) { this.conversionValue = conversionValue; }

    public boolean getTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
}
