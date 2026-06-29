package com.solra.grw.domain.service;

import com.solra.grw.domain.model.FaithDashboard;

/**
 * FaithDashboardService — 信仰体系可视化领域服务接口。
 * GRW-008: 展示用户"存在深度"的多维度证据。
 */
public interface FaithDashboardService {

    /**
     * 为用户生成信仰仪表盘。
     * 综合所有维度的数据，生成用户存在深度的完整画像。
     */
    FaithDashboard generateDashboard(String userId);

    /**
     * 获取用户仪表盘（如已生成）。
     */
    FaithDashboard getDashboard(String userId);

    /**
     * 刷新仪表盘数据。
     */
    FaithDashboard refreshDashboard(String userId);

    /**
     * 获取用户在某维度的平台百分位。
     */
    double getPercentile(String userId, FaithDashboard.DepthDimension dimension);

    /**
     * 获取平台的全球统计数据（用于对比）。
     */
    GlobalFaithStats getGlobalStats();

    /** 全球信仰统计 */
    record GlobalFaithStats(long totalUsers, double avgFaithScore,
                             double avgPresenceScore, long totalMilestones,
                             FaithDashboard.DepthDimension topDimension) {}
}
