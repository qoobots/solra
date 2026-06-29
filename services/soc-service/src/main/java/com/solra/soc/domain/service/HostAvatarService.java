package com.solra.soc.domain.service;

import com.solra.soc.domain.model.HostAvatar;

import java.util.List;
import java.util.Optional;

/**
 * HostAvatarService 领域服务 — 虚拟人主持人机制。
 * <p>
 * SOC-008: 自动协调多人互动节奏，包括话题引导、发言队列、冷场检测、活动组织。
 */
public interface HostAvatarService {

    /** 创建虚拟人主持人 */
    HostAvatar createHost(String sessionId, String avatarId, String avatarName, String mode);

    /** 获取会话的主持人 */
    Optional<HostAvatar> getHost(String sessionId);

    /** 获取指定主持人 */
    Optional<HostAvatar> getHostById(String hostId);

    /** 列出会话中所有主持人 */
    List<HostAvatar> listSessionHosts(String sessionId);

    /** 开始主持 */
    HostAvatar startHosting(String hostId);

    /** 暂停主持 */
    void pauseHosting(String hostId);

    /** 恢复主持 */
    void resumeHosting(String hostId);

    /** 停止主持（移除主持人） */
    void stopHosting(String hostId);

    /** 切换主持人模式 */
    HostAvatar switchMode(String hostId, String mode);

    /** 设置当前话题 */
    HostAvatar setTopic(String hostId, String topic);

    /** 添加话题到队列 */
    void addTopicToQueue(String hostId, String topic);

    /** 切换到下一个话题 */
    String nextTopic(String hostId);

    /** 添加用户到发言队列 */
    void addToSpeakerQueue(String hostId, String userId);

    /** 从发言队列移除 */
    void removeFromSpeakerQueue(String hostId, String userId);

    /** 授予发言权 */
    String grantSpeakingTurn(String hostId);

    /** 结束当前发言 */
    void endSpeakingTurn(String hostId);

    /** 冷场检测 — 返回建议动作 */
    String detectSilence(String sessionId);

    /** 记录互动 */
    void recordInteraction(String hostId);

    /** 获取主持人统计 */
    HostStats getHostStats(String hostId);

    record HostStats(String hostId, String sessionId, String mode, String state,
                     String currentTopic, int topicQueueSize, int speakerQueueSize,
                     int totalInteractions, long runningMinutes) {}
}
