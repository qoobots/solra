package com.solra.soc.application.dto;

import com.solra.soc.domain.service.FriendService;

/**
 * 好友统计 DTO。
 */
public record FriendStatsDTO(
        long totalFriends,
        long onlineFriends,
        long inSameSpace,
        int groupCount
) {
    public static FriendStatsDTO from(FriendService.FriendStats s) {
        return new FriendStatsDTO(s.totalFriends(), s.onlineFriends(), s.inSameSpace(), s.groupCount());
    }
}
