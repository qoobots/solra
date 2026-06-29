package com.solra.grw.domain.service;

import com.solra.grw.domain.model.Achievement;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AchievementService — 成就系统领域服务接口。
 * GRW-005: ≥30个成就，每成就含专属徽章+动效+音效。
 */
public interface AchievementService {

    /**
     * 检查并解锁用户成就。
     *
     * @param userId 用户ID
     * @param eventType 触发检查的事件类型
     * @param currentProgress 当前进度值
     * @return 新解锁的成就列表
     */
    List<AchievementUnlockResult> checkAndUnlock(String userId, String eventType, int currentProgress);

    /**
     * 获取用户所有成就状态。
     *
     * @param userId 用户ID
     * @return 成就状态映射 (成就编码 → 是否已解锁)
     */
    Map<String, Boolean> getUserAchievementStatus(String userId);

    /**
     * 获取用户已解锁的成就列表。
     */
    List<Achievement> getUnlockedAchievements(String userId);

    /**
     * 获取用户尚未解锁的成就列表（含进度）。
     */
    List<AchievementProgress> getLockedAchievements(String userId);

    /**
     * 获取成就系统全局统计。
     */
    AchievementStats getGlobalStats();

    /**
     * 获取用户成就完成度。
     */
    AchievementCompletion getCompletion(String userId);

    /** 成就解锁结果 */
    record AchievementUnlockResult(String achievementId, String code, String name,
                                    Achievement.Category category, Achievement.Rarity rarity,
                                    String badgeEffect, String soundEffect,
                                    int experienceReward, boolean isNewUnlock) {}

    /** 成就进度 */
    record AchievementProgress(String achievementId, String code, String name,
                                Achievement.Category category, Achievement.Rarity rarity,
                                int requiredCount, int currentProgress,
                                double progressPercent, boolean isUnlockable) {}

    /** 成就完成度 */
    record AchievementCompletion(String userId, int totalAchievements,
                                  int unlockedCount, double completionPercent,
                                  Map<Achievement.Category, CategoryCompletion> byCategory) {}

    /** 分类完成度 */
    record CategoryCompletion(int total, int unlocked, double percent) {}

    /** 全局统计 */
    record AchievementStats(int totalAchievements, int totalDefinitions,
                            Map<Achievement.Rarity, Long> rarityDistribution,
                            Map<String, Long> mostUnlocked) {}
}
