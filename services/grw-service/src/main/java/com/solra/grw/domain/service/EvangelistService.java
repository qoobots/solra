package com.solra.grw.domain.service;

import com.solra.grw.domain.model.Evangelist;

import java.util.List;
import java.util.Optional;

/**
 * EvangelistService — 布道者体系领域服务接口。
 * GRW-003: 布道者申请/认证/权益/义务，占DAU 0.5-1%。
 */
public interface EvangelistService {

    /**
     * 提交布道者申请。
     */
    Evangelist apply(String userId, String displayName, String bio);

    /**
     * 审批布道者申请。
     */
    Evangelist review(String applicationId, boolean approved, String reviewerId, String comment);

    /**
     * 获取用户的布道者状态。
     */
    Optional<Evangelist> getEvangelistStatus(String userId);

    /**
     * 获取所有活跃布道者。
     */
    List<Evangelist> getActiveEvangelists(int page, int size);

    /**
     * 按等级获取布道者。
     */
    List<Evangelist> getEvangelistsByTier(Evangelist.EvangelistTier tier, int page, int size);

    /**
     * 更新布道者粉丝数。
     */
    void updateFollowers(String userId, int count);

    /**
     * 更新布道者访问数。
     */
    void updateVisits(String userId, int count);

    /**
     * 增加布道者贡献分数。
     */
    void addContribution(String userId, double score);

    /**
     * 暂停布道者资格。
     */
    void suspend(String userId, String reason);

    /**
     * 撤销布道者资格。
     */
    void revoke(String userId, String reason);

    /**
     * 检查用户是否达到布道者资格。
     */
    boolean isEligible(String userId);

    /**
     * 获取布道者统计。
     */
    EvangelistStats getStats();

    /** 布道者统计 */
    record EvangelistStats(long totalApplications, long totalApproved, long totalActive,
                           Map<Evangelist.EvangelistTier, Long> byTier,
                           double dauPercent, long avgFollowers, long avgVisits) {}
}
