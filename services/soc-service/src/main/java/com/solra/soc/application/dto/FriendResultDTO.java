package com.solra.soc.application.dto;

import com.solra.soc.domain.model.Friend;
import com.solra.soc.domain.model.FriendStatus;

import java.time.Instant;

/**
 * 好友关系结果 DTO — 不可变记录。
 */
public record FriendResultDTO(
        String friendshipId,
        String userId,
        String friendUserId,
        FriendStatus status,
        Instant createdAt,
        Instant acceptedAt
) {
    public static FriendResultDTO from(Friend f) {
        return new FriendResultDTO(
                f.getFriendshipId(), f.getUserId(), f.getFriendUserId(),
                f.getStatus(), f.getCreatedAt(), f.getAcceptedAt());
    }
}
