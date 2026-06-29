package com.solra.soc.application.dto;

import java.util.List;

/**
 * 分享列表分页结果 DTO。
 */
public record ShareListDTO(
        List<ShareResultDTO> items,
        long totalCount,
        int page,
        int size
) {}
