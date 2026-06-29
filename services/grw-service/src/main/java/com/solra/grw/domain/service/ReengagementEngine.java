package com.solra.grw.domain.service;

import com.solra.grw.domain.model.ChurnRiskLevel;
import com.solra.grw.domain.model.RecallTask;

import java.util.List;

/**
 * ReengagementEngine — 用户召回推送引擎领域服务接口。
 * 检测流失风险用户并生成召回推送任务。
 * GRW-007: 用户召回推送策略。
 */
public interface ReengagementEngine {

    /**
     * 评估用户的流失风险等级。
     *
     * @param userId 用户ID
     * @param inactiveDays 用户未活跃天数
     * @param totalInteractions 用户总互动次数
     * @param friendsCount 用户好友数
     * @param presenceScore 用户存在值
     * @return 流失风险等级
     */
    ChurnRiskLevel evaluateChurnRisk(String userId, int inactiveDays,
                                      int totalInteractions, int friendsCount,
                                      double presenceScore);

    /**
     * 为指定用户生成召回任务。
     *
     * @param userId 用户ID
     * @param riskLevel 流失风险等级
     * @param inactiveDays 未活跃天数
     * @param avatarName 用户关联的虚拟人名称（用于个性化消息）
     * @return 生成的召回任务列表（每个渠道一个任务）
     */
    List<RecallTask> generateRecallTasks(String userId, ChurnRiskLevel riskLevel,
                                          int inactiveDays, String avatarName);

    /**
     * 个性化消息模板填充。
     *
     * @param template 消息模板（支持 {avatarName} {inactiveDays} 等占位符）
     * @param avatarName 虚拟人名称
     * @param inactiveDays 未活跃天数
     * @return 填充后的消息文本
     */
    String fillTemplate(String template, String avatarName, int inactiveDays);

    /**
     * 判断是否应该对用户进行召回。
     *
     * @param riskLevel 风险等级
     * @param previousAttempts 已尝试召回次数
     * @param lastRecallHoursAgo 上次召回到现在的小时数
     * @param cooldownHours 冷却时间
     * @param maxAttempts 最大尝试次数
     * @return 是否应该召回
     */
    boolean shouldRecall(ChurnRiskLevel riskLevel, int previousAttempts,
                         int lastRecallHoursAgo, int cooldownHours, int maxAttempts);
}
