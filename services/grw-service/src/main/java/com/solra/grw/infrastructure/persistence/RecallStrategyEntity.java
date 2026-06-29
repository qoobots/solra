package com.solra.grw.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recall_strategies")
public class RecallStrategyEntity {
    @Id
    @Column(name = "strategy_id", length = 64)
    private String strategyId;
    @Column(name = "name", length = 128, nullable = false)
    private String name;
    @Column(name = "target_risk_level", length = 20, nullable = false)
    private String targetRiskLevel;
    @Column(name = "inactive_days_min")
    private int inactiveDaysMin;
    @Column(name = "inactive_days_max")
    private int inactiveDaysMax;
    @Column(name = "message_template", length = 1024)
    private String messageTemplate;
    @Column(name = "title_template", length = 256)
    private String titleTemplate;
    @Column(name = "channels", length = 256)
    private String channels;
    @Column(name = "max_attempts")
    private int maxAttempts;
    @Column(name = "cooldown_hours")
    private int cooldownHours;
    @Column(name = "active")
    private boolean active;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;

    public RecallStrategyEntity() {}

    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTargetRiskLevel() { return targetRiskLevel; }
    public void setTargetRiskLevel(String targetRiskLevel) { this.targetRiskLevel = targetRiskLevel; }
    public int getInactiveDaysMin() { return inactiveDaysMin; }
    public void setInactiveDaysMin(int inactiveDaysMin) { this.inactiveDaysMin = inactiveDaysMin; }
    public int getInactiveDaysMax() { return inactiveDaysMax; }
    public void setInactiveDaysMax(int inactiveDaysMax) { this.inactiveDaysMax = inactiveDaysMax; }
    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getCooldownHours() { return cooldownHours; }
    public void setCooldownHours(int cooldownHours) { this.cooldownHours = cooldownHours; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
