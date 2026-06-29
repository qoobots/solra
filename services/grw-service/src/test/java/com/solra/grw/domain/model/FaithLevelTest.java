package com.solra.grw.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FaithLevel 枚举 单元测试")
class FaithLevelTest {

    @Nested
    @DisplayName("fromScore — 根据分数获取等级")
    class FromScoreTests {

        @Test
        @DisplayName("0 → SEEKER")
        void zeroIsSeeker() {
            assertEquals(FaithLevel.SEEKER, FaithLevel.fromScore(0));
        }

        @Test
        @DisplayName("1 → SEEKER")
        void oneIsSeeker() {
            assertEquals(FaithLevel.SEEKER, FaithLevel.fromScore(1));
        }

        @Test
        @DisplayName("10 → SEEKER")
        void tenIsSeeker() {
            assertEquals(FaithLevel.SEEKER, FaithLevel.fromScore(10));
        }

        @Test
        @DisplayName("11 → BELIEVER")
        void elevenIsBeliever() {
            assertEquals(FaithLevel.BELIEVER, FaithLevel.fromScore(11));
        }

        @Test
        @DisplayName("30 → BELIEVER")
        void thirtyIsBeliever() {
            assertEquals(FaithLevel.BELIEVER, FaithLevel.fromScore(30));
        }

        @Test
        @DisplayName("31 → DISCIPLE")
        void thirtyOneIsDisciple() {
            assertEquals(FaithLevel.DISCIPLE, FaithLevel.fromScore(31));
        }

        @Test
        @DisplayName("60 → DISCIPLE")
        void sixtyIsDisciple() {
            assertEquals(FaithLevel.DISCIPLE, FaithLevel.fromScore(60));
        }

        @Test
        @DisplayName("61 → EVANGELIST")
        void sixtyOneIsEvangelist() {
            assertEquals(FaithLevel.EVANGELIST, FaithLevel.fromScore(61));
        }

        @Test
        @DisplayName("100 → EVANGELIST")
        void hundredIsEvangelist() {
            assertEquals(FaithLevel.EVANGELIST, FaithLevel.fromScore(100));
        }

        @Test
        @DisplayName("超 100 → EVANGELIST")
        void overHundredIsEvangelist() {
            assertEquals(FaithLevel.EVANGELIST, FaithLevel.fromScore(200));
        }

        @Test
        @DisplayName("负值 → SEEKER")
        void negativeIsSeeker() {
            assertEquals(FaithLevel.SEEKER, FaithLevel.fromScore(-5));
        }

        @Test
        @DisplayName("边界值 10/11 正确切换")
        void boundaryBetweenSeekerAndBeliever() {
            assertEquals(FaithLevel.SEEKER, FaithLevel.fromScore(10));
            assertEquals(FaithLevel.BELIEVER, FaithLevel.fromScore(11));
        }

        @Test
        @DisplayName("边界值 30/31 正确切换")
        void boundaryBetweenBelieverAndDisciple() {
            assertEquals(FaithLevel.BELIEVER, FaithLevel.fromScore(30));
            assertEquals(FaithLevel.DISCIPLE, FaithLevel.fromScore(31));
        }

        @Test
        @DisplayName("边界值 60/61 正确切换")
        void boundaryBetweenDiscipleAndEvangelist() {
            assertEquals(FaithLevel.DISCIPLE, FaithLevel.fromScore(60));
            assertEquals(FaithLevel.EVANGELIST, FaithLevel.fromScore(61));
        }
    }
}
