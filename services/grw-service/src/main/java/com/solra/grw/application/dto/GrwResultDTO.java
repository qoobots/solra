package com.solra.grw.application.dto;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.service.AchievementService;
import com.solra.grw.domain.service.AvatarCollectionService;
import com.solra.grw.domain.service.EvangelistService;
import com.solra.grw.domain.service.ExperienceService;
import com.solra.grw.domain.service.FaithDashboardService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 用户画像结果 DTO */
public record UserProfileResultDTO(
        String userId,
        double presenceScore,
        FaithLevel faithLevel,
        int totalInteractions,
        int spacesVisited,
        int conversationsHad,
        int friendsCount,
        Instant lastActiveAt,
        boolean onboardingCompleted
) {}

/** 决定性时刻结果 DTO */
public record DecisiveMomentResultDTO(
        String momentId,
        String userId,
        String momentType,
        Instant detectedAt,
        double conversionValue,
        boolean triggered
) {}

/** 引导路径结果 DTO */
public record OnboardingPathResultDTO(
        String pathId,
        String userId,
        int currentStep,
        int totalSteps,
        String status,
        Instant startTime,
        Instant completedAt
) {}

/** 引导步骤结果 DTO */
public record OnboardingStepResultDTO(
        int stepNumber,
        String stepType,
        boolean completed,
        boolean skipped
) {}

/** 经验结果 DTO */
public record ExperienceResultDTO(
        String eventId,
        String userId,
        String eventType,
        int value,
        int totalExperience,
        Instant timestamp
) {}

/** 信誉等级结果 DTO */
public record FaithLevelResultDTO(
        String userId,
        FaithLevel currentLevel,
        FaithLevel previousLevel,
        boolean changed
) {}

/** 流失风险评估结果 DTO */
public record ChurnRiskResultDTO(
        String userId,
        String riskLevel,
        int inactiveDays,
        double churnProbability,
        boolean shouldRecall,
        java.time.Instant evaluatedAt
) {}

/** 召回任务结果 DTO */
public record RecallTaskResultDTO(
        String taskId,
        String userId,
        String strategyId,
        String strategyName,
        String riskLevel,
        int inactiveDays,
        String channel,
        String status,
        String title,
        String message,
        int attemptNumber,
        java.time.Instant createdAt,
        java.time.Instant sentAt,
        java.time.Instant clickedAt,
        java.time.Instant convertedAt
) {}

/** 召回策略结果 DTO */
public record RecallStrategyResultDTO(
        String strategyId,
        String name,
        String targetRiskLevel,
        int inactiveDaysMin,
        int inactiveDaysMax,
        java.util.List<String> channels,
        int maxAttempts,
        int cooldownHours,
        boolean active,
        java.time.Instant createdAt
) {}

/** 召回统计 DTO */
public record RecallStatsDTO(
        int totalEvaluated,
        int atRisk,
        int tasksGenerated,
        int tasksSent,
        int clicked,
        int converted,
        double conversionRate
) {}

// ===== GRW-001 等级与存在值 DTO =====

/** 等级升级结果 DTO */
public record LevelUpResultDTO(
        String userId,
        int oldLevel,
        int newLevel,
        String oldLevelName,
        String newLevelName,
        boolean leveledUp,
        int experienceGained,
        int totalExperience,
        int experienceToNextLevel,
        double progressPercent
) {
    public static LevelUpResultDTO from(ExperienceService.LevelUpResult r) {
        return new LevelUpResultDTO(r.userId(), r.oldLevel().getLevel(), r.newLevel().getLevel(),
                r.oldLevel().getDisplayName(), r.newLevel().getDisplayName(),
                r.leveledUp(), r.experienceGained(), r.totalExperience(),
                r.experienceToNextLevel(), r.progressPercent());
    }
}

