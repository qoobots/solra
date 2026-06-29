package com.solra.soc.domain.service;

import com.solra.soc.domain.model.ShareClick;
import com.solra.soc.domain.model.ShareSession;

import java.util.Optional;

/**
 * ShareEngine — 分享引擎领域服务接口。
 * <p>
 * 负责分享链接生成、点击追踪、转化追踪和病毒传播链统计。
 * 具体实现由基础设施层提供（如使用 JPA 持久化、Redis 缓存等）。
 */
public interface ShareEngine {

    /**
     * 生成一个分享链接。
     * <p>
     * 创建 ShareSession 聚合根，生成唯一分享码，返回完整的分享会话。
     *
     * @param spaceId      空间ID
     * @param sharerUserId 分享者用户ID
     * @param type         分享类型（SPACE/PROFILE/INVITE）
     * @return 包含分享码和链接的分享会话
     */
    ShareSession generateShareLink(String spaceId, String sharerUserId, String type);

    /**
     * 追踪一次分享点击。
     * <p>
     * 记录访客信息，更新对应分享会话的点击计数和传播链。
     *
     * @param shareCode   分享码
     * @param visitorInfo 访客信息（userId可为空表示匿名访客）
     * @return 点击记录（可能为空，表示分享码无效或已过期）
     */
    Optional<ShareClick> trackClick(String shareCode, VisitorInfo visitorInfo);

    /**
     * 追踪一次转化。
     * <p>
     * 当被分享用户完成注册或加入空间时调用，更新转化计数。
     *
     * @param shareCode 分享码
     * @param userId    完成转化的用户ID
     * @return true 表示转化记录成功，false 表示分享码无效或已过期
     */
    boolean trackConversion(String shareCode, String userId);

    /**
     * 获取病毒传播链统计。
     *
     * @param shareCode 分享码
     * @return 病毒传播统计数据（可能为空）
     */
    Optional<ViralStats> getViralChainStats(String shareCode);

    /**
     * 访客信息值对象。
     */
    record VisitorInfo(String visitorUserId, String ipAddress, String userAgent, String platform) {
        public static VisitorInfo of(String userId, String ip, String ua, String platform) {
            return new VisitorInfo(userId, ip, ua, platform);
        }
    }

    /**
     * 病毒传播统计数据值对象。
     */
    record ViralStats(String shareId, String shareCode, long totalClicks, long totalConversions,
                      double conversionRate, java.util.List<String> topReferrers, String viralChain) {}
}
