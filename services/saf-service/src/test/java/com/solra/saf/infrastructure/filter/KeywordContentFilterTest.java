package com.solra.saf.infrastructure.filter;

import com.solra.saf.domain.model.ContentType;
import com.solra.saf.domain.model.PolicyViolation;
import com.solra.saf.domain.service.ContentFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KeywordContentFilter 单元测试")
class KeywordContentFilterTest {

    private KeywordContentFilter filter;

    @BeforeEach
    void setUp() {
        filter = new KeywordContentFilter();
    }

    // ========== supports ==========

    @Nested
    @DisplayName("supports — 内容类型支持判断")
    class SupportsTests {

        @Test
        @DisplayName("支持 TEXT 类型")
        void supportsText() {
            assertTrue(filter.supports(ContentType.TEXT));
        }

        @Test
        @DisplayName("支持 AVATAR_SPEECH 类型")
        void supportsAvatarSpeech() {
            assertTrue(filter.supports(ContentType.AVATAR_SPEECH));
        }

        @Test
        @DisplayName("支持 SPACE_NAME 类型")
        void supportsSpaceName() {
            assertTrue(filter.supports(ContentType.SPACE_NAME));
        }

        @Test
        @DisplayName("支持 SPACE_DESCRIPTION 类型")
        void supportsSpaceDescription() {
            assertTrue(filter.supports(ContentType.SPACE_DESCRIPTION));
        }

        @Test
        @DisplayName("支持 USER_PROFILE 类型")
        void supportsUserProfile() {
            assertTrue(filter.supports(ContentType.USER_PROFILE));
        }

        @Test
        @DisplayName("不支持 IMAGE 类型")
        void notSupportsImage() {
            assertFalse(filter.supports(ContentType.IMAGE));
        }

        @Test
        @DisplayName("不支持 VIDEO 类型")
        void notSupportsVideo() {
            assertFalse(filter.supports(ContentType.VIDEO));
        }

        @Test
        @DisplayName("不支持 AUDIO 类型")
        void notSupportsAudio() {
            assertFalse(filter.supports(ContentType.AUDIO));
        }
    }

    // ========== filter ==========

    @Nested
    @DisplayName("filter — 内容过滤")
    class FilterTests {

        @Test
        @DisplayName("安全内容通过 → 评分 1.0")
        void safeContentPasses() {
            ContentFilter.FilterResult result = filter.filter("Hello, this is a normal message.");

            assertTrue(result.passed());
            assertEquals(1.0f, result.score().getOverallScore(), 0.001);
            assertTrue(result.violations().isEmpty());
        }

        @Test
        @DisplayName("null 内容返回安全")
        void nullContentSafe() {
            ContentFilter.FilterResult result = filter.filter(null);

            assertTrue(result.passed());
            assertEquals(1.0f, result.score().getOverallScore(), 0.001);
        }

        @Test
        @DisplayName("空白内容返回安全")
        void blankContentSafe() {
            ContentFilter.FilterResult result = filter.filter("   ");

            assertTrue(result.passed());
            assertEquals(1.0f, result.score().getOverallScore(), 0.001);
        }

        @Test
        @DisplayName("检测到 self-harm 关键词 → CRITICAL 违规，评分 0.0")
        void detectsSelfHarm() {
            ContentFilter.FilterResult result = filter.filter("i want to kill myself");

            assertFalse(result.passed());
            assertEquals(0.0f, result.score().getOverallScore(), 0.001);
            assertTrue(result.violations().stream()
                    .anyMatch(v -> v.getCategory() == PolicyViolation.Category.SELF_HARM));
        }

        @Test
        @DisplayName("检测到 hack 关键词 → ILLEGAL 违规")
        void detectsHackKeyword() {
            ContentFilter.FilterResult result = filter.filter("how to hack the server");

            assertFalse(result.passed());
            assertTrue(result.violations().stream()
                    .anyMatch(v -> v.getCategory() == PolicyViolation.Category.ILLEGAL));
        }

        @Test
        @DisplayName("检测到信用卡号模式 → PERSONAL_INFO 违规")
        void detectsCreditCardPattern() {
            ContentFilter.FilterResult result = filter.filter("my card number is 1234567890123456");

            assertFalse(result.passed());
            assertTrue(result.violations().stream()
                    .anyMatch(v -> v.getCategory() == PolicyViolation.Category.PERSONAL_INFO));
        }

        @Test
        @DisplayName("检测到 fraud 关键词 → FRAUD 违规，评分 0.2（非CRITICAL）")
        void detectsFraudKeyword() {
            ContentFilter.FilterResult result = filter.filter("this is a scam operation");

            assertFalse(result.passed());
            assertEquals(0.2f, result.score().getOverallScore(), 0.001);
            assertTrue(result.violations().stream()
                    .anyMatch(v -> v.getCategory() == PolicyViolation.Category.FRAUD));
        }

        @Test
        @DisplayName("检测到 harass 关键词 → HARASSMENT 违规")
        void detectsHarassmentKeyword() {
            ContentFilter.FilterResult result = filter.filter("i will bully you");

            assertFalse(result.passed());
            assertTrue(result.violations().stream()
                    .anyMatch(v -> v.getCategory() == PolicyViolation.Category.HARASSMENT));
        }

        @Test
        @DisplayName("多违规混合：CRITICAL 优先 → 评分 0.0")
        void multipleViolationsCriticalWins() {
            ContentFilter.FilterResult result = filter.filter("self harm and fraud and scam");

            assertFalse(result.passed());
            // SELF_HARM is CRITICAL → score should be 0.0
            assertEquals(0.0f, result.score().getOverallScore(), 0.001);
            assertTrue(result.violations().size() >= 2);
        }

        @Test
        @DisplayName("大小写不敏感匹配")
        void caseInsensitiveMatch() {
            ContentFilter.FilterResult result = filter.filter("HACK the SYSTEM with MALWARE");

            assertFalse(result.passed());
            assertTrue(result.violations().stream()
                    .anyMatch(v -> v.getCategory() == PolicyViolation.Category.ILLEGAL));
        }

        @Test
        @DisplayName("违规详情包含证据文本")
        void violationContainsEvidence() {
            ContentFilter.FilterResult result = filter.filter("this is spam");

            assertFalse(result.passed());
            PolicyViolation v = result.violations().get(0);
            assertNotNull(v.getEvidence());
            assertNotNull(v.getDescription());
            assertNotNull(v.getPolicyId());
        }
    }
}
