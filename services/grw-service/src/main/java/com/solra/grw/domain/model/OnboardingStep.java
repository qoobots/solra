package com.solra.grw.domain.model;

import java.time.Instant;

/**
 * OnboardingStep — 引导步骤值对象。
 */
public class OnboardingStep {

    private int stepNumber;
    private OnboardingStepType stepType;
    private Instant completedAt;
    private Instant skippedAt;
    private String metadata;

    public OnboardingStep() {}

    public OnboardingStep(int stepNumber, OnboardingStepType stepType) {
        this.stepNumber = stepNumber;
        this.stepType = stepType;
    }

    public void complete() { this.completedAt = Instant.now(); }
    public void skip() { this.skippedAt = Instant.now(); }
    public boolean isCompleted() { return completedAt != null; }
    public boolean isSkipped() { return skippedAt != null; }

    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }
    public OnboardingStepType getStepType() { return stepType; }
    public void setStepType(OnboardingStepType stepType) { this.stepType = stepType; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getSkippedAt() { return skippedAt; }
    public void setSkippedAt(Instant skippedAt) { this.skippedAt = skippedAt; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
