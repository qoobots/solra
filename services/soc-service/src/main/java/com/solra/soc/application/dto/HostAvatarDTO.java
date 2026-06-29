package com.solra.soc.application.dto;

import com.solra.soc.domain.model.HostAvatar;
import com.solra.soc.domain.service.HostAvatarService;

import java.time.Instant;
import java.util.List;

/**
 * 虚拟人主持人 DTO。
 */
public record HostAvatarDTO(
        String hostId,
        String sessionId,
        String avatarId,
        String avatarName,
        String mode,
        String state,
        String currentTopic,
        List<String> topicQueue,
        int topicQueueSize,
        List<String> speakerQueue,
        int speakerQueueSize,
        String activeSpeaker,
        int totalInteractions,
        Instant startedAt,
        Instant lastActivityAt
) {
    public static HostAvatarDTO from(HostAvatar h) {
        return new HostAvatarDTO(
                h.getHostId(), h.getSessionId(), h.getAvatarId(), h.getAvatarName(),
                h.getMode().name(), h.getState().name(),
                h.getCurrentTopic(),
                h.getTopicQueue(), h.getTopicQueueSize(),
                h.getSpeakerQueue(), h.getSpeakerQueueSize(),
                h.getActiveSpeaker(),
                h.getTotalInteractions(),
                h.getStartedAt(), h.getLastActivityAt());
    }
}

/** 主持人统计 DTO */
public record HostStatsDTO(
        String hostId,
        String sessionId,
        String mode,
        String state,
        String currentTopic,
        int topicQueueSize,
        int speakerQueueSize,
        int totalInteractions,
        long runningMinutes
) {
    public static HostStatsDTO from(HostAvatarService.HostStats s) {
        return new HostStatsDTO(s.hostId(), s.sessionId(), s.mode(), s.state(),
                s.currentTopic(), s.topicQueueSize(), s.speakerQueueSize(),
                s.totalInteractions(), s.runningMinutes());
    }
}
