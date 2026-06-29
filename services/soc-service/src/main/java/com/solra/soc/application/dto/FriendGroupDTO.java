package com.solra.soc.application.dto;

import com.solra.soc.domain.model.FriendGroup;

import java.time.Instant;
import java.util.List;

/**
 * 好友分组 DTO。
 */
public record FriendGroupDTO(
        String groupId,
        String groupName,
        int sortOrder,
        List<String> memberUserIds,
        int memberCount,
        Instant createdAt
) {
    public static FriendGroupDTO from(FriendGroup g) {
        return new FriendGroupDTO(
                g.getGroupId(), g.getGroupName(), g.getSortOrder(),
                g.getMemberUserIds(), g.getMemberUserIds().size(),
                g.getCreatedAt());
    }
}
