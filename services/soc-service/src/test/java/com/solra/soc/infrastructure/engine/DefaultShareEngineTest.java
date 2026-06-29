package com.solra.soc.infrastructure.engine;

import com.solra.soc.domain.model.ShareSession;
import com.solra.soc.domain.model.ShareType;
import com.solra.soc.domain.repository.ShareSessionRepository;
import com.solra.soc.domain.service.ShareEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultShareEngine 单元测试")
class DefaultShareEngineTest {

    @Mock
    private ShareSessionRepository sessionRepo;

    private DefaultShareEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DefaultShareEngine(sessionRepo);
        lenient().when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ========== generateShareLink ==========

    @Nested
    @DisplayName("generateShareLink — 生成分享链接")
    class GenerateShareLinkTests {

        @Test
        @DisplayName("生成 SPACE 类型分享 → 创建 ShareSession")
        void generatesSpaceShare() {
            ShareSession result = engine.generateShareLink("space001", "userA", "SPACE");

            assertNotNull(result);
            assertEquals("space001", result.getSpaceId());
            assertEquals("userA", result.getSharerUserId());
            assertEquals(ShareType.SPACE, result.getShareType());
            assertNotNull(result.getShareCode());
            assertEquals(12, result.getShareCode().length());
            assertEquals(0, result.getClickCount());
            assertEquals(0, result.getConversionCount());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getExpiresAt());
            verify(sessionRepo).save(any());
        }

        @Test
        @DisplayName("生成 INVITE 类型分享")
        void generatesInviteShare() {
            ShareSession result = engine.generateShareLink("space002", "userB", "INVITE");

            assertEquals(ShareType.INVITE, result.getShareType());
        }

        @Test
        @DisplayName("生成 PROFILE 类型分享")
        void generatesProfileShare() {
            ShareSession result = engine.generateShareLink("space003", "userC", "PROFILE");

            assertEquals(ShareType.PROFILE, result.getShareType());
        }

        @Test
        @DisplayName("分享码为 12 位大写字母数字")
        void shareCodeFormat() {
            ShareSession result = engine.generateShareLink("s1", "u1", "SPACE");

            String code = result.getShareCode();
            assertEquals(12, code.length());
            assertTrue(code.matches("[A-Z0-9]+"));
        }

