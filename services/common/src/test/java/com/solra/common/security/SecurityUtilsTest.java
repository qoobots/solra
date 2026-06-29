package com.solra.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SecurityUtils 单元测试")
class SecurityUtilsTest {

    // ======================== 工具类不可实例化 ========================

    @Test
    @DisplayName("构造函数应抛出 UnsupportedOperationException")
    void shouldNotAllowInstantiation() throws Exception {
        Constructor<SecurityUtils> constructor = SecurityUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(UnsupportedOperationException.class, () -> {
            try {
                constructor.newInstance();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        });
    }

    // ======================== generateSecureToken ========================

    @Nested
    @DisplayName("generateSecureToken")
    class GenerateSecureToken {

        @Test
        @DisplayName("应生成非空 token")
        void shouldGenerateNonEmptyToken() {
            String token = SecurityUtils.generateSecureToken(16);
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("应生成不含填充的 Base64URL token")
        void shouldBeBase64UrlWithoutPadding() {
            String token = SecurityUtils.generateSecureToken(16);
            assertFalse(token.contains("="), "不应包含 Base64 填充字符");
            assertTrue(token.matches("^[A-Za-z0-9_-]+$"), "应仅包含 Base64URL 字符");
        }

        @Test
        @DisplayName("不同调用应生成不同 token")
        void shouldGenerateDifferentTokens() {
            String token1 = SecurityUtils.generateSecureToken(16);
            String token2 = SecurityUtils.generateSecureToken(16);
            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("byteLength=32 应产生更长 token")
        void shouldRespectByteLength() {
            String shortToken = SecurityUtils.generateSecureToken(8);
            String longToken = SecurityUtils.generateSecureToken(32);
            assertTrue(longToken.length() > shortToken.length());
        }
    }

    // ======================== generateVerificationCode ========================

    @Nested
    @DisplayName("generateVerificationCode")
    class GenerateVerificationCode {

        @Test
        @DisplayName("应生成指定位数的验证码")
        void shouldGenerateCodeWithSpecifiedDigits() {
            String code = SecurityUtils.generateVerificationCode(6);
            assertEquals(6, code.length());
        }

        @Test
        @DisplayName("应仅包含数字字符")
        void shouldContainOnlyDigits() {
            String code = SecurityUtils.generateVerificationCode(4);
            assertTrue(code.matches("^\\d+$"));
        }

        @Test
        @DisplayName("digits=4 应生成 4 位验证码")
        void shouldGenerate4DigitCode() {
            assertEquals(4, SecurityUtils.generateVerificationCode(4).length());
        }

        @Test
        @DisplayName("digits=8 应生成 8 位验证码")
        void shouldGenerate8DigitCode() {
            assertEquals(8, SecurityUtils.generateVerificationCode(8).length());
        }

        @Test
        @DisplayName("不同调用应生成不同验证码")
        void shouldGenerateDifferentCodes() {
            // 小概率相同，但多次调用应有变化
            String code1 = SecurityUtils.generateVerificationCode(6);
            String code2 = SecurityUtils.generateVerificationCode(6);
            String code3 = SecurityUtils.generateVerificationCode(6);
            assertTrue(!code1.equals(code2) || !code2.equals(code3) || !code1.equals(code3),
                    "三次调用不应全部相同");
        }
    }

    // ======================== generateSessionId ========================

    @Nested
    @DisplayName("generateSessionId")
    class GenerateSessionId {

        @Test
        @DisplayName("应生成非空 session ID")
        void shouldGenerateNonEmptySessionId() {
            String sessionId = SecurityUtils.generateSessionId();
            assertNotNull(sessionId);
            assertFalse(sessionId.isBlank());
        }

        @Test
        @DisplayName("不同调用应生成不同 session ID")
        void shouldGenerateUniqueSessionIds() {
            String id1 = SecurityUtils.generateSessionId();
            String id2 = SecurityUtils.generateSessionId();
            assertNotEquals(id1, id2);
        }
    }

    // ======================== generateUserId ========================

    @Nested
    @DisplayName("generateUserId")
    class GenerateUserId {

        @Test
        @DisplayName("应以 usr_ 前缀开头")
        void shouldStartWithUsrPrefix() {
            String userId = SecurityUtils.generateUserId();
            assertTrue(userId.startsWith("usr_"));
        }

        @Test
        @DisplayName("应生成非空 user ID")
        void shouldGenerateNonEmptyUserId() {
            String userId = SecurityUtils.generateUserId();
            assertNotNull(userId);
            assertTrue(userId.length() > 4);
        }

        @Test
        @DisplayName("不同调用应生成不同 user ID")
        void shouldGenerateUniqueUserIds() {
            String id1 = SecurityUtils.generateUserId();
            String id2 = SecurityUtils.generateUserId();
            assertNotEquals(id1, id2);
        }
    }

    // ======================== hashSensitive ========================

    @Nested
    @DisplayName("hashSensitive")
    class HashSensitive {

        @Test
        @DisplayName("应生成非空哈希")
        void shouldGenerateNonEmptyHash() {
            String hash = SecurityUtils.hashSensitive("hello", "salt123");
            assertNotNull(hash);
            assertFalse(hash.isBlank());
        }

        @Test
        @DisplayName("相同输入+相同盐值应产生相同哈希")
        void shouldBeDeterministic() {
            String h1 = SecurityUtils.hashSensitive("data", "salt");
            String h2 = SecurityUtils.hashSensitive("data", "salt");
            assertEquals(h1, h2);
        }

        @Test
        @DisplayName("不同输入应产生不同哈希")
        void shouldDifferForDifferentData() {
            String h1 = SecurityUtils.hashSensitive("data1", "salt");
            String h2 = SecurityUtils.hashSensitive("data2", "salt");
            assertNotEquals(h1, h2);
        }

        @Test
        @DisplayName("不同盐值应产生不同哈希")
        void shouldDifferForDifferentSalt() {
            String h1 = SecurityUtils.hashSensitive("data", "salt1");
            String h2 = SecurityUtils.hashSensitive("data", "salt2");
            assertNotEquals(h1, h2);
        }

        @Test
        @DisplayName("空数据+盐值应能正常哈希")
        void shouldHandleEmptyData() {
            assertDoesNotThrow(() -> {
                String hash = SecurityUtils.hashSensitive("", "salt");
                assertNotNull(hash);
            });
        }
    }

    // ======================== maskPhoneNumber ========================

    @Nested
    @DisplayName("maskPhoneNumber")
    class MaskPhoneNumber {

        @Test
        @DisplayName("标准手机号应脱敏为 138****5678 格式")
        void shouldMaskStandardPhone() {
            assertEquals("138****5678", SecurityUtils.maskPhoneNumber("13812345678"));
        }

        @Test
        @DisplayName("null 应返回 ***")
        void shouldReturnAsterisksForNull() {
            assertEquals("***", SecurityUtils.maskPhoneNumber(null));
        }

        @Test
        @DisplayName("长度小于 7 的号码应返回 ***")
        void shouldReturnAsterisksForShortNumber() {
            assertEquals("***", SecurityUtils.maskPhoneNumber("123456"));
            assertEquals("***", SecurityUtils.maskPhoneNumber("12345"));
            assertEquals("***", SecurityUtils.maskPhoneNumber(""));
        }

        @Test
        @DisplayName("长度正好为 7 的号码应正常脱敏")
        void shouldMask7CharPhone() {
            String result = SecurityUtils.maskPhoneNumber("1234567");
            assertEquals("123****4567", result);
        }

        @Test
        @DisplayName("较长号码应正常脱敏")
        void shouldMaskLongPhone() {
            String result = SecurityUtils.maskPhoneNumber("19987654321");
            assertEquals("199****4321", result);
        }
    }

    // ======================== isValidChinesePhone ========================

    @Nested
    @DisplayName("isValidChinesePhone")
    class IsValidChinesePhone {

        @Test
        @DisplayName("有效中国手机号应返回 true")
        void shouldReturnTrueForValidPhone() {
            assertTrue(SecurityUtils.isValidChinesePhone("13812345678"));
            assertTrue(SecurityUtils.isValidChinesePhone("15912345678"));
            assertTrue(SecurityUtils.isValidChinesePhone("18812345678"));
            assertTrue(SecurityUtils.isValidChinesePhone("19912345678"));
        }

        @Test
        @DisplayName("null 应返回 false")
        void shouldReturnFalseForNull() {
            assertFalse(SecurityUtils.isValidChinesePhone(null));
        }

        @Test
        @DisplayName("非 1 开头的号码应返回 false")
        void shouldReturnFalseForNonOneStart() {
            assertFalse(SecurityUtils.isValidChinesePhone("23812345678"));
        }

        @Test
        @DisplayName("第二位非 3-9 的号码应返回 false")
        void shouldReturnFalseForInvalidSecondDigit() {
            assertFalse(SecurityUtils.isValidChinesePhone("12012345678"));
            assertFalse(SecurityUtils.isValidChinesePhone("11012345678"));
            assertFalse(SecurityUtils.isValidChinesePhone("10012345678"));
        }

        @Test
        @DisplayName("位数不足应返回 false")
        void shouldReturnFalseForShortNumber() {
            assertFalse(SecurityUtils.isValidChinesePhone("1381234567"));
            assertFalse(SecurityUtils.isValidChinesePhone("138123456"));
        }

        @Test
        @DisplayName("位数过多应返回 false")
        void shouldReturnFalseForLongNumber() {
            assertFalse(SecurityUtils.isValidChinesePhone("138123456789"));
        }

        @Test
        @DisplayName("含非数字字符应返回 false")
        void shouldReturnFalseForNonDigitChars() {
            assertFalse(SecurityUtils.isValidChinesePhone("1381234567a"));
            assertFalse(SecurityUtils.isValidChinesePhone("138-1234-5678"));
        }
    }

    // ======================== isValidEmail ========================

    @Nested
    @DisplayName("isValidEmail")
    class IsValidEmail {

        @Test
        @DisplayName("有效邮箱应返回 true")
        void shouldReturnTrueForValidEmail() {
            assertTrue(SecurityUtils.isValidEmail("user@example.com"));
            assertTrue(SecurityUtils.isValidEmail("user.name@example.com"));
            assertTrue(SecurityUtils.isValidEmail("user_name@example.co.uk"));
            assertTrue(SecurityUtils.isValidEmail("user+tag@example.org"));
        }

        @Test
        @DisplayName("null 应返回 false")
        void shouldReturnFalseForNull() {
            assertFalse(SecurityUtils.isValidEmail(null));
        }

        @Test
        @DisplayName("缺少 @ 应返回 false")
        void shouldReturnFalseForMissingAt() {
            assertFalse(SecurityUtils.isValidEmail("userexample.com"));
        }

        @Test
        @DisplayName("缺少域名应返回 false")
        void shouldReturnFalseForMissingDomain() {
            assertFalse(SecurityUtils.isValidEmail("user@"));
            assertFalse(SecurityUtils.isValidEmail("user@.com"));
        }

        @Test
        @DisplayName("缺少用户名应返回 false")
        void shouldReturnFalseForMissingUsername() {
            assertFalse(SecurityUtils.isValidEmail("@example.com"));
        }

        @Test
        @DisplayName("空字符串应返回 false")
        void shouldReturnFalseForEmptyString() {
            assertFalse(SecurityUtils.isValidEmail(""));
        }
    }
}
