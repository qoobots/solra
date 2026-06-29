package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * RecallStrategy — 召回策略聚合根。
 * 定义针对特定流失风险等级用户的召回策略模板。
 */
public class RecallStrategy {

    private String strategyId;
    private String name;
    private ChurnRiskLevel targetRiskLevel;
    private int inactiveDaysMin;
    private int inactiveDaysMax;
    private String messageTemplate;
    private String titleTemplate;
    private List<RecallChannel> channels;
    private int maxAttempts;
    private int cooldownHours;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public RecallStrategy() {}

    public RecallStrategy(String strategyId, String name, ChurnRiskLevel targetRiskLevel,
                          int inactiveDaysMin, int inactiveDaysMax,
                          String titleTemplate, String messageTemplate,
                          List<RecallChannel> channels) {
        this.strategyId = strategyId;
        this.name = name;
        this.targetRiskLevel = targetRiskLevel;
        this.inactiveDaysMin = inactiveDaysMin;
        this.inactiveDaysMax = inactiveDaysMax;
        this.titleTemplate = titleTemplate;
        this.messageTemplate = messageTemplate;
        this.channels = channels;
        this.maxAttempts = 3;
        this.cooldownHours = 72;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 判断该策略是否适用于给定的未活跃天数 */
    public boolean appliesTo(int inactiveDays) {
        return active && inactiveDays >= inactiveDaysMin && inactiveDays <= inactiveDaysMax;
    }

    /** 启用策略 */
    public void activate() { this.active = true; this.updatedAt = Instant.now(); }

    /** 停用策略 */
    public void deactivate() { this.active = false; this.updatedAt = Instant.now(); }

    /** 更新冷却时间 */
    public void updateCooldown(int hours) {
        this.cooldownHours = hours;
        this.updatedAt = Instant.now();
    }

    /** 更新最大尝试次数 */
    public void updateMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.updatedAt = Instant.now();
    }

    // ---- getters/setters ----

    public String getStrategyId() { return strategyId; }
    public void setStrategyId(String strategyId) { this.strategyId = strategyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ChurnRiskLevel getTargetRiskLevel() { return targetRiskLevel; }
    public void setTargetRiskLevel(ChurnRiskLevel targetRiskLevel) { this.targetRiskLevel = targetRiskLevel; }
    public int getInactiveDaysMin() { return inactiveDaysMin; }
    public void setInactiveDaysMin(int inactiveDaysMin) { this.inactiveDaysMin = inactiveDaysMin; }
    public int getInactiveDaysMax() { return inactiveDaysMax; }
    public void setInactiveDaysMax(int inactiveDaysMax) { this.inactiveDaysMax = inactiveDaysMax; }
    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }
    public List<RecallChannel> getChannels() { return channels; }
    public void setChannels(List<RecallChannel> channels) { this.channels = channels; }
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
