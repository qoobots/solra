package com.solra.common.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CostManagementService — 成本核算与FinOps服务。
 * 提供多云成本归因、预算管理、成本报告生成和优化建议。
 * INF-009: 成本核算与FinOps系统。
 *
 * 核心能力：
 * 1. 多维度成本归因（功能/用户/空间/环境/资源类型）
 * 2. 预算管理（总预算 + 分模块预算 + 阈值告警）
 * 3. 成本报告（日/周/月 + 环比分析）
 * 4. FinOps 建议（Right-sizing / Spot实例 / 生命周期 / 缓存优化）
 */
@Service
public class CostManagementService {

    private static final Logger log = LoggerFactory.getLogger(CostManagementService.class);

    // ---- 成本数据存储（内存） ----
    private final List<CostEntry> costEntries = new CopyOnWriteArrayList<>();

    // ---- 预算配置 ----
    private final Map<String, Budget> budgets = new ConcurrentHashMap<>();

    // ---- 成本报告缓存 ----
    private final Map<String, CostReport> reportCache = new ConcurrentHashMap<>();

    // ==================== 初始化 ====================

    public CostManagementService() {
        initDefaultBudgets();
        log.info("CostManagementService initialized with {} budgets", budgets.size());
    }

    private void initDefaultBudgets() {
        // 总预算：¥50,000/月（原型阶段）
        budgets.put("solra-monthly-total", new Budget(
                "solra-monthly-total", 50000, "CNY", null,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));

        // 分模块预算（对应 infra/finops/cost-management.yaml）
        budgets.put("solra-compute-budget", new Budget(
                "solra-compute-budget", 20000, "CNY",
                CostResourceType.COMPUTE,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));

        budgets.put("solra-storage-budget", new Budget(
                "solra-storage-budget", 10000, "CNY",
                CostResourceType.STORAGE,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));

        budgets.put("solra-network-budget", new Budget(
                "solra-network-budget", 5000, "CNY",
                CostResourceType.NETWORK,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));

        budgets.put("solra-api-budget", new Budget(
                "solra-api-budget", 5000, "CNY",
                CostResourceType.API,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));

        budgets.put("solra-observability-budget", new Budget(
                "solra-observability-budget", 5000, "CNY",
                CostResourceType.OBSERVABILITY,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));

        budgets.put("solra-misc-budget", new Budget(
                "solra-misc-budget", 5000, "CNY",
                CostResourceType.MISCELLANEOUS,
                List.of(50, 75, 90, 100),
                List.of("email", "slack")));
    }

    // ==================== 成本记录 ====================

