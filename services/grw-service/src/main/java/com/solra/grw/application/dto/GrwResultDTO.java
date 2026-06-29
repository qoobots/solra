package com.solra.grw.application.dto;

import com.solra.grw.domain.model.FaithLevel;

import java.time.Instant;

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