/** 等级进度 DTO */
public record LevelProgressDTO(
        String userId,
        int currentLevel,
        String currentLevelName,
        int totalExperience,
        int experienceToNextLevel,
        double progressPercent,
        String faithLevel,
        double presenceScore
) {
    public static LevelProgressDTO from(ExperienceService.LevelProgress p) {
        return new LevelProgressDTO(p.userId(), p.currentLevel().getLevel(),
                p.currentLevel().getDisplayName(), p.totalExperience(),
                p.experienceToNextLevel(), p.progressPercent(),
                p.faithLevel().name(), p.presenceScore());
    }
}

// ===== GRW-005 成就系统 DTO =====

/** 成就解锁结果 DTO */
public record AchievementUnlockDTO(
        String achievementId,
        String code,
        String name,
        String category,
        String rarity,
        String badgeEffect,
        String soundEffect,
        int experienceReward,
        boolean isNewUnlock
) {
    public static AchievementUnlockDTO from(AchievementService.AchievementUnlockResult r) {
        return new AchievementUnlockDTO(r.achievementId(), r.code(), r.name(),
                r.category().name(), r.rarity().name(),
                r.badgeEffect(), r.soundEffect(), r.experienceReward(), r.isNewUnlock());
    }
}

/** 成就进度 DTO */
public record AchievementProgressDTO(
        String achievementId,
        String code,
        String name,
        String category,
        String rarity,
        int requiredCount,
        int currentProgress,
        double progressPercent,
        boolean isUnlockable
) {
    public static AchievementProgressDTO from(AchievementService.AchievementProgress p) {
        return new AchievementProgressDTO(p.achievementId(), p.code(), p.name(),
                p.category().name(), p.rarity().name(),
                p.requiredCount(), p.currentProgress(), p.progressPercent(), p.isUnlockable());
    }
}

/** 成就完成度 DTO */
public record AchievementCompletionDTO(
        String userId,
        int totalAchievements,
        int unlockedCount,
        double completionPercent,
        Map<String, CategoryCompletionDTO> byCategory
) {
    public static AchievementCompletionDTO from(AchievementService.AchievementCompletion c) {
        Map<String, CategoryCompletionDTO> byCat = c.byCategory().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(),
                        e -> new CategoryCompletionDTO(e.getValue().total(),
                                e.getValue().unlocked(), e.getValue().percent())));
        return new AchievementCompletionDTO(c.userId(), c.totalAchievements(),
                c.unlockedCount(), c.completionPercent(), byCat);
    }
}

public record CategoryCompletionDTO(int total, int unlocked, double percent) {}

// ===== GRW-003 布道者体系 DTO =====

/** 布道者信息 DTO */
public record EvangelistDTO(
        String applicationId,
        String userId,
        String displayName,
        String bio,
        String tier,
        String tierDisplayName,
        String status,
        int followersCount,
        int totalVisits,
        int spacesCreated,
        int sharesGenerated,
        double contributionScore,
        Instant appliedAt,
        Instant reviewedAt
) {
    public static EvangelistDTO from(Evangelist e) {
        return new EvangelistDTO(e.getApplicationId(), e.getUserId(), e.getDisplayName(),
                e.getBio(), e.getTier().name(), e.getTier().getDisplayName(),
                e.getStatus().name(), e.getFollowersCount(), e.getTotalVisits(),
                e.getSpacesCreated(), e.getSharesGenerated(), e.getContributionScore(),
                e.getAppliedAt(), e.getReviewedAt());
    }
}

/** 布道者统计 DTO */
public record EvangelistStatsDTO(
        long totalApplications,
        long totalApproved,
        long totalActive,
        Map<String, Long> byTier,
        double dauPercent,
        long avgFollowers,
        long avgVisits
) {
    public static EvangelistStatsDTO from(EvangelistService.EvangelistStats s) {
        Map<String, Long> tierMap = s.byTier().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        return new EvangelistStatsDTO(s.totalApplications(), s.totalApproved(),
                s.totalActive(), tierMap, s.dauPercent(), s.avgFollowers(), s.avgVisits());
    }
}

// ===== GRW-004 虚拟人收集与养成 DTO =====

