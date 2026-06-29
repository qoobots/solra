package com.solra.soc.application.dto;

import com.solra.soc.domain.model.Participant;
import com.solra.soc.domain.model.Session;
import com.solra.soc.domain.model.SessionConfig;

import java.time.Instant;
import java.util.List;

/**
 * SessionDTO — SOC-001 会话数据传输对象。
 */
public record SessionDTO(
        String sessionId,
        String spaceId,
        String type,
        String status,
        List<ParticipantDTO> participants,
        int maxParticipants,
        String hostId,
        Instant startedAt,
        Instant endedAt,
        SessionConfigDTO config,
        String webrtcOfferSdp,
        long sequenceNumber,
        int onlineCount
) {
    public static SessionDTO from(Session session) {
        return new SessionDTO(
                session.getSessionId(),
                session.getSpaceId(),
                session.getType().name(),
                session.getStatus().name(),
                session.getParticipants().stream().map(ParticipantDTO::from).toList(),
                session.getMaxParticipants(),
                session.getHostId(),
                session.getStartedAt(),
                session.getEndedAt(),
                SessionConfigDTO.from(session.getConfig()),
                session.getWebrtcOfferSdp(),
                session.getSequenceNumber(),
                session.getOnlineCount()
        );
    }

    public record ParticipantDTO(
            String userId,
            String sessionId,
            String avatarId,
            String status,
            Instant joinedAt,
            Instant lastActiveAt,
            boolean microphoneOn,
            boolean cameraOn
    ) {
        public static ParticipantDTO from(Participant p) {
            return new ParticipantDTO(
                    p.getUserId(),
                    p.getSessionId(),
                    p.getAvatarId(),
                    p.getStatus().name(),
                    p.getJoinedAt(),
                    p.getLastActiveAt(),
                    p.isMicrophoneOn(),
                    p.isCameraOn()
            );
        }
    }

    public record SessionConfigDTO(
            boolean voiceChatEnabled,
            boolean gestureEnabled,
            boolean screenShareEnabled,
            boolean recordSession,
            List<String> entryApprovalUserIds
    ) {
        public static SessionConfigDTO from(SessionConfig config) {
            return new SessionConfigDTO(
                    config.isVoiceChatEnabled(),
                    config.isGestureEnabled(),
                    config.isScreenShareEnabled(),
                    config.isRecordSession(),
                    config.getEntryApprovalUserIds()
            );
        }
    }
}
