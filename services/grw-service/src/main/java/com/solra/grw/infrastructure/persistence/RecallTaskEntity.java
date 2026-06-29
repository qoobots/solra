package com.solra.grw.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recall_tasks")
public class RecallTaskEntity {
    @Id
    @Column(name = "task_id", length = 64)
    private String taskId;
    @Column(name = "user_id", length = 64, nullable = false)
    private String userId;
    @Column(name = "strategy_id", length = 64)
    private String strategyId;
    @Column(name = "strategy_name", length = 128)
    private String strategyName;
    @Column(name = "risk_level", length = 20)
    private String riskLevel;
    @Column(name = "inactive_days")
    private int inactiveDays;
    @Column(name = "channel", length = 20)
    private String channel;
    @Column(name = "status", length = 20)
    private String status;
    @Column(name = "title", length = 256)
    private String title;
    @Column(name = "message", length = 1024)
    private String message;
    @Column(name = "attempt_number")
    private int attemptNumber;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "sent_at")
    private Instant sentAt;
    @Column(name = "clicked_at")
    private Instant clickedAt;
    @Column(name = "converted_at")
    private Instant convertedAt;

    public RecallTaskEntity() {}

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public int getInactiveDays() { return inactiveDays; }
    public void setInactiveDays(int inactiveDays) { this.inactiveDays = inactiveDays; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getClickedAt() { return clickedAt; }
    public void setClickedAt(Instant clickedAt) { this.clickedAt = clickedAt; }
    public Instant getConvertedAt() { return convertedAt; }
    public void setConvertedAt(Instant convertedAt) { this.convertedAt = convertedAt; }
}