    /**
     * 记录一条成本数据。
     *
     * @param entryId      成本条目ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param amount       金额（分）
     * @param dimensions   归因维度
     * @param description  描述
     * @return 创建的 CostEntry
     */
    public CostEntry recordCost(String entryId, CostResourceType resourceType,
                                 String resourceId, double amount,
                                 Map<CostDimension, String> dimensions,
                                 String description) {
        CostEntry entry = CostEntry.builder()
                .entryId(entryId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .amount(amount)
                .dimensions(dimensions != null ? dimensions : Map.of())
                .description(description != null ? description : "")
                .build();

        costEntries.add(entry);
        log.debug("Cost recorded: {} type={} amount=¥{}/{}",
                entryId, resourceType, entry.getAmountYuan(), entry.getCurrency());
        return entry;
    }

    /**
     * 批量记录成本数据（从云厂商账单导入）。
     */
    public int recordCostBatch(List<CostEntry> entries) {
        costEntries.addAll(entries);
        log.info("Batch cost recorded: {} entries", entries.size());
        return entries.size();
    }

    // ==================== 成本查询 ====================

    /**
     * 查询指定时间范围内的成本条目。
     */
    public List<CostEntry> queryCosts(Instant from, Instant to) {
        return costEntries.stream()
                .filter(e -> !e.getTimestamp().isBefore(from) && !e.getTimestamp().isAfter(to))
                .toList();
    }

    /**
     * 按维度归因查询成本总和。
     *
     * @param dim   归因维度
     * @param from  起始时间
     * @param to    结束时间
     * @return 维度值 -> 总成本（分）
     */
    public Map<String, Double> getCostByDimension(CostDimension dim, Instant from, Instant to) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (CostEntry e : queryCosts(from, to)) {
            String val = e.getDimensionValue(dim);
            result.merge(val, e.getAmount(), Double::sum);
        }
        // 降序排列
        return result.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    /**
     * 按资源类型查询成本总和。
     */
    public Map<CostResourceType, Double> getCostByResourceType(Instant from, Instant to) {
        Map<CostResourceType, Double> result = new LinkedHashMap<>();
        for (CostEntry e : queryCosts(from, to)) {
            result.merge(e.getResourceType(), e.getAmount(), Double::sum);
        }
        return result.entrySet().stream()
                .sorted(Map.Entry.<CostResourceType, Double>comparingByValue().reversed())
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    /**
     * 获取指定时间范围内的总成本（分）。
     */
    public double getTotalCost(Instant from, Instant to) {
        return queryCosts(from, to).stream().mapToDouble(CostEntry::getAmount).sum();
    }

    // ==================== 成本报告 ====================

    /**
     * 生成指定周期的成本报告。
     *
     * @param period      报告周期
     * @param periodStart 周期起始时间
     * @return 成本报告
     */
    public CostReport generateReport(CostReport.ReportPeriod period, Instant periodStart) {
        Instant periodEnd = calculatePeriodEnd(period, periodStart);
        String reportId = "report-" + period.name() + "-" + periodStart.toString();

        CostReport report = new CostReport(reportId, period, periodStart, periodEnd);

        // 聚合本期成本
        List<CostEntry> periodEntries = queryCosts(periodStart, periodEnd);
        for (CostEntry entry : periodEntries) {
            report.addEntry(entry);
        }

        // 计算上期成本（环比）
        Instant prevStart = calculatePreviousPeriodStart(period, periodStart);
        Instant prevEnd = periodStart;
        double prevCost = getTotalCost(prevStart, prevEnd);
        report.setPreviousPeriodCost(prevCost);

        // 生成 FinOps 建议
        generateRecommendations(report);

        // 缓存报告
        reportCache.put(reportId, report);

        log.info("Cost report generated: {} total=¥{} period={} change={:.1f}%",
                reportId, report.getTotalCostYuan(), period, report.getCostChangePercent());
        return report;
    }

    /**
     * 获取当前月份至今的成本报告。
     */
    public CostReport getCurrentMonthReport() {
        Instant monthStart = YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return generateReport(CostReport.ReportPeriod.MONTHLY, monthStart);
    }

    /**
     * 获取缓存中的报告。
     */
    public Optional<CostReport> getCachedReport(String reportId) {
        return Optional.ofNullable(reportCache.get(reportId));
    }

    // ==================== 预算管理 ====================

    /**
     * 获取所有预算配置。
     */
    public Collection<Budget> getAllBudgets() {
        return Collections.unmodifiableCollection(budgets.values());
    }

    /**
     * 获取指定预算。
     */
    public Optional<Budget> getBudget(String budgetName) {
        return Optional.ofNullable(budgets.get(budgetName));
    }

    /**
     * 注册/更新预算。
     */
    public void registerBudget(Budget budget) {
        budgets.put(budget.getName(), budget);
        log.info("Budget registered: {} amount=¥{}/month", budget.getName(), budget.getAmount());
    }

    /**
     * 删除预算。
     */
    public void removeBudget(String budgetName) {
        budgets.remove(budgetName);
        log.info("Budget removed: {}", budgetName);
    }

    /**
     * 检查所有预算的告警状态。
     *
     * @param from 统计起始时间
     * @param to   统计结束时间
     * @return 触发的告警列表
     */
    public List<BudgetAlert> checkBudgetAlerts(Instant from, Instant to) {
        List<BudgetAlert> alerts = new ArrayList<>();

        for (Budget budget : budgets.values()) {
            double currentCost;
            if (budget.isTotalBudget()) {
                currentCost = getTotalCost(from, to);
            } else {
                currentCost = queryCosts(from, to).stream()
                        .filter(e -> e.getResourceType() == budget.getResourceType())
                        .mapToDouble(CostEntry::getAmount)
                        .sum();
            }

            double currentCostYuan = currentCost / 100.0;
            int highestThreshold = budget.getHighestTriggeredThreshold(currentCostYuan);

            if (highestThreshold > 0) {
                BudgetAlert alert = new BudgetAlert(
                        UUID.randomUUID().toString(),
                        budget.getName(),
                        highestThreshold,
                        currentCostYuan,
                        budget.getAmount(),
                        BudgetAlert.severityFromThreshold(highestThreshold),
                        Instant.now(),
                        String.format("预算「%s」消耗已达 %.1f%%，当前 ¥%.2f / ¥%.2f",
                                budget.getName(),
                                budget.getConsumptionPercent(currentCostYuan),
                                currentCostYuan,
                                budget.getAmount())
                );
                alerts.add(alert);
                log.warn("Budget alert: {}", alert);
            }
        }

        return alerts;
    }

    // ==================== FinOps 建议 ====================

    private void generateRecommendations(CostReport report) {
        double totalYuan = report.getTotalCostYuan();

        // 1. 如果计算资源占比 > 60%，建议使用 Spot 实例
        Double computeCost = report.getCostByType().getOrDefault(CostResourceType.COMPUTE, 0.0);
        if (computeCost / 100.0 / Math.max(totalYuan, 0.01) > 0.6) {
            double savings = computeCost / 100.0 * 0.4; // 预估节省40%
            report.addRecommendation(new CostReport.FinOpsRecommendation(
                    "FIN-001", "增加 Spot/Preemptible 实例比例",
                    "计算资源占比超过60%，建议将非关键服务迁移至竞价实例，可节省约40%计算成本",
                    savings,
                    CostReport.FinOpsRecommendation.Priority.HIGH,
                    "spot_instance"));
        }

        // 2. 如果存储成本持续增长，建议启用生命周期策略
        Double storageCost = report.getCostByType().getOrDefault(CostResourceType.STORAGE, 0.0);
        if (report.getCostChangePercent() > 10 && storageCost / 100.0 > 1000) {
            report.addRecommendation(new CostReport.FinOpsRecommendation(
                    "FIN-002", "启用存储生命周期策略",
                    "存储成本环比增长超过10%，建议将冷数据自动迁移至低频/归档存储",
                    storageCost / 100.0 * 0.25,
                    CostReport.FinOpsRecommendation.Priority.MEDIUM,
                    "lifecycle"));
        }

        // 3. 如果 API 成本 > ¥3000/月，建议启用 LLM 缓存
        Double apiCost = report.getCostByType().getOrDefault(CostResourceType.API, 0.0);
        if (apiCost / 100.0 > 3000) {
            double savings = apiCost / 100.0 * 0.3; // 缓存可节省约30%
            report.addRecommendation(new CostReport.FinOpsRecommendation(
                    "FIN-003", "启用 LLM/TTS API 缓存",
                    "API调用月成本超过¥3000，建议启用语义缓存和TTS结果缓存，可节省约30%",
                    savings,
                    CostReport.FinOpsRecommendation.Priority.HIGH,
                    "caching"));
        }

        // 4. 如果网络成本 > ¥2000/月，建议优化CDN
        Double networkCost = report.getCostByType().getOrDefault(CostResourceType.NETWORK, 0.0);
        if (networkCost / 100.0 > 2000) {
            report.addRecommendation(new CostReport.FinOpsRecommendation(
                    "FIN-004", "优化CDN配置",
                    "网络成本超过¥2000/月，建议启用 Brotli 压缩、图片自动优化、源站护盾",
                    networkCost / 100.0 * 0.15,
                    CostReport.FinOpsRecommendation.Priority.MEDIUM,
                    "cdn_optimization"));
        }

        // 5. 如果有闲置GPU实例（可观测成本异常低但有固定支出）
        if (report.getCostChangePercent() < -20 && totalYuan > 0) {
            report.addRecommendation(new CostReport.FinOpsRecommendation(
                    "FIN-005", "审查资源 Right-sizing",
                    "成本环比下降超过20%，可能存在过度配置，建议审查资源规格",
                    0,
                    CostReport.FinOpsRecommendation.Priority.LOW,
                    "right_sizing"));
        }

        // 6. 总成本 >= ¥40,000 时建议全面审查
        if (totalYuan >= 40000) {
            report.addRecommendation(new CostReport.FinOpsRecommendation(
                    "FIN-006", "月度成本接近预算上限",
                    String.format("当前月成本 ¥%.2f 接近 ¥50,000 总预算，建议全面审查所有资源使用情况", totalYuan),
                    0,
                    CostReport.FinOpsRecommendation.Priority.HIGH,
                    "budget_review"));
        }
    }

    // ==================== 工具方法 ====================

    private Instant calculatePeriodEnd(CostReport.ReportPeriod period, Instant start) {
        return switch (period) {
            case DAILY -> start.plus(Duration.ofDays(1));
            case WEEKLY -> start.plus(Duration.ofDays(7));
            case MONTHLY -> {
                LocalDate date = start.atZone(ZoneOffset.UTC).toLocalDate();
                yield date.plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        };
    }

    private Instant calculatePreviousPeriodStart(CostReport.ReportPeriod period, Instant currentStart) {
        return switch (period) {
            case DAILY -> currentStart.minus(Duration.ofDays(1));
            case WEEKLY -> currentStart.minus(Duration.ofDays(7));
            case MONTHLY -> {
                LocalDate date = currentStart.atZone(ZoneOffset.UTC).toLocalDate();
                yield date.minusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        };
    }

    /**
     * 清除指定时间之前的成本数据（用于数据归档）。
     */
    public int purgeCostsBefore(Instant cutoff) {
        int before = costEntries.size();
        costEntries.removeIf(e -> e.getTimestamp().isBefore(cutoff));
        int removed = before - costEntries.size();
        log.info("Purged {} cost entries before {}", removed, cutoff);
        return removed;
    }

    /**
     * 获取成本条目总数。
     */
    public int getCostEntryCount() {
        return costEntries.size();
    }
}
