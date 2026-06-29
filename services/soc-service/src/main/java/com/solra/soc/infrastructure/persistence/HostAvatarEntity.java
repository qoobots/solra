package com.solra.soc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * HostAvatar JPA 实体 — 虚拟人主持人持久化映射。
 */
@Entity
@Table(name = "host_avatars")
public class HostAvatarEntity {

    @Id
    @Column(name = "host_id", length = 64)
    private String hostId;

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "avatar_id", length = 64, nullable = false)
    private String avatarId;

    @Column(name = "avatar_name", length = 100)
    private String avatarName;

    @Column(name = "mode", length = 20, nullable = false)
    private String mode;

    @Column(name = "state", length = 20, nullable = false)
    private String state;

    @Column(name = "current_topic", length = 500)
    private String currentTopic;

    @Column(name = "topic_queue", columnDefinition = "TEXT")
    private String topicQueue; // JSON array

    @Column(name = "speaker_queue", columnDefinition = "TEXT")
    private String speakerQueue; // JSON array

    @Column(name = "active_speaker", length = 64)
    private String activeSpeaker;

    @Column(name = "speaking_duration_sec")
    private int speakingDurationSec;

    @Column(name = "total_interactions")
    private int totalInteractions;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    public HostAvatarEntity() {}

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public String getAvatarName() { return avatarName; }
    public void setAvatarName(String avatarName) { this.avatarName = avatarName; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getCurrentTopic() { return currentTopic; }
    public void setCurrentTopic(String currentTopic) { this.currentTopic = currentTopic; }

    public String getTopicQueue() { return topicQueue; }
    public void setTopicQueue(String topicQueue) { this.topicQueue = topicQueue; }

    public String getSpeakerQueue() { return speakerQueue; }
    public void setSpeakerQueue(String speakerQueue) { this.speakerQueue = speakerQueue; }

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
