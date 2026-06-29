package com.solra.avt.domain.model;

import com.solra.avt.domain.model.AffectionLevel.AffectionSource;
import com.solra.avt.domain.model.AffectionLevel.UnlockableInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffectionLevel — AVT-009 好感度系统领域模型。
 */
@DisplayName("AVT-009: AffectionLevel Domain Model")
class AffectionLevelTest {

    private AffectionLevel affection;

    @BeforeEach
    void setUp() {
        affection = new AffectionLevel("user-001", "avatar-001");
    }

    @Test
    @DisplayName("Should initialize at level 1 with 0 score")
    void shouldInitializeAtLevel1() {
        assertEquals(1, affection.getLevel());
        assertEquals(0, affection.getScore());
        assertEquals("陌生人", affection.getTitle());
        assertEquals("polite", affection.getIntimacyTone());
    }

    @Test
    @DisplayName("Should accumulate affection points from conversation")
    void shouldAccumulatePoints() {
        int result = affection.addAffection(AffectionSource.CASUAL_CHAT, 10, "Test chat");
        assertEquals(-1, result); // No level up from small points
        assertTrue(affection.getScore() > 0);
    }

    @Test
    @DisplayName("Should level up when crossing threshold")
    void shouldLevelUp() {
        // L1→L2 threshold is 100 points
        int result = affection.addAffection(AffectionSource.DEEP_CONVERSATION, 50, "Deep chat 1");
        assertEquals(-1, result);
        result = affection.addAffection(AffectionSource.DEEP_CONVERSATION, 50, "Deep chat 2");
        assertEquals(2, result); // Leveled up to 2
        assertEquals("初识", affection.getTitle());
    }

    @Test
    @DisplayName("Should progress through multiple levels")
    void shouldProgressThroughLevels() {
        // Give enough points to reach level 5 (700 points)
        for (int i = 0; i < 30; i++) {
            affection.addAffection(AffectionSource.DEEP_CONVERSATION, 10, "Chat " + i);
        }
        assertTrue(affection.getLevel() >= 4, "Should be at least level 4");
    }

    @Test
    @DisplayName("Should change intimacy tone with level")
    void shouldChangeIntimacyTone() {
        assertEquals("polite", affection.getIntimacyTone());

        // Boost to level 3
        for (int i = 0; i < 10; i++) {
            affection.addAffection(AffectionSource.DEEP_CONVERSATION, 10, "Chat " + i);
        }
        assertTrue(affection.getLevel() >= 3);
        assertEquals("friendly", affection.getIntimacyTone());
    }

    @Test
    @DisplayName("Should track greeting frequency multiplier")
    void shouldTrackGreetingMultiplier() {
        assertEquals(1.0f, affection.getGreetingFrequencyMultiplier(), 0.01f);

        // Level up to 3
        for (int i = 0; i < 10; i++) {
            affection.addAffection(AffectionSource.DEEP_CONVERSATION, 10, "Chat " + i);
        }
        assertEquals(1.0f + (affection.getLevel() - 1) * 0.1f,
                affection.getGreetingFrequencyMultiplier(), 0.01f);
    }

    @Test
    @DisplayName("Should unlock interactions at correct levels")
    void shouldUnlockInteractions() {
        // L1: nothing unlocked
        assertFalse(affection.isInteractionUnlocked(UnlockableInteraction.PERSONALIZED_GREETING));

        // Boost to level 5
        for (int i = 0; i < 25; i++) {
            affection.addAffection(AffectionSource.GAVE_GIFT, 10, "Gift " + i);
        }

        assertTrue(affection.getLevel() >= 5);
        assertTrue(affection.isInteractionUnlocked(UnlockableInteraction.PERSONALIZED_GREETING));
        assertTrue(affection.isInteractionUnlocked(UnlockableInteraction.PROACTIVE_CARE));
        assertFalse(affection.isInteractionUnlocked(UnlockableInteraction.FULL_TRUST));
    }

    @Test
    @DisplayName("Should apply time decay after inactivity")
    void shouldApplyTimeDecay() {
        // Add some points first
        affection.addAffection(AffectionSource.DEEP_CONVERSATION, 50, "Chat");

        int scoreBefore = affection.getScore();
        assertTrue(scoreBefore > 0);

        // Time decay won't reduce below 0
        affection.applyTimeDecay();
        assertTrue(affection.getScore() >= 0);
    }

    @Test
    @DisplayName("Should record affection history")
    void shouldRecordHistory() {
        affection.addAffection(AffectionSource.CASUAL_CHAT, 10, "Hello");
        affection.addAffection(AffectionSource.GAVE_GIFT, 20, "Gift");

        assertEquals(2, affection.getHistory().size());
        assertEquals("GAVE_GIFT", affection.getHistory().get(1).source());
    }

    @Test
    @DisplayName("Should track dimension scores separately")
    void shouldTrackDimensionScores() {
        affection.addAffection(AffectionSource.CASUAL_CHAT, 10, "Chat");
        affection.addAffection(AffectionSource.GAVE_GIFT, 20, "Gift");

        assertTrue(affection.getDialogueQualityScore() > 0);
        assertTrue(affection.getUserBehaviorScore() > 0);
        // Gift has higher multiplier (8x) than casual chat (1x)
        assertTrue(affection.getUserBehaviorScore() > affection.getDialogueQualityScore());
    }

    @Test
    @DisplayName("Should cap at level 10 (max level)")
    void shouldCapAtMaxLevel() {
        // Give massive points
        for (int i = 0; i < 100; i++) {
            affection.addAffection(AffectionSource.ANNIVERSARY, 50, "Anniversary " + i);
        }

        assertEquals(10, affection.getLevel());
        assertEquals("命中注定", affection.getTitle());
    }
}
