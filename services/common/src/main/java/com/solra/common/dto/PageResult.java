package com.solra.common.dto;

import java.util.List;

/**
 * Unified pagination result for all Solra services.
 */
public record PageResult<T>(
    List<T> items,
    int total,
    boolean hasMore,
    String nextCursor
) {
    public static <T> PageResult<T> of(List<T> items, int total, boolean hasMore, String nextCursor) {
        return new PageResult<>(items, total, hasMore, nextCursor);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0, false, null);
    }
}
