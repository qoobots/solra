package com.solra.spc.domain.event;

import java.time.Instant;

public final class SpcDomainEvents {
    private SpcDomainEvents() {}

    public record SpaceEntered(String spaceId, String userId, Instant at) {}
    public record SpaceExited(String spaceId, String userId, long dwellMs, Instant at) {}
    public record SpaceLiked(String spaceId, String userId, Instant at) {}
    public record SpaceShared(String spaceId, String userId, String shareChannel, Instant at) {}
    public record SpaceViewed(String spaceId, String userId, Instant at) {}
}
