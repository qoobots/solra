package com.solra.common.cost;

import java.util.List;

/**
 * 预算配置值对象。
 * 对应 infra/finops/cost-management.yaml 中的预算配置。
 */
public class Budget {

    private final String name;
    private final double amount;              // 预算总额（元）
    private final String currency;
    private final CostResourceType resourceType; // null 表示总预算
    private final List<Integer> thresholdPercentages; // [50, 75, 90, 100]
    private final List<String> alertChannels;  // email, slack, pagerduty

    public Budget(String name, double amount, String currency,
                  CostResourceType resourceType, List<Integer> thresholdPercentages,
                  List<String> alertChannels) {
        this.name = name;
        this.amount = amount;
        this.currency = currency != null ? currency : "CNY";
        this.resourceType = resourceType;
        this.thresholdPercentages = thresholdPercentages != null
                ? List.copyOf(thresholdPercentages) : List.of(50, 75, 90, 100);
        this.alertChannels = alertChannels != null
                ? List.copyOf(alertChannels) : List.of("email");
    }

    // ---- Getters ----
    public String getName() { return name; }
    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public CostResourceType getResourceType() { return resourceType; }
    public List<Integer> getThresholdPercentages() { return thresholdPercentages; }
    public List<String> getAlertChannels() { return alertChannels; }

    /** 计算当前花费占预算的百分比 */
    public double getConsumptionPercent(double currentCost) {
        if (amount <= 0) return 0;
        return (currentCost / amount) * 100.0;
    }

    /** 判断当前花费是否超过某阈值 */
    public boolean isOverThreshold(double currentCost, int thresholdPercent) {
        return getConsumptionPercent(currentCost) >= thresholdPercent;
    }

    /** 获取当前花费触发的最高告警级别 */
    public int getHighestTriggeredThreshold(double currentCost) {
        return thresholdPercentages.stream()
                .filter(t -> isOverThreshold(currentCost, t))
                .max(Integer::compareTo)
                .orElse(0);
    }

    /** 是否为总预算 */
    public boolean isTotalBudget() { return resourceType == null; }

    /** 剩余预算 */
    public double getRemaining(double currentCost) {
        return Math.max(0, amount - currentCost);
    }
}
