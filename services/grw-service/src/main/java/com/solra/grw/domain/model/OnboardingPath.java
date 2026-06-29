package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * OnboardingPath 实体 — 新用户引导路径。
 * 管理用户从注册到完成引导的完整流程，包括步骤记录和状态跟踪。
 */
public class OnboardingPath {

    private String pathId;
    private String userId;
    private int currentStep;
    private int totalSteps;
    private List<OnboardingStep> stepHistory;
    private Instant startTime;
    private Instant completedAt;
    private OnboardingStatus status;

    // ---- constructors ----
    public OnboardingPath() {
        this.stepHistory = new ArrayList<>();
    }

    public OnboardingPath(String pathId, String userId, int totalSteps) {
        this();
        this.pathId = pathId;
        this.userId = userId;
        this.totalSteps = totalSteps;
        this.currentStep = 0;
        this.startTime = Instant.now();
        this.status = OnboardingStatus.NOT_STARTED;
    }

    // ---- business methods ----

    /**
     * 开始引导流程。
     */
    public void start() {
        this.status = OnboardingStatus.IN_PROGRESS;
        this.currentStep = 1;
    }

    /**
     * 记录一个引导步骤完成。
     *
     * @param step 完成的步骤
     */
    public void recordStep(OnboardingStep step) {
        this.stepHistory.add(step);
        if (step.getCompletedAt() != null) {
            this.currentStep = step.getStepNumber() + 1;
            if (this.currentStep > this.totalSteps) {
                complete();
            }
        }
    }

    /**
     * 完成引导流程。
     */
    public void complete() {
        this.status = OnboardingStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    /**
     * 放弃引导流程。
     */
    public void abandon() {
        this.status = OnboardingStatus.ABANDONED;
    }

    /**
     * 获取当前进度百分比。
     */
    public double getProgressPercentage() {
        if (totalSteps == 0) return 0.0;
        return (double) stepHistory.stream().filter(s -> s.getCompletedAt() != null).count() / totalSteps * 100.0;
    }

    /**
     * 判断引导是否已完成。
     */
    public boolean isCompleted() {
        return status == OnboardingStatus.COMPLETED;
    }

    // ---- getters / setters ----

    public String getPathId() { return pathId; }
    public void setPathId(String pathId) { this.pathId = pathId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public List<OnboardingStep> getStepHistory() { return stepHistory; }
    public void setStepHistory(List<OnboardingStep> stepHistory) { this.stepHistory = stepHistory; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public OnboardingStatus getStatus() { return status; }
    public void setStatus(OnboardingStatus status) { this.status = status; }
}
