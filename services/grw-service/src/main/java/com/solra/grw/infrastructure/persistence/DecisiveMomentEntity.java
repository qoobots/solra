package com.solra.grw.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "decisive_moments")
public class DecisiveMomentEntity {
    @Id @Column(name = "moment_id", length = 64)
    private String momentId;
    @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "moment_type", length = 50)
    private String momentType;
    @Column(name = "detected_at")
    private Instant detectedAt;
    @Column(name = "state_before", columnDefinition = "TEXT")
    private String stateBefore;
    @Column(name = "state_after", columnDefinition = "TEXT")
    private String stateAfter;
    @Column(name = "conversion_value")
    private double conversionValue;
    @Column(name = "triggered")
    private boolean triggered;

    public DecisiveMomentEntity() {}

    public String getMomentId() { return momentId; }
    public void setMomentId(String momentId) { this.momentId = momentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getMomentType() { return momentType; }
    public void setMomentType(String momentType) { this.momentType = momentType; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public String getStateBefore() { return stateBefore; }
    public void setStateBefore(String stateBefore) { this.stateBefore = stateBefore; }
    public String getStateAfter() { return stateAfter; }
    public void setStateAfter(String stateAfter) { this.stateAfter = stateAfter; }
    public double getConversionValue() { return conversionValue; }
    public void setConversionValue(double conversionValue) { this.conversionValue = conversionValue; }
    public boolean isTriggered() { return triggered; }
    public void setTriggered(boolean triggered) { this.triggered = triggered; }
}
