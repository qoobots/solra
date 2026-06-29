package com.solra.saf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PolicyViolation 值对象 单元测试")
class PolicyViolationTest {

    @Nested
    @DisplayName("detected — 检测到违规工厂")
    class DetectedTests {

        @Test
        @DisplayName("创建违规记录包含所有字段")
        void createsViolationWithAllFields() {
            PolicyViolation v = PolicyViolation.detected("P001", "hate-speech",
                    PolicyViolation.Category.HATE_SPEECH,
                    PolicyViolation.Severity.HIGH,
                    "Hate speech detected in user content",
                    "i hate you");

            assertEquals("P001", v.getPolicyId());
            assertEquals("hate-speech", v.getPolicyName());
            assertEquals(PolicyViolation.Category.HATE_SPEECH, v.getCategory());
            assertEquals(PolicyViolation.Severity.HIGH, v.getSeverity());
            assertEquals("Hate speech detected in user content", v.getDescription());
            assertEquals("i hate you", v.getEvidence());
        }

        @Test
        @DisplayName("创建 CRITICAL 级别违规")
        void createsCriticalViolation() {
            PolicyViolation v = PolicyViolation.detected("P002", "self-harm",
                    PolicyViolation.Category.SELF_HARM,
                    PolicyViolation.Severity.CRITICAL,
                    "Self-harm content detected",
                    "kill myself");

            assertEquals(PolicyViolation.Severity.CRITICAL, v.getSeverity());
            assertEquals(PolicyViolation.Category.SELF_HARM, v.getCategory());
        }

        @Test
        @DisplayName("创建 LOW 级别违规")
        void createsLowViolation() {
            PolicyViolation v = PolicyViolation.detected("P003", "spam",
                    PolicyViolation.Category.SPAM,
                    PolicyViolation.Severity.LOW,
                    "Potential spam content",
                    "buy now click here");

            assertEquals(PolicyViolation.Severity.LOW, v.getSeverity());
        }
    }

    @Nested
    @DisplayName("枚举值验证")
    class EnumValidation {

        @Test
        @DisplayName("Category 包含 10 种类型")
        void categoryHasTenTypes() {
            assertEquals(10, PolicyViolation.Category.values().length);
        }

        @Test
        @DisplayName("Severity 有 4 个级别")
        void severityHasFourLevels() {
            assertEquals(4, PolicyViolation.Severity.values().length);
        }
    }
}
