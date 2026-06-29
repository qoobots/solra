package com.solra.grw.domain.service;

import com.solra.grw.domain.model.PresenceLevel;
import com.solra.grw.domain.model.FaithLevel;

/**
 * ExperienceService — 等级与存在值系统领域服务接口。
 * GRW-001: 管理用户经验值、等级计算、存在值（Presence Score）和信誉等级升级。
 */
public interface ExperienceService {

    /**
     * 为用户增加经验值，返回是否触发等级提升。
     *
     * @param userId 用户ID
     * @param amount 经验值增量
     * @param eventType 经验来源类型
     * @return 升级结果（旧等级、新等级、是否升级）
     */
    LevelUpResult addExperience(String userId, int amount, String eventType);

    /**
     * 获取用户当前等级。
     */
    PresenceLevel getLevel(String userId);

    /**
     * 获取用户当前信誉等级（FaithLevel）。
     */
    FaithLevel getFaithLevel(String userId);

    /**
     * 获取升级进度信息。
     */
    LevelProgress getLevelProgress(String userId);

    /**
     * 计算存在值（Presence Score），综合多个维度。
     */
    double calculatePresenceScore(String userId);

    /** 升级结果 */
    record LevelUpResult(String userId, PresenceLevel oldLevel, PresenceLevel newLevel,
                         boolean leveledUp, int experienceGained, int totalExperience,
                         int experienceToNextLevel, double progressPercent) {}

    /** 等级进度 */
    record LevelProgress(String userId, PresenceLevel currentLevel, int totalExperience,
                         int experienceToNextLevel, double progressPercent,
                         FaithLevel faithLevel, double presenceScore) {}
}
