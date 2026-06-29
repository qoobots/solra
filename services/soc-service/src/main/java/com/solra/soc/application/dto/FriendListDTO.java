package com.solra.soc.application.dto;

import java.util.List;

/**
 * 好友列表分页结果 DTO。
 */
public record FriendListDTO(
        List<FriendResultDTO> items,
        long totalCount,
        int page,
        int size
) {}
