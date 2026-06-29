package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * Session — SOC-001 多人空间会话聚合根。
 *
 * 管理一个空间内的多人会话生命周期：创建→参与者加入/离开→结束。
 * 支持 WebRTC 信令和状态同步。
 */
public class Session {

    private String sessionId;
    private String spaceId;
    private SessionType type;
    private SessionStatus status;
    private List<Participant> participants = new ArrayList<>();
    private int maxParticipants;
    private String hostId;
    private Instant startedAt;
    private Instant endedAt;
    private SessionConfig config;
    private String webrtcOfferSdp;  // SOC-006: WebRTC信令

    // State sync sequence number
    private long sequenceNumber;

    private Session() {}

    public static Session create(String sessionId, String spaceId, String hostId,
                                  SessionType type, int maxParticipants, SessionConfig config) {
        Session s = new Session();
        s.sessionId = sessionId;
        s.spaceId = spaceId;
        s.hostId = hostId;
        s.type = type;
        s.status = SessionStatus.PENDING;
        s.maxParticipants = Math.max(1, Math.min(maxParticipants, 100));
        s.config = config != null ? config : SessionConfig.defaults();
        s.startedAt = Instant.now();
        s.sequenceNumber = 0;
        return s;
    }

    /** Activate session */
    public void activate() {
        if (status != SessionStatus.PENDING) {
            throw new IllegalStateException("Session can only be activated from PENDING, current: " + status);
        }
        this.status = SessionStatus.ACTIVE;
        this.startedAt = Instant.now();
    }

    /** SOC-001: Participant joins */
    public void joinParticipant(String userId, String avatarId, String webrtcAnswerSdp) {
        if (status != SessionStatus.ACTIVE && status != SessionStatus.PENDING) {
            throw new IllegalStateException("Cannot join session in status: " + status);
        }
        if (participants.size() >= maxParticipants) {
            throw new IllegalStateException("Session full: " + maxParticipants + " max");
        }

        // Remove duplicate if reconnecting
        participants.removeIf(p -> p.getUserId().equals(userId));

        Participant p = new Participant(userId, sessionId, avatarId,
                ParticipantStatus.CONNECTING);
        participants.add(p);
        sequenceNumber++;
    }

    /** SOC-001: Participant leaves */
    public void leaveParticipant(String userId) {
        participants.removeIf(p -> p.getUserId().equals(userId));
        sequenceNumber++;

        if (participants.isEmpty()) {
            this.status = SessionStatus.ENDED;
            this.endedAt = Instant.now();
        }
    }

    /** Update participant status */
    public void updateParticipantStatus(String userId, ParticipantStatus newStatus) {
        participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus(newStatus);
                    p.setLastActiveAt(Instant.now());
                });
        sequenceNumber++;
    }

    /** Update participant microphone/camera state */
    public void updateParticipantMedia(String userId, boolean microphoneOn, boolean cameraOn) {
        participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .ifPresent(p -> {
                    p.setMicrophoneOn(microphoneOn);
                    p.setCameraOn(cameraOn);
                });
    }

    /** End session */
    public void end() {
        this.status = SessionStatus.ENDED;
        this.endedAt = Instant.now();
    }

    /** Pause session */
    public void pause() {
        if (status == SessionStatus.ACTIVE) {
            this.status = SessionStatus.PAUSED;
        }
    }

    /** Resume session */
    public void resume() {
        if (status == SessionStatus.PAUSED) {
            this.status = SessionStatus.ACTIVE;
        }
    }

    /** Get online participant count */
    public int getOnlineCount() {
        return (int) participants.stream()
                .filter(p -> p.getStatus() != ParticipantStatus.CONNECTING)
                .count();
    }

    /** Check if user is in session */
    public boolean hasParticipant(String userId) {
        return participants.stream().anyMatch(p -> p.getUserId().equals(userId));
    }

    /** Get participant by user ID */
    public Optional<Participant> getParticipant(String userId) {
        return participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst();
    }

    // -- Getters/Setters --
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public SessionType getType() { return type; }
    public void setType(SessionType type) { this.type = type; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public List<Participant> getParticipants() { return Collections.unmodifiableList(participants); }
    public void setParticipants(List<Participant> participants) { this.participants = participants; }
    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public SessionConfig getConfig() { return config; }
    public void setConfig(SessionConfig config) { this.config = config; }
    public String getWebrtcOfferSdp() { return webrtcOfferSdp; }
    public void setWebrtcOfferSdp(String webrtcOfferSdp) { this.webrtcOfferSdp = webrtcOfferSdp; }
    public long getSequenceNumber() { return sequenceNumber; }

    // -- Enums --
    public enum SessionType { PRIVATE, FRIENDS, PUBLIC, EVENT }
    public enum SessionStatus { PENDING, ACTIVE, PAUSED, ENDED }
    public enum ParticipantStatus { CONNECTING, ACTIVE, AWAY, SPEAKING, MUTED }
}
