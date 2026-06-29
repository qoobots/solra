package com.solra.soc.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

/**
 * FriendPresence 值对象单元测试。
 */
class FriendPresenceTest {

    @Test
    void shouldBeOnlineWhenStatusIsOnline() {
        FriendPresence p = new FriendPresence("u1", "Alice", null,
                OnlineStatus.ONLINE, "space1", "Fun Space", Instant.now());
        assertTrue(p.isOnline());
    }

    @Test
    void shouldBeOfflineWhenStatusIsOffline() {
        FriendPresence p = new FriendPresence("u1", "Alice", null,
                OnlineStatus.OFFLINE, null, null, Instant.now());
        assertFalse(p.isOnline());
    }

    @Test
    void shouldDetectSameSpace() {
        FriendPresence p = new FriendPresence("u1", "Alice", null,
                OnlineStatus.ONLINE, "space1", "Fun Space", Instant.now());
        assertTrue(p.isInSameSpace("space1"));
        assertFalse(p.isInSameSpace("space2"));
    }

    @Test
    void shouldUpdateOnlineStatusToOnline() {
        FriendPresence p = new FriendPresence("u1", "Alice", null,
                OnlineStatus.OFFLINE, null, null, Instant.now());
        p.updateOnlineStatus(OnlineStatus.ONLINE, "space1", "Fun Space");

        assertTrue(p.isOnline());
        assertEquals("space1", p.getCurrentSpaceId());
        assertEquals("Fun Space", p.getCurrentSpaceName());
    }

    @Test
    void shouldUpdateOnlineStatusToOffline() {
        FriendPresence p = new FriendPresence("u1", "Alice", null,
                OnlineStatus.ONLINE, "space1", "Fun Space", Instant.now());
        Instant before = p.getLastSeenAt();
        p.updateOnlineStatus(OnlineStatus.OFFLINE, null, null);

        assertFalse(p.isOnline());
        assertNotNull(p.getLastSeenAt());
    }

    @Test
    void shouldReturnNullForIsInSameSpaceWhenNoSpace() {
        FriendPresence p = new FriendPresence("u1", "Alice", null,
                OnlineStatus.OFFLINE, null, null, Instant.now());
        assertFalse(p.isInSameSpace("any-space"));
    }
}
