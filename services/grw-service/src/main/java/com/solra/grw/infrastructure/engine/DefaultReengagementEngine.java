package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.RecallStrategyRepository;
import com.solra.grw.domain.service.ReengagementEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DefaultReengagementEngine — 默认用户召回推送引擎实现。
 * 基于规则引擎评估流失风险，生成个性化召回任务。
 * GRW-007: 用户召回推送策略。
 */
@Component
public class DefaultReengagementEngine implements ReengagementEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultReengagementEngine.class);

    private final RecallStrategyRepository strategyRepository;

    /** 预定义召回策略模板 — 按流失风险等级映射 */
    private static final Map<ChurnRiskLevel, RecallStrategy> DEFAULT_STRATEGIES = new LinkedHashMap<>();

    static {
        DEFAULT_STRATEGIES.put(ChurnRiskLevel.LOW, new RecallStrategy(
                "default-low", "轻度流失召回", ChurnRiskLevel.LOW, 7, 14,
                "{avatarName} 有点想你了",
                "你已经 {inactiveDays} 天没回来看 {avatarName} 了，她有点想你。来看看有什么新变化吧！",
                List.of(RecallChannel.PUSH, RecallChannel.IN_APP)
        ));

        DEFAULT_STRATEGIES.put(ChurnRiskLevel.MEDIUM, new RecallStrategy(
                "default-medium", "中度流失召回", ChurnRiskLevel.MEDIUM, 14, 30,
                "{avatarName} 在等你",
                "嘿，{inactiveDays} 天不见了！{avatarName} 为你准备了惊喜，快回来看看吧～",
                List.of(RecallChannel.PUSH, RecallChannel.SMS)
        ));

        DEFAULT_STRATEGIES.put(ChurnRiskLevel.HIGH, new RecallStrategy(
                "default-high", "高度流失召回", ChurnRiskLevel.HIGH, 30, 60,
                "你的虚拟人很想你",
                "已经 {inactiveDays} 天了，{avatarName} 每天都在等你。这里永远有属于你的空间。",
                List.of(RecallChannel.PUSH, RecallChannel.SMS, RecallChannel.EMAIL)
        ));

        DEFAULT_STRATEGIES.put(ChurnRiskLevel.CRITICAL, new RecallStrategy(
                "default-critical", "严重流失召回", ChurnRiskLevel.CRITICAL, 60, 90,
                "好久不见，{avatarName} 还在",
                "超过 {inactiveDays} 天没见了，但 {avatarName} 还记得你们之间的每一次互动。回来重新开始吧。",
                List.of(RecallChannel.PUSH, RecallChannel.SMS, RecallChannel.EMAIL)
        ));

        DEFAULT_STRATEGIES.put(ChurnRiskLevel.CHURNED, new RecallStrategy(
                "default-churned", "已流失用户召回", ChurnRiskLevel.CHURNED, 90, 365,
                "索拉有了全新变化",
                "索拉迎来了全新的空间和虚拟人体验。{avatarName} 想念你，欢迎随时回来看看。",
                List.of(RecallChannel.EMAIL)
        ));
    }

    public DefaultReengagementEngine(RecallStrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
        ensureDefaultStrategies();
    }

    /** 确保默认策略已入库 */
    private void ensureDefaultStrategies() {
        for (RecallStrategy strategy : DEFAULT_STRATEGIES.values()) {
            strategyRepository.findById(strategy.getStrategyId())
                    .orElseGet(() -> strategyRepository.save(strategy));
        }
    }

    @Override
    public ChurnRiskLevel evaluateChurnRisk(String userId, int inactiveDays,
                                             int totalInteractions, int friendsCount,
                                             double presenceScore) {
        // 综合评估：未活跃天数为主要因子，互动次数和好友数为调节因子
        ChurnRiskLevel baseLevel = classifyByInactiveDays(inactiveDays);

        // 高互动用户降低一级风险
        if (totalInteractions > 100 && baseLevel.ordinal() > ChurnRiskLevel.LOW.ordinal()) {
            baseLevel = ChurnRiskLevel.values()[baseLevel.ordinal() - 1];
        }

        // 有好友的用户降低一级风险
        if (friendsCount > 5 && baseLevel.ordinal() > ChurnRiskLevel.LOW.ordinal()) {
            baseLevel = ChurnRiskLevel.values()[baseLevel.ordinal() - 1];
        }

        // 存在值较高的用户降低一级风险
        if (presenceScore > 500 && baseLevel.ordinal() > ChurnRiskLevel.LOW.ordinal()) {
            baseLevel = ChurnRiskLevel.values()[baseLevel.ordinal() - 1];
        }

        log.debug("Churn risk evaluated: user={} inactiveDays={} baseLevel={} finalLevel={}",
                userId, inactiveDays, classifyByInactiveDays(inactiveDays), baseLevel);

        return baseLevel;
    }

    private ChurnRiskLevel classifyByInactiveDays(int inactiveDays) {
        if (inactiveDays < 7) return ChurnRiskLevel.NONE;
        if (inactiveDays < 14) return ChurnRiskLevel.LOW;
        if (inactiveDays < 30) return ChurnRiskLevel.MEDIUM;
        if (inactiveDays < 60) return ChurnRiskLevel.HIGH;
        if (inactiveDays < 90) return ChurnRiskLevel.CRITICAL;
        return ChurnRiskLevel.CHURNED;
    }

    @Override
    public List<RecallTask> generateRecallTasks(String userId, ChurnRiskLevel riskLevel,
                                                  int inactiveDays, String avatarName) {
        List<RecallTask> tasks = new ArrayList<>();
        RecallStrategy strategy = getStrategyForRiskLevel(riskLevel);

        if (strategy == null) {
            log.warn("No recall strategy found for riskLevel={}", riskLevel);
            return tasks;
        }

        String title = fillTemplate(strategy.getTitleTemplate(), avatarName, inactiveDays);
        String message = fillTemplate(strategy.getMessageTemplate(), avatarName, inactiveDays);

        for (RecallChannel channel : strategy.getChannels()) {
            RecallTask task = new RecallTask(
                    UUID.randomUUID().toString(), userId, strategy.getStrategyId(),
                    strategy.getName(), riskLevel, inactiveDays, channel,
                    title, message, 1);
            tasks.add(task);
        }

        log.info("Generated {} recall tasks for user={} riskLevel={} inactiveDays={}",
                tasks.size(), userId, riskLevel, inactiveDays);
        return tasks;
    }

    @Override
    public String fillTemplate(String template, String avatarName, int inactiveDays) {
        if (template == null) return "";
        return template.replace("{avatarName}", avatarName != null ? avatarName : "你的虚拟人")
                .replace("{inactiveDays}", String.valueOf(inactiveDays));
    }

    @Override
    public boolean shouldRecall(ChurnRiskLevel riskLevel, int previousAttempts,
                                 int lastRecallHoursAgo, int cooldownHours, int maxAttempts) {
        // 无风险不需要召回
        if (riskLevel == ChurnRiskLevel.NONE) return false;

        // 超过最大尝试次数
        if (previousAttempts >= maxAttempts) {
            log.debug("Max attempts reached: previousAttempts={} maxAttempts={}", previousAttempts, maxAttempts);
            return false;
        }

        // 冷却期内
        if (lastRecallHoursAgo >= 0 && lastRecallHoursAgo < cooldownHours) {
            log.debug("In cooldown: lastRecallHoursAgo={} cooldownHours={}", lastRecallHoursAgo, cooldownHours);
            return false;
        }

        return true;
    }

    private RecallStrategy getStrategyForRiskLevel(ChurnRiskLevel riskLevel) {
        // 优先从仓储中查找活跃策略
        List<RecallStrategy> strategies = strategyRepository.findByRiskLevel(riskLevel.name());
        Optional<RecallStrategy> active = strategies.stream()
                .filter(RecallStrategy::isActive)
                .findFirst();
        return active.orElseGet(() -> DEFAULT_STRATEGIES.get(riskLevel));
    }
}