        @Test
        @DisplayName("有效期设置为 7 天")
        void expiresInSevenDays() {
            ShareSession result = engine.generateShareLink("s1", "u1", "SPACE");

            Instant expectedMin = Instant.now().plus(6, ChronoUnit.DAYS);
            Instant expectedMax = Instant.now().plus(8, ChronoUnit.DAYS);

            assertTrue(result.getExpiresAt().isAfter(expectedMin));
            assertTrue(result.getExpiresAt().isBefore(expectedMax));
        }
    }

    // ========== trackClick ==========

    @Nested
    @DisplayName("trackClick — 追踪点击")
    class TrackClickTests {

        @Test
        @DisplayName("有效分享码 → 记录点击并返回 ShareClick")
        void validCodeTracksClick() {
            ShareSession session = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "ABC123", Instant.now().plus(1, ChronoUnit.DAYS));
            when(sessionRepo.findByShareCode("ABC123")).thenReturn(Optional.of(session));
            doAnswer(inv -> null).when(sessionRepo).update(any());

            ShareEngine.VisitorInfo visitor = ShareEngine.VisitorInfo.of("v1", "1.2.3.4", "Chrome", "WEB");
            Optional<com.solra.soc.domain.model.ShareClick> result = engine.trackClick("ABC123", visitor);

            assertTrue(result.isPresent());
            assertEquals("s1", result.get().getShareId());
            assertEquals("v1", result.get().getVisitorUserId());
            assertEquals("Chrome", result.get().getUserAgent());
            assertEquals(1, session.getClickCount());
            verify(sessionRepo).update(session);
        }

        @Test
        @DisplayName("无效分享码 → 返回 empty")
        void invalidCodeReturnsEmpty() {
            when(sessionRepo.findByShareCode("INVALID")).thenReturn(Optional.empty());

            ShareEngine.VisitorInfo visitor = ShareEngine.VisitorInfo.of("v1", "1.2.3.4", "Chrome", "WEB");
            Optional<com.solra.soc.domain.model.ShareClick> result = engine.trackClick("INVALID", visitor);

            assertFalse(result.isPresent());
        }

        @Test
        @DisplayName("已过期分享 → 返回 empty")
        void expiredShareReturnsEmpty() {
            ShareSession session = new ShareSession("s2", "sp2", "u2",
                    ShareType.SPACE, "EXPIRED", Instant.now().minus(1, ChronoUnit.DAYS));
            when(sessionRepo.findByShareCode("EXPIRED")).thenReturn(Optional.of(session));

            ShareEngine.VisitorInfo visitor = ShareEngine.VisitorInfo.of("v2", "2.2.2.2", "Firefox", "WEB");
            Optional<com.solra.soc.domain.model.ShareClick> result = engine.trackClick("EXPIRED", visitor);

            assertFalse(result.isPresent());
            verify(sessionRepo, never()).update(any());
        }
    }

    // ========== trackConversion ==========

    @Nested
    @DisplayName("trackConversion — 追踪转化")
    class TrackConversionTests {

        @Test
        @DisplayName("有效分享码 → 记录转化并返回 true")
        void validCodeTracksConversion() {
            ShareSession session = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "CONV001", Instant.now().plus(1, ChronoUnit.DAYS));
            when(sessionRepo.findByShareCode("CONV001")).thenReturn(Optional.of(session));
            doAnswer(inv -> null).when(sessionRepo).update(any());

            boolean result = engine.trackConversion("CONV001", "newUser");

            assertTrue(result);
            assertEquals(1, session.getConversionCount());
            verify(sessionRepo).update(session);
        }

        @Test
        @DisplayName("无效分享码 → 返回 false")
        void invalidCodeReturnsFalse() {
            when(sessionRepo.findByShareCode("BAD")).thenReturn(Optional.empty());

            boolean result = engine.trackConversion("BAD", "newUser");

            assertFalse(result);
        }

        @Test
        @DisplayName("已过期分享 → 返回 false")
        void expiredShareReturnsFalse() {
            ShareSession session = new ShareSession("s2", "sp2", "u2",
                    ShareType.SPACE, "EXPIRED2", Instant.now().minus(1, ChronoUnit.DAYS));
            when(sessionRepo.findByShareCode("EXPIRED2")).thenReturn(Optional.of(session));

            boolean result = engine.trackConversion("EXPIRED2", "newUser");

            assertFalse(result);
        }
    }

    // ========== getViralChainStats ==========

    @Nested
    @DisplayName("getViralChainStats — 病毒传播统计")
    class GetViralChainStatsTests {

        @Test
        @DisplayName("有转化 → 计算转化率")
        void withConversions() {
            ShareSession session = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "VIRAL1", Instant.now().plus(1, ChronoUnit.DAYS));
            session.setClickCount(10);
            session.setConversionCount(3);
            when(sessionRepo.findByShareCode("VIRAL1")).thenReturn(Optional.of(session));

            Optional<ShareEngine.ViralStats> result = engine.getViralChainStats("VIRAL1");

            assertTrue(result.isPresent());
            assertEquals(10, result.get().totalClicks());
            assertEquals(3, result.get().totalConversions());
            assertEquals(0.3, result.get().conversionRate(), 0.001);
        }

        @Test
        @DisplayName("无点击 → 转化率 0.0")
        void noClicksZeroRate() {
            ShareSession session = new ShareSession("s1", "sp1", "u1",
                    ShareType.SPACE, "VIRAL2", Instant.now().plus(1, ChronoUnit.DAYS));
            when(sessionRepo.findByShareCode("VIRAL2")).thenReturn(Optional.of(session));

            Optional<ShareEngine.ViralStats> result = engine.getViralChainStats("VIRAL2");

            assertTrue(result.isPresent());
            assertEquals(0.0, result.get().conversionRate(), 0.001);
        }

        @Test
        @DisplayName("无效分享码 → 返回 empty")
        void invalidCodeReturnsEmpty() {
            when(sessionRepo.findByShareCode("NONE")).thenReturn(Optional.empty());

            Optional<ShareEngine.ViralStats> result = engine.getViralChainStats("NONE");

            assertFalse(result.isPresent());
        }
    }
}
