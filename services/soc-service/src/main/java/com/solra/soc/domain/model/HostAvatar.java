package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * HostAvatar 聚合根 — 虚拟人主持人，负责在多人空间中自动协调互动节奏。
 * <p>
 * SOC-008: 虚拟人主持人机制。支持话题引导、互动协调、节奏控制、发言队列管理。
 */
public class HostAvatar {

    private String hostId;
    private String sessionId;
    private String avatarId;
    private String avatarName;
    private HostMode mode;
    private HostState state;
    private String currentTopic;
    private List<String> topicQueue = new ArrayList<>();
    private List<String> speakerQueue = new ArrayList<>();
    private String activeSpeaker;
    private int speakingDurationSec;
    private int totalInteractions;
    private Instant startedAt;
    private Instant lastActivityAt;

    /** 主持人模式 */
    public enum HostMode {
        /** 自由模式：不干预，仅在冷场时介入 */
        FREE_FLOW,
        /** 轮流模式：按发言队列依次邀请 */
        ROUND_ROBIN,
        /** 引导模式：主动抛出话题引导讨论 */
        GUIDED,
        /** 活动模式：组织游戏/竞赛/投票等结构化互动 */
        EVENT_DRIVEN
    }

    /** 主持人状态 */
    public enum HostState {
        /** 待机中 */
        IDLE,
        /** 正在引导中 */
        GUIDING,
        /** 正在协调发言 */
        COORDINATING,
        /** 正在组织活动 */
        HOSTING,
        /** 暂停（用户要求） */
        PAUSED
    }

    public HostAvatar() {}

    public HostAvatar(String hostId, String sessionId, String avatarId, String avatarName, HostMode mode) {
        this.hostId = hostId;
        this.sessionId = sessionId;
        this.avatarId = avatarId;
        this.avatarName = avatarName;
        this.mode = mode;
        this.state = HostState.IDLE;
        this.startedAt = Instant.now();
        this.lastActivityAt = Instant.now();
    }

    // ---- business methods ----

    /** 开始主持 */
    public void start() {
        this.state = HostState.GUIDING;
        this.lastActivityAt = Instant.now();
    }

    /** 暂停主持 */
    public void pause() {
        this.state = HostState.PAUSED;
        this.lastActivityAt = Instant.now();
    }

    /** 恢复主持 */
    public void resume() {
        this.state = HostState.GUIDING;
        this.lastActivityAt = Instant.now();
    }

    /** 切换模式 */
    public void switchMode(HostMode newMode) {
        this.mode = newMode;
        this.lastActivityAt = Instant.now();
    }

    /** 添加话题到队列 */
    public void addTopic(String topic) {
        if (!topicQueue.contains(topic)) {
            topicQueue.add(topic);
        }
        this.lastActivityAt = Instant.now();
    }

    /** 设置当前话题 */
    public void setCurrentTopic(String topic) {
        this.currentTopic = topic;
        this.state = HostState.GUIDING;
        this.lastActivityAt = Instant.now();
    }

    /** 添加用户到发言队列 */
    public void addToSpeakerQueue(String userId) {
        if (!speakerQueue.contains(userId)) {
            speakerQueue.add(userId);
        }
        this.lastActivityAt = Instant.now();
    }

    /** 从发言队列移除用户 */
    public void removeFromSpeakerQueue(String userId) {
        speakerQueue.remove(userId);
        this.lastActivityAt = Instant.now();
    }

    /** 授予发言权 */
    public String grantSpeakingTurn() {
        if (speakerQueue.isEmpty()) return null;
        String next = speakerQueue.remove(0);
        this.activeSpeaker = next;
        this.state = HostState.COORDINATING;
        this.speakingDurationSec = 0;
        this.lastActivityAt = Instant.now();
        return next;
    }

    /** 结束当前发言 */
    public void endSpeakingTurn() {
        this.activeSpeaker = null;
        this.speakingDurationSec = 0;
        this.state = HostState.GUIDING;
        this.lastActivityAt = Instant.now();
    }

    /** 记录互动次数 */
    public void recordInteraction() {
        this.totalInteractions++;
        this.lastActivityAt = Instant.now();
    }

    /** 切换当前话题为队列中下一个 */
    public String nextTopic() {
        if (topicQueue.isEmpty()) return null;
        String next = topicQueue.remove(0);
        this.currentTopic = next;
        this.state = HostState.GUIDING;
        this.lastActivityAt = Instant.now();
        return next;
    }

    /** 检查是否在活跃 */
    public boolean isActive() {
        return state != HostState.IDLE && state != HostState.PAUSED;
    }

    /** 获取发言队列长度 */
    public int getSpeakerQueueSize() {
        return speakerQueue.size();
    }

    /** 获取话题队列长度 */
    public int getTopicQueueSize() {
        return topicQueue.size();
    }

    // ---- getters / setters ----

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public String getAvatarName() { return avatarName; }
    public void setAvatarName(String avatarName) { this.avatarName = avatarName; }

    public HostMode getMode() { return mode; }
    public void setMode(HostMode mode) { this.mode = mode; }

    public HostState getState() { return state; }
    public void setState(HostState state) { this.state = state; }

    public String getCurrentTopic() { return currentTopic; }
    public void setCurrentTopic(String currentTopic) { this.currentTopic = currentTopic; }

    public List<String> getTopicQueue() { return topicQueue; }
    public void setTopicQueue(List<String> topicQueue) { this.topicQueue = topicQueue; }

    public List<String> getSpeakerQueue() { return speakerQueue; }
    public void setSpeakerQueue(List<String> speakerQueue) { this.speakerQueue = speakerQueue; }

    public String getActiveSpeaker() { return activeSpeaker; }
    public void setActiveSpeaker(String activeSpeaker) { this.activeSpeaker = activeSpeaker; }

    public int getSpeakingDurationSec() { return speakingDurationSec; }
    public void setSpeakingDurationSec(int speakingDurationSec) { this.speakingDurationSec = speakingDurationSec; }

    public int getTotalInteractions() { return totalInteractions; }
    public void setTotalInteractions(int totalInteractions) { this.totalInteractions = totalInteractions; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Instant lastActivityAt) { this.lastActivityAt = lastActivityAt; }
}
