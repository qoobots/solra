package com.solra.soc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Friend 实体 单元测试")
class FriendTest {

    @Nested
    @DisplayName("构造 — 创建好友关系")
    class ConstructionTests {

        @Test
        @DisplayName("新好友关系初始状态为 PENDING")
        void createsWithPendingStatus() {
            Friend f = new Friend("f1", "userA", "userB");

            assertEquals("f1", f.getFriendshipId());
            assertEquals("userA", f.getUserId());
            assertEquals("userB", f.getFriendUserId());
            assertEquals(FriendStatus.PENDING, f.getStatus());
            assertNotNull(f.getCreatedAt());
            assertNull(f.getAcceptedAt());
        }

        @Test
        @DisplayName("isPending 返回 true")
        void isPendingTrue() {
            Friend f = new Friend("f1", "userA", "userB");
            assertTrue(f.isPending());
            assertFalse(f.isAccepted());
            assertFalse(f.isBlocked());
        }
    }

    @Nested
    @DisplayName("accept — 接受好友请求")
    class AcceptTests {

        @Test
        @DisplayName("PENDING → 接受 → ACCEPTED")
        void pendingToAccepted() {
            Friend f = new Friend("f1", "userA", "userB");
            f.accept();

            assertEquals(FriendStatus.ACCEPTED, f.getStatus());
            assertNotNull(f.getAcceptedAt());
            assertTrue(f.isAccepted());
        }

        @Test
        @DisplayName("非 PENDING 状态接受抛出异常")
        void nonPendingThrowsOnAccept() {
            Friend f = new Friend("f1", "userA", "userB");
            f.accept();

            assertThrows(IllegalStateException.class, f::accept);
        }

        @Test
        @DisplayName("BLOCKED 状态接受抛出异常")
        void blockedThrowsOnAccept() {
            Friend f = new Friend("f1", "userA", "userB");
            f.block();

            assertThrows(IllegalStateException.class, f::accept);
        }
    }

    @Nested
    @DisplayName("block / unblock — 拉黑与取消拉黑")
    class BlockUnblockTests {

        @Test
        @DisplayName("PENDING → block → BLOCKED")
        void pendingToBlocked() {
            Friend f = new Friend("f1", "userA", "userB");
            f.block();

            assertEquals(FriendStatus.BLOCKED, f.getStatus());
            assertTrue(f.isBlocked());
        }

        @Test
        @DisplayName("ACCEPTED → block → BLOCKED")
        void acceptedToBlocked() {
            Friend f = new Friend("f1", "userA", "userB");
            f.accept();
            f.block();

            assertEquals(FriendStatus.BLOCKED, f.getStatus());
        }

        @Test
        @DisplayName("BLOCKED → unblock → ACCEPTED")
        void blockedToAccepted() {
            Friend f = new Friend("f1", "userA", "userB");
            f.block();
            f.unblock();

            assertEquals(FriendStatus.ACCEPTED, f.getStatus());
        }

        @Test
        @DisplayName("非 BLOCKED 状态 unblock 无效果")
        void unblockNonBlockedNoEffect() {
            Friend f = new Friend("f1", "userA", "userB");
            f.accept();
            f.unblock();

            assertEquals(FriendStatus.ACCEPTED, f.getStatus());
        }
    }
}
