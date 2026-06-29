package com.solra.soc.application.dto;

import com.solra.soc.domain.model.FriendPresence;

import java.time.Instant;

/**
 * 好友在线状态 DTO。
 */
public record FriendPresenceDTO(
        String userId,
        String nickname,
        String avatarUrl,
        String onlineStatus,
        String currentSpaceId,
        String currentSpaceName,
        Instant lastSeenAt,
        boolean isOnline,
        boolean isInSameSpace
) {
    public static FriendPresenceDTO from(FriendPresence p, String currentSpaceId) {
        return new FriendPresenceDTO(
                p.getUserId(), p.getNickname(), p.getAvatarUrl(),
                p.getOnlineStatus() != null ? p.getOnlineStatus().name() : "OFFLINE",
                p.getCurrentSpaceId(), p.getCurrentSpaceName(),
                p.getLastSeenAt(), p.isOnline(),
                currentSpaceId != null && p.isInSameSpace(currentSpaceId));
    }
}
