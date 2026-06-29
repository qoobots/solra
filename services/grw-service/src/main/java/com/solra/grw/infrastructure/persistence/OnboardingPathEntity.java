package com.solra.grw.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "onboarding_paths")
public class OnboardingPathEntity {
    @Id @Column(name = "path_id", length = 64)
    private String pathId;
    @Column(name = "user_id", length = 64, unique = true)
    private String userId;
    @Column(name = "current_step")
    private int currentStep;
    @Column(name = "total_steps")
    private int totalSteps;
    @Column(name = "step_history", columnDefinition = "TEXT")
    private String stepHistory; // JSON
    @Column(name = "start_time")
    private Instant startTime;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "status", length = 20)
    private String status;

    public OnboardingPathEntity() {}

    public String getPathId() { return pathId; }
    public void setPathId(String pathId) { this.pathId = pathId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getCurrentStep() { return currentStep; }
    public void setCurrentStep(int currentStep) { this.currentStep = currentStep; }
    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
    public String getStepHistory() { return stepHistory; }
    public void setStepHistory(String stepHistory) { this.stepHistory = stepHistory; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
