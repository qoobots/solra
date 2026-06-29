package com.solra.avt.domain.service;

import com.solra.avt.domain.model.AffectionLevel;
import com.solra.avt.domain.model.AffectionLevel.AffectionSource;
import com.solra.avt.domain.model.AffectionLevel.UnlockableInteraction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AffectionSystemService — AVT-009 好感度系统领域服务。
 */
@DisplayName("AVT-009: AffectionSystemService")
class AffectionSystemServiceTest {

    private AffectionSystemService service;

    @BeforeEach
    void setUp() {
        service = new AffectionSystemService();
    }

    @Test
    @DisplayName("Should create affection record for new user-avatar pair")
    void shouldCreateNewAffection() {
        AffectionLevel affection = service.getOrCreate("user-001", "avatar-001");

        assertNotNull(affection);
        assertEquals("user-001", affection.getUserId());
        assertEquals("avatar-001", affection.getAvatarId());
        assertEquals(1, affection.getLevel());
        assertEquals(0, affection.getScore());
    }

    @Test
    @DisplayName("Should return same instance for repeated calls")
    void shouldReturnSameInstance() {
        AffectionLevel a1 = service.getOrCreate("user-001", "avatar-001");
        AffectionLevel a2 = service.getOrCreate("user-001", "avatar-001");

        assertSame(a1, a2);
    }

    @Test
    @DisplayName("Should record conversation affection")
    void shouldRecordConversationAffection() {
        service.recordConversationAffection("user-001", "avatar-001",
                200, true, true, 15);

        AffectionLevel affection = service.getOrCreate("user-001", "avatar-001");
        assertTrue(affection.getScore() > 0);
    }

    @Test
    @DisplayName("Should record daily visit affection")
    void shouldRecordDailyVisit() {
        service.recordDailyVisit("user-001", "avatar-001", 7);

        AffectionLevel affection = service.getOrCreate("user-001", "avatar-001");
        assertTrue(affection.getScore() > 0);
    }

    @Test
    @DisplayName("Should record extended session affection")
    void shouldRecordExtendedSession() {
        service.recordExtendedSession("user-001", "avatar-001", 30);

        AffectionLevel affection = service.getOrCreate("user-001", "avatar-001");
        assertTrue(affection.getScore() > 0);
    }

    @Test
    @DisplayName("Should not record affection for short sessions")
    void shouldNotRecordShortSession() {
        service.recordExtendedSession("user-001", "avatar-001", 5);

        AffectionLevel affection = service.getOrCreate("user-001", "avatar-001");
        assertEquals(0, affection.getScore());
    }

    @Test
    @DisplayName("Should return intimacy tone based on level")
    void shouldReturnIntimacyTone() {
        assertEquals("polite", service.getIntimacyTone("user-001", "avatar-001"));

        // Boost affection
        for (int i = 0; i < 20; i++) {
            service.recordAffection("user-001", "avatar-001",
                    AffectionSource.GAVE_GIFT, 20, "Gift " + i);
        }

        String tone = service.getIntimacyTone("user-001", "avatar-001");
        assertNotNull(tone);
        assertNotEquals("polite", tone); // Should have upgraded
    }

    @Test
    @DisplayName("Should track greeting frequency multiplier")
    void shouldTrackGreetingMultiplier() {
        float base = service.getGreetingFrequencyMultiplier("user-001", "avatar-001");
        assertEquals(1.0f, base, 0.01f);
    }

    @Test
    @DisplayName("Should check interaction unlock status")
    void shouldCheckInteractionUnlock() {
        // L1 - nothing unlocked
        assertFalse(service.isInteractionUnlocked("user-001", "avatar-001",
                UnlockableInteraction.PERSONALIZED_GREETING));

        // Boost to level 5+
        for (int i = 0; i < 30; i++) {
            service.recordAffection("user-001", "avatar-001",
                    AffectionSource.GAVE_GIFT, 10, "Gift " + i);
        }

        assertTrue(service.isInteractionUnlocked("user-001", "avatar-001",
                UnlockableInteraction.PERSONALIZED_GREETING));
    }

    @Test
    @DisplayName("Should get top avatars by affection")
    void shouldGetTopAvatars() {
        service.recordAffection("user-001", "avatar-001",
                AffectionSource.GAVE_GIFT, 20, "Gift");
        service.recordAffection("user-001", "avatar-002",
                AffectionSource.DEEP_CONVERSATION, 5, "Chat");
        service.recordAffection("user-001", "avatar-003",
                AffectionSource.ANNIVERSARY, 30, "Anniversary");

        List<AffectionLevel> top = service.getTopAvatars("user-001", 3);

        assertEquals(3, top.size());
        // avatar-003 should be first (highest score from ANNIVERSARY)
        assertEquals("avatar-003", top.get(0).getAvatarId());
    }

    @Test
    @DisplayName("Should calculate level progress percentage")
    void shouldCalculateLevelProgress() {
        // L1→L2 needs 100 points
        service.recordAffection("user-001", "avatar-001",
                AffectionSource.CASUAL_CHAT, 50, "Chat");

        int progress = service.getLevelProgress("user-001", "avatar-001");
        assertTrue(progress > 0);
        assertTrue(progress < 100);
    }

    @Test
    @DisplayName("Should return 100% progress at max level")
    void shouldReturnFullProgressAtMax() {
        // Boost to max level
        for (int i = 0; i < 100; i++) {
            service.recordAffection("user-001", "avatar-001",
                    AffectionSource.ANNIVERSARY, 50, "Boost " + i);
        }

        assertEquals(100, service.getLevelProgress("user-001", "avatar-001"));
    }
}