/** 虚拟人条目 DTO */
public record AvatarEntryDTO(
        String avatarId,
        String avatarTypeId,
        String name,
        String rarity,
        String element,
        int level,
        int experience,
        int affection,
        boolean isFavorite,
        double growthProgress,
        Instant obtainedAt,
        Instant lastInteractedAt
) {
    public static AvatarEntryDTO from(AvatarCollection.AvatarEntry e) {
        return new AvatarEntryDTO(e.getAvatarId(), e.getAvatarTypeId(), e.getName(),
                e.getRarity().name(), e.getElement().name(),
                e.getLevel(), e.getExperience(), e.getAffection(),
                e.isFavorite(), e.getGrowthProgress(),
                e.getObtainedAt(), e.getLastInteractedAt());
    }
}

/** 图鉴进度 DTO */
public record CollectionProgressDTO(
        String userId,
        int collected,
        int totalAvailable,
        double completionPercent,
        Map<String, Long> rarityDistribution,
        Map<String, Long> elementDistribution,
        int totalSlots,
        int usedSlots
) {
    public static CollectionProgressDTO from(AvatarCollectionService.CollectionProgress p) {
        Map<String, Long> rarityMap = p.rarityDistribution().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        Map<String, Long> elementMap = p.elementDistribution().entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue));
        return new CollectionProgressDTO(p.userId(), p.collected(), p.totalAvailable(),
                p.completionPercent(), rarityMap, elementMap,
                p.totalSlots(), p.usedSlots());
    }
}

// ===== GRW-008 信仰体系可视化 DTO =====

/** 维度度量 DTO */
public record DimensionMetricDTO(
        String dimension,
        String label,
        String description,
        double value,
        String unit,
        double percentile,
        double trend,
        String highlight
) {
    public static DimensionMetricDTO from(FaithDashboard.DimensionMetric m) {
        return new DimensionMetricDTO(m.getDimension().name(), m.getDimension().getLabel(),
                m.getDimension().getDescription(), m.getValue(), m.getDimension().getUnit(),
                m.getPercentile(), m.getTrend(), m.getHighlight());
    }
}

/** 里程碑 DTO */
public record MilestoneDTO(
        String milestoneId,
        String title,
        String description,
        Instant achievedAt,
        String iconUrl,
        boolean isHighlight
) {
    public static MilestoneDTO from(FaithDashboard.Milestone m) {
        return new MilestoneDTO(m.getMilestoneId(), m.getTitle(), m.getDescription(),
                m.getAchievedAt(), m.getIconUrl(), m.isHighlight());
    }
}

/** 信仰仪表盘 DTO */
public record FaithDashboardDTO(
        String userId,
        List<DimensionMetricDTO> dimensions,
        List<MilestoneDTO> milestones,
        double overallFaithScore,
        String faithLevel,
        String presenceLevel,
        String personalNarrative,
        Instant generatedAt
) {
    public static FaithDashboardDTO from(FaithDashboard d) {
        List<DimensionMetricDTO> dims = d.getDimensions().values().stream()
                .map(DimensionMetricDTO::from).toList();
        List<MilestoneDTO> ms = d.getMilestones().stream()
                .map(MilestoneDTO::from).toList();
        return new FaithDashboardDTO(d.getUserId(), dims, ms,
                d.getOverallFaithScore(),
                d.getFaithLevel() != null ? d.getFaithLevel().name() : null,
                d.getPresenceLevel() != null ? d.getPresenceLevel().getDisplayName() : null,
                d.getPersonalNarrative(), d.getGeneratedAt());
    }
}

/** 全球信仰统计 DTO */
public record GlobalFaithStatsDTO(
        long totalUsers,
        double avgFaithScore,
        double avgPresenceScore,
        long totalMilestones,
        String topDimension
) {
    public static GlobalFaithStatsDTO from(FaithDashboardService.GlobalFaithStats s) {
        return new GlobalFaithStatsDTO(s.totalUsers(), s.avgFaithScore(),
                s.avgPresenceScore(), s.totalMilestones(), s.topDimension().name());
    }
}
