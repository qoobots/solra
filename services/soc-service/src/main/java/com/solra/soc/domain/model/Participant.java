package com.solra.soc.domain.model;

import java.time.Instant;

/**
 * Participant — SOC-001 多人空间会话参与者实体。
 */
public class Participant {

    private String userId;
    private String sessionId;
    private String avatarId;
    private Session.ParticipantStatus status;
    private Instant joinedAt;
    private Instant lastActiveAt;
    private boolean microphoneOn;
    private boolean cameraOn;
    private AvatarSyncState avatarState;

    public Participant() {}

    public Participant(String userId, String sessionId, String avatarId,
                        Session.ParticipantStatus status) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.avatarId = avatarId;
        this.status = status;
        this.joinedAt = Instant.now();
        this.lastActiveAt = this.joinedAt;
        this.avatarState = new AvatarSyncState(avatarId);
    }

    public void updateAvatarState(AvatarSyncState state) {
        this.avatarState = state;
        this.lastActiveAt = Instant.now();
    }

    // -- Getters/Setters --
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public Session.ParticipantStatus getStatus() { return status; }
    public void setStatus(Session.ParticipantStatus status) { this.status = status; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public boolean isMicrophoneOn() { return microphoneOn; }
    public void setMicrophoneOn(boolean microphoneOn) { this.microphoneOn = microphoneOn; }
    public boolean isCameraOn() { return cameraOn; }
    public void setCameraOn(boolean cameraOn) { this.cameraOn = cameraOn; }
    public AvatarSyncState getAvatarState() { return avatarState; }
    public void setAvatarState(AvatarSyncState avatarState) { this.avatarState = avatarState; }
}
