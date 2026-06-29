package com.solra.common.cost;

import java.time.Instant;
import java.util.*;

/**
 * 成本报告聚合根。
 * 支持日/周/月维度的成本汇总报告。
 */
public class CostReport {

    public enum ReportPeriod { DAILY, WEEKLY, MONTHLY }

    private final String reportId;
    private final ReportPeriod period;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final Instant generatedAt;

    // 成本数据
    private double totalCost;                          // 总成本（分）
    private final Map<CostResourceType, Double> costByType;  // 按资源类型
    private final Map<String, Double> costByFunction;  // 按功能
    private final Map<String, Double> costByUser;      // 按用户（topN）
    private final Map<String, Double> costBySpace;     // 按空间（topN）

    // 对比数据
    private double previousPeriodCost;                 // 上期成本
    private double costChangePercent;                  // 环比变化%

    // FinOps 建议
    private final List<FinOpsRecommendation> recommendations;

    public CostReport(String reportId, ReportPeriod period,
                      Instant periodStart, Instant periodEnd) {
        this.reportId = reportId;
        this.period = period;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.generatedAt = Instant.now();
        this.costByType = new LinkedHashMap<>();
        this.costByFunction = new LinkedHashMap<>();
        this.costByUser = new LinkedHashMap<>();
        this.costBySpace = new LinkedHashMap<>();
        this.recommendations = new ArrayList<>();
    }

    // ---- 数据聚合 ----

    public void addEntry(CostEntry entry) {
        totalCost += entry.getAmount();
        costByType.merge(entry.getResourceType(), entry.getAmount(), Double::sum);
        costByFunction.merge(entry.getDimensionValue(CostDimension.FUNCTION), entry.getAmount(), Double::sum);
        costByUser.merge(entry.getDimensionValue(CostDimension.USER), entry.getAmount(), Double::sum);
        costBySpace.merge(entry.getDimensionValue(CostDimension.SPACE), entry.getAmount(), Double::sum);
    }

    public void setPreviousPeriodCost(double cost) {
        this.previousPeriodCost = cost;
        if (previousPeriodCost > 0) {
            this.costChangePercent = ((totalCost - previousPeriodCost) / previousPeriodCost) * 100.0;
        }
    }

    public void addRecommendation(FinOpsRecommendation rec) {
        recommendations.add(rec);
    }

    // ---- 查询方法 ----

    /** 获取按资源类型降序排列的成本分布 */
    public List<Map.Entry<CostResourceType, Double>> getTopCostTypes(int topN) {
        return costByType.entrySet().stream()
                .sorted(Map.Entry.<CostResourceType, Double>comparingByValue().reversed())
                .limit(topN)
                .toList();
    }

    /** 获取按功能降序排列的成本分布 */
    public List<Map.Entry<String, Double>> getTopFunctions(int topN) {
        return costByFunction.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .toList();
    }

    /** 获取按用户降序排列的成本分布 */
    public List<Map.Entry<String, Double>> getTopUsers(int topN) {
        return costByUser.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .toList();
    }

    /** 获取按空间降序排列的成本分布 */
    public List<Map.Entry<String, Double>> getTopSpaces(int topN) {
        return costBySpace.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .toList();
    }

    // ---- Getters ----
    public String getReportId() { return reportId; }
    public ReportPeriod getPeriod() { return period; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public Instant getGeneratedAt() { return generatedAt; }
    public double getTotalCost() { return totalCost; }
    public double getTotalCostYuan() { return totalCost / 100.0; }
    public Map<CostResourceType, Double> getCostByType() { return Collections.unmodifiableMap(costByType); }
    public Map<String, Double> getCostByFunction() { return Collections.unmodifiableMap(costByFunction); }
    public Map<String, Double> getCostByUser() { return Collections.unmodifiableMap(costByUser); }
    public Map<String, Double> getCostBySpace() { return Collections.unmodifiableMap(costBySpace); }
    public double getPreviousPeriodCost() { return previousPeriodCost; }
    public double getCostChangePercent() { return costChangePercent; }
    public List<FinOpsRecommendation> getRecommendations() { return Collections.unmodifiableList(recommendations); }

    /**
     * FinOps 优化建议。
     */
    public record FinOpsRecommendation(
            String id,
            String title,
            String description,
            double estimatedMonthlySavings,  // 预估月节省（元）
            Priority priority,
            String category  // right_sizing, spot_instance, lifecycle, caching, etc.
    ) {
        public enum Priority { HIGH, MEDIUM, LOW }
    }
}
