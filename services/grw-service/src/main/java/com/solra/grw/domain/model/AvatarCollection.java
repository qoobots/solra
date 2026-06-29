package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * AvatarCollection 聚合根 — 虚拟人收集与养成。
 * GRW-004: ≥10个基础虚拟人，图鉴系统。
 */
public class AvatarCollection {

    public enum AvatarRarity {
        STAR_1,     // 1星 — 基础款
        STAR_2,     // 2星 — 进阶款
        STAR_3,     // 3星 — 稀有款
        STAR_4,     // 4星 — 史诗款
        STAR_5      // 5星 — 传说款
    }

    public enum AvatarElement {
        FIRE,       // 火
        WATER,      // 水
        EARTH,      // 土
        WIND,       // 风
        LIGHT,      // 光
        SHADOW      // 暗
    }

    /**
     * 虚拟人条目 — 用户收集的单个虚拟人信息。
     */
    public static class AvatarEntry {
        private String avatarId;         // 虚拟人实例ID
        private String avatarTypeId;     // 虚拟人类型ID
        private String name;
        private AvatarRarity rarity;
        private AvatarElement element;
        private int level;               // 养成等级 (1-100)
        private int experience;          // 当前经验值
        private int affection;           // 好感度 (0-1000)
        private boolean isFavorite;      // 是否设为最爱
        private Instant obtainedAt;      // 获得时间
        private Instant lastInteractedAt;

        public AvatarEntry() {}

        public AvatarEntry(String avatarId, String avatarTypeId, String name,
                           AvatarRarity rarity, AvatarElement element) {
            this.avatarId = avatarId;
            this.avatarTypeId = avatarTypeId;
            this.name = name;
            this.rarity = rarity;
            this.element = element;
            this.level = 1;
            this.experience = 0;
            this.affection = 0;
            this.isFavorite = false;
            this.obtainedAt = Instant.now();
            this.lastInteractedAt = this.obtainedAt;
        }

        /** 增加经验值，可能升级 */
        public void addExperience(int amount) {
            this.experience += amount;
            int newLevel = 1 + this.experience / 100;
            if (newLevel > this.level && newLevel <= 100) {
                this.level = newLevel;
            }
        }

        /** 增加好感度 */
        public void addAffection(int amount) {
            this.affection = Math.min(1000, this.affection + amount);
            this.lastInteractedAt = Instant.now();
        }

        /** 设为最爱 */
        public void setFavorite(boolean favorite) { this.isFavorite = favorite; }

        /** 获取养成进度百分比 */
        public double getGrowthProgress() {
            return Math.min(1.0, (double) experience / 10000);
        }

