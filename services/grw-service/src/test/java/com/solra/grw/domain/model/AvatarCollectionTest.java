package com.solra.grw.domain.model;

import org.junit.jupiter.api.Test;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AvatarCollection 单元测试。
 */
class AvatarCollectionTest {

    @Test
    void shouldCreateEmptyCollection() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        assertEquals(0, col.getUsedSlots());
        assertEquals(20, col.getTotalSlots());
        assertTrue(col.getAvatars().isEmpty());
    }

    @Test
    void shouldCollectAvatarSuccessfully() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        AvatarCollection.AvatarEntry entry = col.collectAvatar(
                "AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_3,
                AvatarCollection.AvatarElement.FIRE);
        assertEquals(1, col.getUsedSlots());
        assertEquals("烈焰", entry.getName());
        assertEquals(AvatarCollection.AvatarRarity.STAR_3, entry.getRarity());
        assertEquals(1, entry.getLevel());
    }

    @Test
    void shouldNotAllowDuplicateCollection() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_3,
                AvatarCollection.AvatarElement.FIRE);
        assertThrows(IllegalArgumentException.class, () ->
                col.collectAvatar("AV-002", "TYPE_FIRE", "烈焰2", AvatarCollection.AvatarRarity.STAR_1,
                        AvatarCollection.AvatarElement.FIRE));
    }

    @Test
    void shouldThrowWhenSlotsFull() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        for (int i = 0; i < 20; i++) {
            col.collectAvatar("AV-" + i, "TYPE_" + i, "Avatar" + i,
                    AvatarCollection.AvatarRarity.STAR_1, AvatarCollection.AvatarElement.FIRE);
        }
        assertEquals(20, col.getUsedSlots());
        assertThrows(IllegalStateException.class, () ->
                col.collectAvatar("AV-20", "TYPE_20", "Avatar20",
                        AvatarCollection.AvatarRarity.STAR_1, AvatarCollection.AvatarElement.FIRE));
    }

    @Test
    void shouldUpgradeAvatarLevel() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_1,
                AvatarCollection.AvatarElement.FIRE);
        col.upgradeAvatar("TYPE_FIRE", 500);
        AvatarCollection.AvatarEntry entry = col.getAvatars().get("TYPE_FIRE");
        assertEquals(6, entry.getLevel()); // 1 + 500/100 = 6
    }

    @Test
    void shouldIncreaseAffection() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_1,
                AvatarCollection.AvatarElement.FIRE);
        col.increaseAffection("TYPE_FIRE", 300);
        assertEquals(300, col.getAvatars().get("TYPE_FIRE").getAffection());
    }

    @Test
    void shouldSetFavoriteAvatar() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_1,
                AvatarCollection.AvatarElement.FIRE);
        col.collectAvatar("AV-002", "TYPE_WATER", "碧波", AvatarCollection.AvatarRarity.STAR_2,
                AvatarCollection.AvatarElement.WATER);
        col.setFavorite("TYPE_WATER");
        assertFalse(col.getAvatars().get("TYPE_FIRE").isFavorite());
        assertTrue(col.getAvatars().get("TYPE_WATER").isFavorite());
    }

    @Test
    void shouldCalculateCollectionCompletion() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_1,
                AvatarCollection.AvatarElement.FIRE);
        col.collectAvatar("AV-002", "TYPE_WATER", "碧波", AvatarCollection.AvatarRarity.STAR_2,
                AvatarCollection.AvatarElement.WATER);
        assertEquals(2.0 / 12, col.getCollectionCompletion(12), 0.01);
    }

    @Test
    void shouldGetRarityDistribution() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_3,
                AvatarCollection.AvatarElement.FIRE);
        col.collectAvatar("AV-002", "TYPE_WATER", "碧波", AvatarCollection.AvatarRarity.STAR_1,
                AvatarCollection.AvatarElement.WATER);
        Map<AvatarCollection.AvatarRarity, Long> dist = col.getRarityDistribution();
        assertEquals(1L, dist.get(AvatarCollection.AvatarRarity.STAR_3));
        assertEquals(1L, dist.get(AvatarCollection.AvatarRarity.STAR_1));
    }

    @Test
    void shouldFindHighestLevelAvatar() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.collectAvatar("AV-001", "TYPE_FIRE", "烈焰", AvatarCollection.AvatarRarity.STAR_1,
                AvatarCollection.AvatarElement.FIRE);
        col.collectAvatar("AV-002", "TYPE_WATER", "碧波", AvatarCollection.AvatarRarity.STAR_2,
                AvatarCollection.AvatarElement.WATER);
        col.upgradeAvatar("TYPE_WATER", 900);
        assertTrue(col.getHighestLevel().isPresent());
        assertTrue(col.getHighestLevel().get().getLevel() > 1);
    }

    @Test
    void shouldExpandSlots() {
        AvatarCollection col = new AvatarCollection("COL-001", "user1");
        col.expandSlots(10);
        assertEquals(30, col.getTotalSlots());
    }
}
