package com.solra.common.cost;

import java.time.Instant;

/**
 * 预算告警值对象。
 * 当成本超过预算阈值时生成告警。
 */
public class BudgetAlert {

    public enum Severity { INFO, WARNING, CRITICAL }

    private final String alertId;
    private final String budgetName;
    private final int triggeredThreshold;  // 触发告警的阈值百分比
    private final double currentCost;      // 当前花费（元）
    private final double budgetAmount;     // 预算总额（元）
    private final double consumptionPercent; // 消耗百分比
    private final Severity severity;
    private final Instant alertTime;
    private final String message;

    public BudgetAlert(String alertId, String budgetName, int triggeredThreshold,
                       double currentCost, double budgetAmount, Severity severity,
                       Instant alertTime, String message) {
        this.alertId = alertId;
        this.budgetName = budgetName;
        this.triggeredThreshold = triggeredThreshold;
        this.currentCost = currentCost;
        this.budgetAmount = budgetAmount;
        this.consumptionPercent = budgetAmount > 0 ? (currentCost / budgetAmount) * 100.0 : 0;
        this.severity = severity;
        this.alertTime = alertTime != null ? alertTime : Instant.now();
        this.message = message;
    }

    // ---- Getters ----
    public String getAlertId() { return alertId; }
    public String getBudgetName() { return budgetName; }
    public int getTriggeredThreshold() { return triggeredThreshold; }
    public double getCurrentCost() { return currentCost; }
    public double getBudgetAmount() { return budgetAmount; }
    public double getConsumptionPercent() { return consumptionPercent; }
    public Severity getSeverity() { return severity; }
    public Instant getAlertTime() { return alertTime; }
    public String getMessage() { return message; }

    /** 根据阈值百分比判断严重级别 */
    public static Severity severityFromThreshold(int threshold) {
        if (threshold >= 100) return Severity.CRITICAL;
        if (threshold >= 90) return Severity.CRITICAL;
        if (threshold >= 75) return Severity.WARNING;
        return Severity.INFO;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: 消耗 %.1f%% (¥%.2f/¥%.2f) - %s",
                severity, budgetName, consumptionPercent, currentCost, budgetAmount, message);
    }
}