        // ---- getters/setters ----
        public String getAvatarId() { return avatarId; }
        public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
        public String getAvatarTypeId() { return avatarTypeId; }
        public void setAvatarTypeId(String avatarTypeId) { this.avatarTypeId = avatarTypeId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public AvatarRarity getRarity() { return rarity; }
        public void setRarity(AvatarRarity rarity) { this.rarity = rarity; }
        public AvatarElement getElement() { return element; }
        public void setElement(AvatarElement element) { this.element = element; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public int getExperience() { return experience; }
        public void setExperience(int experience) { this.experience = experience; }
        public int getAffection() { return affection; }
        public void setAffection(int affection) { this.affection = affection; }
        public boolean isFavorite() { return isFavorite; }
        public void setFavorite(boolean favorite) { isFavorite = favorite; }
        public Instant getObtainedAt() { return obtainedAt; }
        public void setObtainedAt(Instant obtainedAt) { this.obtainedAt = obtainedAt; }
        public Instant getLastInteractedAt() { return lastInteractedAt; }
        public void setLastInteractedAt(Instant lastInteractedAt) { this.lastInteractedAt = lastInteractedAt; }
    }

    // ---- AvatarCollection ----
    private String collectionId;
    private String userId;
    private Map<String, AvatarEntry> avatars;     // avatarTypeId -> AvatarEntry
    private int totalSlots;                        // 总槽位
    private int usedSlots;                         // 已使用槽位
    private Instant createdAt;
    private Instant updatedAt;

    public AvatarCollection() {
        this.avatars = new LinkedHashMap<>();
    }

    public AvatarCollection(String collectionId, String userId) {
        this();
        this.collectionId = collectionId;
        this.userId = userId;
        this.totalSlots = 20;  // 初始20槽位
        this.usedSlots = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    /** 收集一个新虚拟人 */
    public AvatarEntry collectAvatar(String avatarId, String avatarTypeId, String name,
                                      AvatarRarity rarity, AvatarElement element) {
        if (usedSlots >= totalSlots) {
            throw new IllegalStateException("Collection slots full: " + usedSlots + "/" + totalSlots);
        }
        if (avatars.containsKey(avatarTypeId)) {
            throw new IllegalArgumentException("Avatar type already collected: " + avatarTypeId);
        }
        AvatarEntry entry = new AvatarEntry(avatarId, avatarTypeId, name, rarity, element);
        avatars.put(avatarTypeId, entry);
        usedSlots = avatars.size();
        updatedAt = Instant.now();
        return entry;
    }

    /** 升级虚拟人 */
    public void upgradeAvatar(String avatarTypeId, int experienceAmount) {
        AvatarEntry entry = avatars.get(avatarTypeId);
        if (entry == null) throw new IllegalArgumentException("Avatar not in collection: " + avatarTypeId);
        entry.addExperience(experienceAmount);
        updatedAt = Instant.now();
    }

    /** 增加好感度 */
    public void increaseAffection(String avatarTypeId, int amount) {
        AvatarEntry entry = avatars.get(avatarTypeId);
        if (entry == null) throw new IllegalArgumentException("Avatar not in collection: " + avatarTypeId);
        entry.addAffection(amount);
        updatedAt = Instant.now();
    }

    /** 设置最爱虚拟人 */
    public void setFavorite(String avatarTypeId) {
        avatars.values().forEach(e -> e.setFavorite(false));
        AvatarEntry entry = avatars.get(avatarTypeId);
        if (entry != null) entry.setFavorite(true);
        updatedAt = Instant.now();
    }

    /** 扩展槽位 */
    public void expandSlots(int additionalSlots) {
        this.totalSlots += additionalSlots;
        updatedAt = Instant.now();
    }

    /** 获取图鉴完成度 */
    public double getCollectionCompletion(int totalAvailableTypes) {
        if (totalAvailableTypes == 0) return 0.0;
        return (double) avatars.size() / totalAvailableTypes;
    }

    /** 按稀有度统计 */
    public Map<AvatarRarity, Long> getRarityDistribution() {
        Map<AvatarRarity, Long> dist = new LinkedHashMap<>();
        for (AvatarRarity r : AvatarRarity.values()) dist.put(r, 0L);
        for (AvatarEntry e : avatars.values()) {
            dist.merge(e.getRarity(), 1L, Long::sum);
        }
        return dist;
    }

    /** 按元素统计 */
    public Map<AvatarElement, Long> getElementDistribution() {
        Map<AvatarElement, Long> dist = new LinkedHashMap<>();
        for (AvatarElement el : AvatarElement.values()) dist.put(el, 0L);
        for (AvatarEntry e : avatars.values()) {
            dist.merge(e.getElement(), 1L, Long::sum);
        }
        return dist;
    }

    /** 获取最高等级虚拟人 */
    public Optional<AvatarEntry> getHighestLevel() {
        return avatars.values().stream().max(Comparator.comparingInt(AvatarEntry::getLevel));
    }

    /** 获取最高好感度虚拟人 */
    public Optional<AvatarEntry> getHighestAffection() {
        return avatars.values().stream().max(Comparator.comparingInt(AvatarEntry::getAffection));
    }

    // ---- getters/setters ----
    public String getCollectionId() { return collectionId; }
    public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Map<String, AvatarEntry> getAvatars() { return avatars; }
    public void setAvatars(Map<String, AvatarEntry> avatars) { this.avatars = avatars; }
    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }
    public int getUsedSlots() { return usedSlots; }
    public void setUsedSlots(int usedSlots) { this.usedSlots = usedSlots; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
