package com.solra.saf.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SafetyScore 值对象 单元测试")
class SafetyScoreTest {

    @Nested
    @DisplayName("safe — 安全评分工厂")
    class SafeTests {

        @Test
        @DisplayName("安全评分 overallScore = 1.0")
        void safeScoreIsOne() {
            SafetyScore s = SafetyScore.safe("v1");

            assertEquals(1.0f, s.getOverallScore(), 0.001);
            assertEquals("v1", s.getModelVersion());
            assertTrue(s.getCategoryScores().isEmpty());
        }

        @Test
        @DisplayName("安全评分在任意阈值下都判定为安全")
        void safeIsAlwaysSafe() {
            SafetyScore s = SafetyScore.safe("v1");

            assertTrue(s.isSafe(0.0f));
            assertTrue(s.isSafe(0.5f));
            assertTrue(s.isSafe(1.0f));
        }
    }

    @Nested
    @DisplayName("unsafe — 不安全评分工厂")
    class UnsafeTests {

        @Test
        @DisplayName("不安全评分记录各项分数")
        void unsafeScoreRecorded() {
            List<SafetyScore.CategoryScore> cats = List.of(
                    new SafetyScore.CategoryScore("ILLEGAL", 0.1f),
                    new SafetyScore.CategoryScore("SPAM", 0.2f)
            );
            SafetyScore s = SafetyScore.unsafe(0.3f, cats, "v2");

            assertEquals(0.3f, s.getOverallScore(), 0.001);
            assertEquals(2, s.getCategoryScores().size());
            assertEquals("v2", s.getModelVersion());
        }

        @Test
        @DisplayName("分数钳制：负值 → 0")
        void scoreClampedToZero() {
            SafetyScore s = SafetyScore.unsafe(-0.5f, List.of(), "v1");

            assertEquals(0.0f, s.getOverallScore(), 0.001);
        }

        @Test
        @DisplayName("分数钳制：超 1.0 → 1.0")
        void scoreClampedToOne() {
            SafetyScore s = SafetyScore.unsafe(2.5f, List.of(), "v1");

            assertEquals(1.0f, s.getOverallScore(), 0.001);
        }

        @Test
        @DisplayName("分数钳制：0.0 正常")
        void scoreZeroExactly() {
            SafetyScore s = SafetyScore.unsafe(0.0f, List.of(), "v1");

            assertEquals(0.0f, s.getOverallScore(), 0.001);
        }
    }

    @Nested
    @DisplayName("isSafe — 阈值判定")
    class IsSafeTests {

        @Test
        @DisplayName("分数 >= 阈值 → 安全")
        void scoreAboveThreshold() {
            SafetyScore s = SafetyScore.unsafe(0.8f, List.of(), "v1");

            assertTrue(s.isSafe(0.5f));
            assertTrue(s.isSafe(0.8f));
        }

        @Test
        @DisplayName("分数 < 阈值 → 不安全")
        void scoreBelowThreshold() {
            SafetyScore s = SafetyScore.unsafe(0.3f, List.of(), "v1");

            assertFalse(s.isSafe(0.5f));
        }
    }

    @Nested
    @DisplayName("CategoryScore — 分类评分记录")
    class CategoryScoreTests {

        @Test
        @DisplayName("创建并读取分类评分")
        void createAndRead() {
            SafetyScore.CategoryScore cs = new SafetyScore.CategoryScore("NSFW", 0.15f);

            assertEquals("NSFW", cs.category());
            assertEquals(0.15f, cs.score(), 0.001);
        }
    }
}
