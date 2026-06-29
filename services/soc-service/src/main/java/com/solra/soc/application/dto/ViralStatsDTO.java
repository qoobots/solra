package com.solra.soc.application.dto;

import com.solra.soc.domain.service.ShareEngine;

import java.util.List;

/**
 * 病毒传播统计 DTO — 不可变 record。
 */
public record ViralStatsDTO(String shareId, String shareCode, long totalShares, long totalClicks,
                             long totalConversions, double conversionRate,
                             List<String> topReferrers, String viralChain) {

    /**
     * 从领域服务 ViralStats 构造 DTO。
     */
    public static ViralStatsDTO from(ShareEngine.ViralStats stats) {
        return new ViralStatsDTO(
                stats.shareId(),
                stats.shareCode(),
                1, // totalShares: 当前仅单个分享会话
                stats.totalClicks(),
                stats.totalConversions(),
                stats.conversionRate(),
                stats.topReferrers(),
                stats.viralChain()
        );
    }
}
