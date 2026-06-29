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

    /** GRW-001: 用户等级提升 */
    public record UserLeveledUp(String userId, int oldLevel, int newLevel, String oldLevelName, String newLevelName) {}

    /** GRW-001: 存在值变化 */
    public record PresenceScoreChanged(String userId, double oldScore, double newScore) {}

    /** GRW-005: 成就已解锁 */
    public record AchievementUnlocked(String userId, String achievementCode, String achievementName,
                                       String rarity, int experienceReward) {}

    /** GRW-003: 布道者申请已提交 */
    public record EvangelistApplied(String userId, String applicationId, String displayName) {}

    /** GRW-003: 布道者已通过审批 */
    public record EvangelistApproved(String userId, String applicationId, String tier) {}

    /** GRW-003: 布道者资格被暂停 */
    public record EvangelistSuspended(String userId, String reason) {}

    /** GRW-004: 虚拟人已收集 */
    public record AvatarCollected(String userId, String avatarTypeId, String name, String rarity) {}

    /** GRW-004: 虚拟人好感度提升 */
    public record AvatarAffectionIncreased(String userId, String avatarTypeId, int newAffection) {}

    /** GRW-008: 信仰仪表盘已生成 */
    public record FaithDashboardGenerated(String userId, double overallFaithScore) {}
}
