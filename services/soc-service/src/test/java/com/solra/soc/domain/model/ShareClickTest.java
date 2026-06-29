package com.solra.soc.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ShareClick 值对象 单元测试")
class ShareClickTest {

    @Nested
    @DisplayName("构造 — 创建点击记录")
    class ConstructionTests {

        @Test
        @DisplayName("构造后初始状态正确")
        void initializesCorrectly() {
            ShareClick c = new ShareClick("c1", "s1", "v1",
                    "192.168.1.1", "Chrome/100", "WEB");

            assertEquals("c1", c.getClickId());
            assertEquals("s1", c.getShareId());
            assertEquals("v1", c.getVisitorUserId());
            assertEquals("192.168.1.1", c.getIpAddress());
            assertEquals("Chrome/100", c.getUserAgent());
            assertEquals("WEB", c.getPlatform());
            assertNotNull(c.getTimestamp());
            assertFalse(c.isConverted());
        }

        @Test
        @DisplayName("visitorUserId 可为空（匿名访客）")
        void anonymousVisitor() {
            ShareClick c = new ShareClick("c2", "s2", null,
                    "10.0.0.1", "Safari", "IOS");

            assertNull(c.getVisitorUserId());
        }
    }

    @Nested
    @DisplayName("markConverted — 标记转化")
    class MarkConvertedTests {

        @Test
        @DisplayName("标记后 isConverted 返回 true")
        void marksAsConverted() {
            ShareClick c = new ShareClick("c1", "s1", "v1",
                    "1.1.1.1", "UA", "WEB");

            assertFalse(c.isConverted());
            c.markConverted();
            assertTrue(c.isConverted());
        }
    }
}
