package com.solra.soc.domain.service;

import com.solra.soc.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SessionManager — SOC-001 多人空间会话管理领域服务。
 *
 * 管理会话生命周期：创建/加入/离开/结束。
 * 同时提供 WebRTC 信令管理（SOC-006）。
 */
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    /** 活跃会话缓存：sessionId -> Session */
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    /** 空间内活跃会话索引：spaceId -> Set<sessionId> */
    private final Map<String, Set<String>> spaceSessions = new ConcurrentHashMap<>();

    /** 最大并发会话数 */
    private static final int MAX_SESSIONS_PER_SPACE = 50;

    public SessionManager() {}

    /**
     * SOC-001: Create a new multi-user session.
     */
    public Session createSession(String spaceId, String hostId, Session.SessionType type,
                                  int maxParticipants, SessionConfig config) {
        // Check space session limit
        Set<String> existing = spaceSessions.getOrDefault(spaceId, Set.of());
        if (existing.size() >= MAX_SESSIONS_PER_SPACE) {
            throw new IllegalStateException("Too many active sessions in space: " + spaceId);
        }

        String sessionId = UUID.randomUUID().toString();
        Session session = Session.create(sessionId, spaceId, hostId, type, maxParticipants, config);

        // Generate mock WebRTC offer SDP (SOC-006)
        session.setWebrtcOfferSdp(generateOfferSdp(sessionId));

        activeSessions.put(sessionId, session);
        spaceSessions.computeIfAbsent(spaceId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.info("SOC-001 session created: id={} space={} host={} type={}",
                sessionId, spaceId, hostId, type);
        return session;
    }

    /**
     * SOC-001: Join an existing session.
     */
    public Session joinSession(String sessionId, String userId, String avatarId,
                                String webrtcAnswerSdp) {
        Session session = getActiveSession(sessionId);
        session.joinParticipant(userId, avatarId, webrtcAnswerSdp);

        if (session.getStatus() == Session.SessionStatus.PENDING) {
            session.activate();
        }

        // Mark participant as active
        session.updateParticipantStatus(userId, Session.ParticipantStatus.ACTIVE);

        log.info("SOC-001 participant joined: session={} user={} participants={}",
                sessionId, userId, session.getParticipants().size());
        return session;
    }

    /**
     * SOC-001: Leave a session.
     */
    public void leaveSession(String sessionId, String userId) {
        Session session = getActiveSession(sessionId);
        session.leaveParticipant(userId);

        log.info("SOC-001 participant left: session={} user={} remaining={}",
                sessionId, userId, session.getParticipants().size());

        // Cleanup empty sessions
        if (session.getStatus() == Session.SessionStatus.ENDED) {
            cleanupSession(sessionId, session.getSpaceId());
        }
    }

    /**
     * Get session details.
     */
    public Session getSession(String sessionId) {
        return getActiveSession(sessionId);
    }

    /**
     * List active sessions in a space.
     */
    public List<Session> listActiveSessions(String spaceId) {
        Set<String> ids = spaceSessions.getOrDefault(spaceId, Set.of());
        return ids.stream()
                .map(activeSessions::get)
                .filter(Objects::nonNull)
                .filter(s -> s.getStatus() != Session.SessionStatus.ENDED)
                .toList();
    }

    /**
     * End a session.
     */
    public void endSession(String sessionId) {
        Session session = getActiveSession(sessionId);
        session.end();
        cleanupSession(sessionId, session.getSpaceId());
        log.info("SOC-001 session ended: {}", sessionId);
    }

    /**
     * Get online participants in a space (across all sessions).
     */
    public List<Participant> getOnlineParticipants(String spaceId) {
        Set<String> ids = spaceSessions.getOrDefault(spaceId, Set.of());
        List<Participant> all = new ArrayList<>();
        for (String sid : ids) {
            Session s = activeSessions.get(sid);
            if (s != null && s.getStatus() != Session.SessionStatus.ENDED) {
                all.addAll(s.getParticipants());
            }
        }
        return all;
    }

    /**
     * Update participant status (e.g., speaking, muted).
     */
    public void updateParticipantStatus(String sessionId, String userId,
                                         Session.ParticipantStatus status) {
        Session session = getActiveSession(sessionId);
        session.updateParticipantStatus(userId, status);
    }

    /**
     * Update participant media state.
     */
    public void updateParticipantMedia(String sessionId, String userId,
                                        boolean microphoneOn, boolean cameraOn) {
        Session session = getActiveSession(sessionId);
        session.updateParticipantMedia(userId, microphoneOn, cameraOn);
    }

    /**
     * SOC-006: Generate WebRTC offer SDP for a session.
     */
    public String getWebrtcOffer(String sessionId) {
        return getActiveSession(sessionId).getWebrtcOfferSdp();
    }

    /**
     * Get session statistics.
     */
    public SessionStats getStats() {
        int totalActive = activeSessions.size();
        int totalParticipants = activeSessions.values().stream()
                .mapToInt(s -> s.getParticipants().size())
                .sum();
        int totalSpaces = spaceSessions.size();
        return new SessionStats(totalActive, totalParticipants, totalSpaces);
    }

    private Session getActiveSession(String sessionId) {
        Session session = activeSessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return session;
    }

    private void cleanupSession(String sessionId, String spaceId) {
        activeSessions.remove(sessionId);
        Set<String> ids = spaceSessions.get(spaceId);
        if (ids != null) {
            ids.remove(sessionId);
            if (ids.isEmpty()) spaceSessions.remove(spaceId);
        }
    }

    private String generateOfferSdp(String sessionId) {
        // Mock WebRTC offer SDP (production would use a real SFU/TURN server)
        return "v=0\r\n" +
                "o=solra-" + sessionId.substring(0, 8) + " 0 1 IN IP4 0.0.0.0\r\n" +
                "s=Solra Session\r\n" +
                "t=0 0\r\n" +
                "a=group:BUNDLE audio video data\r\n" +
                "m=audio 9 UDP/TLS/RTP/SAVPF 111\r\n" +
                "c=IN IP4 0.0.0.0\r\n" +
                "a=rtpmap:111 opus/48000/2\r\n" +
                "m=video 9 UDP/TLS/RTP/SAVPF 96\r\n" +
                "c=IN IP4 0.0.0.0\r\n" +
                "a=rtpmap:96 VP8/90000\r\n" +
                "m=application 9 UDP/DTLS/SCTP webrtc-datachannel\r\n" +
                "c=IN IP4 0.0.0.0\r\n" +
                "a=sctp-port:5000\r\n";
    }

    // -- Inner types --
    public record SessionStats(int totalActiveSessions, int totalParticipants, int totalSpaces) {}
}
