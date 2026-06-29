package com.solra.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "TestSecretKeyForUnitTestingMinLength32!!";
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        // 15 min access, 30 min refresh for test speed
        provider = new JwtTokenProvider(TEST_SECRET, 900_000, 1_800_000);
    }

    // ======================== Access Token ========================

    @Nested
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("应生成非空 access token")
        void shouldGenerateNonEmptyToken() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("应包含 subject 为 userId")
        void shouldContainSubjectAsUserId() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            String userId = provider.getUserIdFromToken(token);
            assertEquals("user-001", userId);
        }

        @Test
        @DisplayName("应包含 type=access claim")
        void shouldContainAccessTypeClaim() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isPresent());
            assertEquals("access", result.get().getPayload().get("type", String.class));
        }

        @Test
        @DisplayName("应包含 roles claim")
        void shouldContainRolesClaim() {
            List<String> roles = List.of("USER", "CREATOR");
            String token = provider.generateAccessToken("user-001", roles, null);
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isPresent());
            List<?> claimRoles = result.get().getPayload().get("roles", List.class);
            assertNotNull(claimRoles);
            assertTrue(claimRoles.contains("USER"));
            assertTrue(claimRoles.contains("CREATOR"));
        }

        @Test
        @DisplayName("应包含额外自定义 claims")
        void shouldContainExtraClaims() {
            Map<String, Object> extraClaims = Map.of("tenant", "org-42", "scope", "read");
            String token = provider.generateAccessToken("user-001", List.of("USER"), extraClaims);
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isPresent());
            Claims payload = result.get().getPayload();
            assertEquals("org-42", payload.get("tenant", String.class));
            assertEquals("read", payload.get("scope", String.class));
        }

        @Test
        @DisplayName("extraClaims 为 null 时不应抛异常")
        void shouldHandleNullExtraClaims() {
            assertDoesNotThrow(() -> {
                String token = provider.generateAccessToken("user-001", List.of("USER"), null);
                assertNotNull(provider.validateToken(token));
            });
        }

        @Test
        @DisplayName("应设置过期时间为 access token 过期时长")
        void shouldSetAccessTokenExpiry() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            // 未过期的 token 应验证通过
            assertFalse(provider.isTokenExpired(token));
        }

        @Test
        @DisplayName("不同 userId 应生成不同 token")
        void shouldGenerateDifferentTokensForDifferentUsers() {
            String token1 = provider.generateAccessToken("user-001", List.of("USER"), null);
            String token2 = provider.generateAccessToken("user-002", List.of("USER"), null);
            assertNotEquals(token1, token2);
            assertEquals("user-001", provider.getUserIdFromToken(token1));
            assertEquals("user-002", provider.getUserIdFromToken(token2));
        }
    }

    // ======================== Refresh Token ========================

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("应生成非空 refresh token")
        void shouldGenerateNonEmptyToken() {
            String token = provider.generateRefreshToken("user-001");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("应包含 type=refresh claim")
        void shouldContainRefreshTypeClaim() {
            String token = provider.generateRefreshToken("user-001");
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isPresent());
            assertEquals("refresh", result.get().getPayload().get("type", String.class));
        }

        @Test
        @DisplayName("应包含 jti (JWT ID)")
        void shouldContainJti() {
            String token = provider.generateRefreshToken("user-001");
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isPresent());
            assertNotNull(result.get().getPayload().getId());
        }

        @Test
        @DisplayName("不同调用应生成不同 jti")
        void shouldGenerateDifferentJti() {
            String token1 = provider.generateRefreshToken("user-001");
            String token2 = provider.generateRefreshToken("user-001");
            Optional<Jws<Claims>> r1 = provider.validateToken(token1);
            Optional<Jws<Claims>> r2 = provider.validateToken(token2);
            assertNotEquals(r1.get().getPayload().getId(), r2.get().getPayload().getId());
        }

        @Test
        @DisplayName("refresh token 应比 access token 有效期更长")
        void shouldHaveLongerExpiryThanAccessToken() {
            assertTrue(provider.getRefreshTokenExpirationMs() > provider.getAccessTokenExpirationMs());
        }
    }

    // ======================== validateToken ========================

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("有效 token 应返回非空 Optional")
        void shouldReturnPresentForValidToken() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("null token 应返回 empty")
        void shouldReturnEmptyForNullToken() {
            Optional<Jws<Claims>> result = provider.validateToken(null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("空字符串 token 应返回 empty")
        void shouldReturnEmptyForBlankToken() {
            Optional<Jws<Claims>> result = provider.validateToken("");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("篡改的 token 应返回 empty")
        void shouldReturnEmptyForTamperedToken() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            Optional<Jws<Claims>> result = provider.validateToken(tampered);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("不同签名密钥签发的 token 应返回 empty")
        void shouldReturnEmptyForDifferentKey() {
            JwtTokenProvider otherProvider = new JwtTokenProvider(
                    "AnotherSecretKeyForTestingMinLength32", 900_000, 1_800_000);
            String token = otherProvider.generateAccessToken("user-001", List.of("USER"), null);
            Optional<Jws<Claims>> result = provider.validateToken(token);
            assertTrue(result.isEmpty());
        }
    }

    // ======================== getUserIdFromToken ========================

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("应正确提取 userId")
        void shouldExtractUserId() {
            String token = provider.generateAccessToken("user-123", List.of("USER"), null);
            assertEquals("user-123", provider.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("refresh token 也应可提取 userId")
        void shouldExtractUserIdFromRefreshToken() {
            String token = provider.generateRefreshToken("user-456");
            assertEquals("user-456", provider.getUserIdFromToken(token));
        }

        @Test
        @DisplayName("无效 token 应抛出异常")
        void shouldThrowForInvalidToken() {
            assertThrows(Exception.class, () -> provider.getUserIdFromToken("invalid.token.here"));
        }
    }

    // ======================== isTokenExpired ========================

    @Nested
    @DisplayName("isTokenExpired")
    class IsTokenExpired {

        @Test
        @DisplayName("有效未过期 token 应返回 false")
        void shouldReturnFalseForValidUnexpiredToken() {
            String token = provider.generateAccessToken("user-001", List.of("USER"), null);
            assertFalse(provider.isTokenExpired(token));
        }

        @Test
        @DisplayName("已过期的 token 应返回 true")
        void shouldReturnTrueForExpiredToken() {
            // 使用 1ms 过期时间创建 provider
            JwtTokenProvider shortLived = new JwtTokenProvider(TEST_SECRET, 1, 1);
            String token = shortLived.generateAccessToken("user-001", List.of("USER"), null);
            // 等待 token 过期
            try { Thread.sleep(5); } catch (InterruptedException e) { /* ignore */ }
            assertTrue(shortLived.isTokenExpired(token));
        }

        @Test
        @DisplayName("无效 token 应返回 true")
        void shouldReturnTrueForInvalidToken() {
            assertTrue(provider.isTokenExpired("not.a.valid.token"));
        }

        @Test
        @DisplayName("null token 应返回 true")
        void shouldReturnTrueForNullToken() {
            assertTrue(provider.isTokenExpired(null));
        }
    }

    // ======================== Configuration ========================

    @Nested
    @DisplayName("配置值获取")
    class Configuration {

        @Test
        @DisplayName("getAccessTokenExpirationMs 应返回配置值")
        void shouldReturnConfiguredAccessExpiration() {
            assertEquals(900_000, provider.getAccessTokenExpirationMs());
        }

        @Test
        @DisplayName("getRefreshTokenExpirationMs 应返回配置值")
        void shouldReturnConfiguredRefreshExpiration() {
            assertEquals(1_800_000, provider.getRefreshTokenExpirationMs());
        }

        @Test
        @DisplayName("应支持自定义过期时间配置")
        void shouldSupportCustomExpiration() {
            JwtTokenProvider custom = new JwtTokenProvider(TEST_SECRET, 60000, 300000);
            assertEquals(60000, custom.getAccessTokenExpirationMs());
            assertEquals(300000, custom.getRefreshTokenExpirationMs());
        }
    }
}
