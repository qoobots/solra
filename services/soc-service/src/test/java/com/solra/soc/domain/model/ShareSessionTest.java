package com.solra.soc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShareSession 聚合根 单元测试")
class ShareSessionTest {

    @Nested
    @DisplayName("构造 — 创建分享会话")
    class ConstructionTests {

        @Test
        @DisplayName("构造后初始状态正确")
        void initializesCorrectly() {
            Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);
            ShareSession s = new ShareSession("s1", "sp1", "u1", ShareType.SPACE, "CODE123", expires);

            assertEquals("s1", s.getShareId());
            assertEquals("sp1", s.getSpaceId());
            assertEquals("u1", s.getSharerUserId());
            assertEquals(ShareType.SPACE, s.getShareType());
            assertEquals("CODE123", s.getShareCode());
            assertEquals(0, s.getClickCount());
            assertEquals(0, s.getConversionCount());
            assertEquals(ShareStatus.ACTIVE, s.getStatus());
            assertNotNull(s.getCreatedAt());
            assertNotNull(s.getViralChain());
            assertEquals(1, s.getViralChain().size()); // 分享者本人
            assertEquals("u1", s.getViralChain().get(0));
        }

        @Test
        @DisplayName("默认构造 viralChain 为空列表")
        void defaultConstructorEmptyChain() {
            ShareSession s = new ShareSession();
            assertNotNull(s.getViralChain());
            assertTrue(s.getViralChain().isEmpty());
        }
    }

    // ========== recordClick ==========

    @Nested
    @DisplayName("recordClick — 记录点击")
    class RecordClickTests {

        @Test
        @DisplayName("点击计数递增")
        void incrementsClickCount() {
            ShareSession s = createActiveSession();

            s.recordClick("v1");
            assertEquals(1, s.getClickCount());

            s.recordClick("v2");
            assertEquals(2, s.getClickCount());
        }

        @Test
        @DisplayName("新访客追加到传播链")
        void newVisitorAppendedToChain() {
            ShareSession s = createActiveSession();

            s.recordClick("visitor1");
            assertTrue(s.getViralChain().contains("visitor1"));
            assertEquals(2, s.getViralChain().size()); // sharer + visitor1
        }

        @Test
        @DisplayName("重复访客不重复追加")
        void duplicateVisitorNotAppended() {
            ShareSession s = createActiveSession();

            s.recordClick("v1");
            s.recordClick("v1");
            s.recordClick("v1");

            assertEquals(3, s.getClickCount());
            assertEquals(2, s.getViralChain().size()); // sharer + v1 (only once)
        }

        @Test
        @DisplayName("null 访客不追加到链")
        void nullVisitorNotAppended() {
            ShareSession s = createActiveSession();

            s.recordClick(null);
            assertEquals(1, s.getClickCount());
            assertEquals(1, s.getViralChain().size());
        }

        @Test
        @DisplayName("空白访客不追加到链")
        void blankVisitorNotAppended() {
            ShareSession s = createActiveSession();

            s.recordClick("   ");
            assertEquals(1, s.getClickCount());
            assertEquals(1, s.getViralChain().size());
        }

        @Test
        @DisplayName("过期时自动更新状态为 EXPIRED")
        void expiredAutoUpdates() {
            ShareSession s = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "CODE", Instant.now().minus(1, ChronoUnit.HOURS));

            s.recordClick("v1");

            assertEquals(ShareStatus.EXPIRED, s.getStatus());
        }
    }

    // ========== recordConversion ==========

    @Nested
    @DisplayName("recordConversion — 记录转化")
    class RecordConversionTests {

        @Test
        @DisplayName("转化计数递增")
        void incrementsConversionCount() {
            ShareSession s = createActiveSession();

            s.recordConversion("newUser");
            assertEquals(1, s.getConversionCount());
        }

        @Test
        @DisplayName("转化用户追加到传播链")
        void conversionUserAppendedToChain() {
            ShareSession s = createActiveSession();

            s.recordConversion("convertedUser");
            assertTrue(s.getViralChain().contains("convertedUser"));
        }
    }

    // ========== isExpired ==========

    @Nested
    @DisplayName("isExpired — 过期判断")
    class IsExpiredTests {

        @Test
        @DisplayName("未过期且 ACTIVE → false")
        void activeNotExpired() {
            ShareSession s = createActiveSession();

            assertFalse(s.isExpired());
        }

        @Test
        @DisplayName("已过时间 → true，状态自动变为 EXPIRED")
        void pastExpiryReturnsTrue() {
            ShareSession s = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "CODE", Instant.now().minus(1, ChronoUnit.HOURS));

            assertTrue(s.isExpired());
            assertEquals(ShareStatus.EXPIRED, s.getStatus());
        }

        @Test
        @DisplayName("手动设为 EXPIRED → true")
        void manuallyExpiredReturnsTrue() {
            ShareSession s = createActiveSession();
            s.expire();

            assertTrue(s.isExpired());
        }

        @Test
        @DisplayName("无过期时间永不过期")
        void noExpiryNeverExpires() {
            ShareSession s = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "CODE", null);

            assertFalse(s.isExpired());
        }
    }

    // ========== expire / consume ==========

    @Nested
    @DisplayName("expire / consume — 状态变更")
    class StateChangeTests {

        @Test
        @DisplayName("ACTIVE → expire() → EXPIRED")
        void activeToExpired() {
            ShareSession s = createActiveSession();
            s.expire();
            assertEquals(ShareStatus.EXPIRED, s.getStatus());
        }

        @Test
        @DisplayName("ACTIVE → consume() → CONSUMED")
        void activeToConsumed() {
            ShareSession s = createActiveSession();
            s.consume();
            assertEquals(ShareStatus.CONSUMED, s.getStatus());
        }

        @Test
        @DisplayName("已 EXPIRED 再 expire 不改变状态")
        void alreadyExpiredNoChange() {
            ShareSession s = createActiveSession();
            s.expire();
            s.expire();
            assertEquals(ShareStatus.EXPIRED, s.getStatus());
        }
    }

    // ========== 帮助方法 ==========

    private ShareSession createActiveSession() {
        return new ShareSession("s1", "sp1", "u1",
                ShareType.SPACE, "CODE001", Instant.now().plus(7, ChronoUnit.DAYS));
    }
}
