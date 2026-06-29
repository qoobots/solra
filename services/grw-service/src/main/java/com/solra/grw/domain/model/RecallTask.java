package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * RecallTask — 召回任务实体。
 * 记录一次针对特定用户的召回推送任务。
 */
public class RecallTask {

    private String taskId;
    private String userId;
    private String strategyId;
    private String strategyName;
    private ChurnRiskLevel riskLevel;
    private int inactiveDays;
    private RecallChannel channel;
    private RecallTaskStatus status;
    private String title;
    private String message;
    private int attemptNumber;
    private Instant createdAt;
    private Instant sentAt;
    private Instant clickedAt;
    private Instant convertedAt;

    public RecallTask() {}

    public RecallTask(String taskId, String userId, String strategyId, String strategyName,
                      ChurnRiskLevel riskLevel, int inactiveDays, RecallChannel channel,
                      String title, String message, int attemptNumber) {
        this.taskId = taskId;
        this.userId = userId;
        this.strategyId = strategyId;
        this.strategyName = strategyName;
        this.riskLevel = riskLevel;
        this.inactiveDays = inactiveDays;
        this.channel = channel;
        this.status = RecallTaskStatus.PENDING;
        this.title = title;
        this.message = message;
        this.attemptNumber = attemptNumber;
        this.createdAt = Instant.now();
    }

    /** 标记为已发送 */
    public void markSent() {
        this.status = RecallTaskStatus.SENT;
        this.sentAt = Instant.now();
    }

    /** 标记为用户已点击 */
    public void markClicked() {
        this.status = RecallTaskStatus.CLICKED;
        this.clickedAt = Instant.now();
    }

    /** 标记为已转化（用户重新活跃） */
    public void markConverted() {
        this.status = RecallTaskStatus.CONVERTED;
        this.convertedAt = Instant.now();
    }

    /** 标记为已过期 */
    public void markExpired() {
        this.status = RecallTaskStatus.EXPIRED;
    }

    /** 取消任务 */
    public void cancel() {
        this.status = RecallTaskStatus.CANCELLED;
    }

    /** 判断是否已完成（成功或终止） */
    public boolean isTerminal() {
        return status == RecallTaskStatus.CONVERTED
                || status == RecallTaskStatus.EXPIRED
                || status == RecallTaskStatus.CANCELLED;
    }

    // ---- getters/setters ----

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }
    public ChurnRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ChurnRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public int getInactiveDays() { return inactiveDays; }
    public void setInactiveDays(int inactiveDays) { this.inactiveDays = inactiveDays; }
    public RecallChannel getChannel() { return channel; }
    public void setChannel(RecallChannel channel) { this.channel = channel; }
    public RecallTaskStatus getStatus() { return status; }
    public void setStatus(RecallTaskStatus status) { this.status = status; }
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
