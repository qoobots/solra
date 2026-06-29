package com.solra.grw.domain.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Achievement 单元测试。
 */
class AchievementTest {

    @Test
    void shouldHaveAtLeast30Achievements() {
        List<Achievement> all = Achievement.allAchievements();
        assertTrue(all.size() >= 30, "Should have at least 30 achievements, got " + all.size());
    }

    @Test
    void shouldHave7Categories() {
        List<Achievement> all = Achievement.allAchievements();
        long categories = all.stream().map(Achievement::getCategory).distinct().count();
        assertEquals(7, categories);
    }

    @Test
    void shouldHaveAllRarities() {
        List<Achievement> all = Achievement.allAchievements();
        long rarities = all.stream().map(Achievement::getRarity).distinct().count();
        assertEquals(5, rarities);
    }

    @Test
    void shouldBeUnlockableWhenProgressMeetsRequired() {
        Achievement ach = new Achievement("ACH-TEST", "TEST", "Test", "desc",
                Achievement.Category.EXPLORATION, Achievement.Rarity.COMMON, 10, 50);
        assertTrue(ach.isUnlockable(10));
        assertTrue(ach.isUnlockable(15));
    }

    @Test
    void shouldNotBeUnlockableWhenProgressInsufficient() {
        Achievement ach = new Achievement("ACH-TEST", "TEST", "Test", "desc",
                Achievement.Category.EXPLORATION, Achievement.Rarity.COMMON, 10, 50);
        assertFalse(ach.isUnlockable(5));
    }

    @Test
    void shouldHaveAllPrerequisitesWhenEmpty() {
        Achievement ach = new Achievement("ACH-TEST", "TEST", "Test", "desc",
                Achievement.Category.EXPLORATION, Achievement.Rarity.COMMON, 10, 50);
        assertTrue(ach.hasAllPrerequisites(Set.of()));
    }

    @Test
    void shouldCheckPrerequisitesCorrectly() {
        Achievement ach = new Achievement("ACH-TEST", "TEST", "Test", "desc",
                Achievement.Category.EXPLORATION, Achievement.Rarity.COMMON, 10, 50);
        ach.getPrerequisiteCodes().add("PRE_A");
        ach.getPrerequisiteCodes().add("PRE_B");
        assertTrue(ach.hasAllPrerequisites(Set.of("PRE_A", "PRE_B", "PRE_C")));
        assertFalse(ach.hasAllPrerequisites(Set.of("PRE_A")));
    }

    @Test
    void shouldCalculateProgressPercentage() {
        Achievement ach = new Achievement("ACH-TEST", "TEST", "Test", "desc",
                Achievement.Category.SOCIAL, Achievement.Rarity.RARE, 100, 200);
        assertEquals(0.5, ach.progressPercentage(50), 0.01);
        assertEquals(1.0, ach.progressPercentage(150), 0.01);
        assertEquals(0.0, ach.progressPercentage(0), 0.01);
    }

    @Test
    void shouldHaveExplorationAchievements() {
        List<Achievement> exploration = Achievement.explorationAchievements();
        assertEquals(8, exploration.size());
        assertTrue(exploration.stream().allMatch(a -> a.getCategory() == Achievement.Category.EXPLORATION));
    }

    @Test
    void shouldHaveSocialAchievements() {
        List<Achievement> social = Achievement.socialAchievements();
        assertEquals(8, social.size());
    }

    @Test
    void shouldHaveLegendaryAchievements() {
        List<Achievement> all = Achievement.allAchievements();
        long legendary = all.stream().filter(a -> a.getRarity() == Achievement.Rarity.LEGENDARY).count();
        assertTrue(legendary >= 2, "Should have at least 2 legendary achievements");
    }

    @Test
    void shouldHaveSpecialAchievements() {
        List<Achievement> special = Achievement.specialAchievements();
        assertEquals(2, special.size());
    }
}
