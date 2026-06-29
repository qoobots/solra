package com.solra.grw.domain.event;

import com.solra.grw.domain.model.FaithLevel;

/**
 * GrwDomainEvents — 用户成长领域事件集合。
 */
public final class GrwDomainEvents {

    private GrwDomainEvents() {}

    /** 决定性时刻被检测到 */
    public record DecisiveMomentDetected(String userId, String momentType, double conversionValue) {}

    /** 引导步骤完成 */
    public record OnboardingStepCompleted(String userId, int stepNumber, String stepType) {}

    /** 引导流程完成 */
    public record OnboardingCompleted(String userId) {}

    /** 信誉等级变化 */
    public record FaithLevelChanged(String userId, FaithLevel oldLevel, FaithLevel newLevel) {}

    /** 经验增长 */
    public record ExperienceGained(String userId, int amount, String eventType) {}

    /** 用户流失风险被评估 */
    public record ChurnRiskEvaluated(String userId, String riskLevel, int inactiveDays) {}

    /** 召回任务被创建 */
    public record RecallTaskGenerated(String taskId, String userId, String channel, String riskLevel) {}

    /** 召回任务已发送 */
    public record RecallTaskSent(String taskId, String userId, String channel) {}

    /** 用户被成功召回（重新活跃） */
    public record UserReengaged(String userId, String taskId, int inactiveDays) {}
}
