package com.solra.grw.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PresenceLevel 单元测试。
 */
class PresenceLevelTest {

    @Test
    void shouldReturnVisitorForZeroExperience() {
        assertEquals(PresenceLevel.VISITOR, PresenceLevel.fromExperience(0));
    }

    @Test
    void shouldReturnObserverFor10Experience() {
        assertEquals(PresenceLevel.OBSERVER, PresenceLevel.fromExperience(10));
    }

    @Test
    void shouldReturnLegendFor1200Experience() {
        assertEquals(PresenceLevel.LEGEND, PresenceLevel.fromExperience(1200));
    }

    @Test
    void shouldReturnHighestLevelForLargeExperience() {
        PresenceLevel level = PresenceLevel.fromExperience(100000);
        assertEquals(PresenceLevel.ETERNAL, level);
    }

    @Test
    void shouldProvideNextLevel() {
        assertEquals(PresenceLevel.OBSERVER, PresenceLevel.VISITOR.nextLevel());
    }

    @Test
    void shouldReturnSelfForHighestLevelNext() {
        assertEquals(PresenceLevel.ETERNAL, PresenceLevel.ETERNAL.nextLevel());
    }

    @Test
    void shouldCalculateExperienceToNextLevel() {
        int remaining = PresenceLevel.VISITOR.experienceToNextLevel(5);
        assertEquals(5, remaining); // 需要10，已有5
    }

    @Test
    void shouldCalculateProgressCorrectly() {
        double progress = PresenceLevel.VISITOR.progressToNextLevel(5);
        assertEquals(0.5, progress, 0.01);
    }

    @Test
    void shouldReturnFullProgressForMaxLevel() {
        double progress = PresenceLevel.ETERNAL.progressToNextLevel(100000);
        assertEquals(1.0, progress, 0.01);
    }

    @Test
    void shouldHaveDisplayNames() {
        assertEquals("访客", PresenceLevel.VISITOR.getDisplayName());
        assertEquals("传奇", PresenceLevel.LEGEND.getDisplayName());
        assertEquals("永恒者", PresenceLevel.ETERNAL.getDisplayName());
    }

    @Test
    void shouldHaveLevelNumbers() {
        assertEquals(1, PresenceLevel.VISITOR.getLevel());
        assertEquals(10, PresenceLevel.LEGEND.getLevel());
        assertEquals(50, PresenceLevel.ETERNAL.getLevel());
    }

    @Test
    void shouldHaveExperienceThresholds() {
        assertEquals(0, PresenceLevel.VISITOR.getExperienceRequired());
        assertEquals(1200, PresenceLevel.LEGEND.getExperienceRequired());
        assertTrue(PresenceLevel.ETERNAL.getExperienceRequired() > 10000);
    }
}
