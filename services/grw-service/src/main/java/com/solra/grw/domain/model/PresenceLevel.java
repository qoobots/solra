package com.solra.grw.domain.model;

/**
 * PresenceLevel 枚举 — 用户存在值等级体系。
 * GRW-001: 等级享有可见权益，≥20个成就里程碑。
 * 代表用户在 Solra 平台中的存在深度（Presence Depth）。
 */
public enum PresenceLevel {
    /** Lv1: 访客 — 刚刚到来 */
    VISITOR(1, 0, 0, "访客"),
    /** Lv2: 旁观者 */
    OBSERVER(2, 10, 0, "旁观者"),
    /** Lv3: 新人 */
    NEWCOMER(3, 30, 0, "新人"),
    /** Lv4: 探索者 */
    EXPLORER(4, 60, 1, "探索者"),
    /** Lv5: 居民 */
    RESIDENT(5, 100, 2, "居民"),
    /** Lv6: 常客 */
    REGULAR(6, 180, 3, "常客"),
    /** Lv7: 社群成员 */
    MEMBER(7, 300, 5, "社群成员"),
    /** Lv8: 核心成员 */
    CORE(8, 500, 7, "核心成员"),
    /** Lv9: 守护者 */
    GUARDIAN(9, 800, 10, "守护者"),
    /** Lv10: 传奇 */
    LEGEND(10, 1200, 15, "传奇"),
    /** Lv11-20 为进阶体系保留 */
    ELDER(15, 2000, 20, "长老"),
    SAGE(20, 3500, 30, "贤者"),
    MYSTIC(25, 5500, 45, "隐士"),
    ORACLE(30, 8000, 60, "先知"),
    ARCHITECT(35, 12000, 80, "建筑师"),
    DEMIURGE(40, 18000, 100, "造物主"),
    TRANSCENDENT(45, 25000, 130, "超越者"),
    ETERNAL(50, 35000, 170, "永恒者");

    private final int level;
    private final int experienceRequired;  // 达到该等级所需总经验值
    private final int spacesRequired;      // 达到该等级所需探索空间数
    private final String displayName;

    PresenceLevel(int level, int experienceRequired, int spacesRequired, String displayName) {
        this.level = level;
        this.experienceRequired = experienceRequired;
        this.spacesRequired = spacesRequired;
        this.displayName = displayName;
    }

    /** 根据经验值计算当前等级 */
    public static PresenceLevel fromExperience(int totalExperience) {
        PresenceLevel result = VISITOR;
        for (PresenceLevel pl : values()) {
            if (totalExperience >= pl.experienceRequired) {
                result = pl;
            }
        }
        return result;
    }

    /** 获取下一等级 */
    public PresenceLevel nextLevel() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal < values().length) {
            return values()[nextOrdinal];
        }
        return this; // 已是最高等级
    }

    /** 获取升级所需剩余经验值 */
    public int experienceToNextLevel(int currentExperience) {
        PresenceLevel next = nextLevel();
        if (next == this) return 0;
        return Math.max(0, next.experienceRequired - currentExperience);
    }

    /** 获取该等级的进度百分比 (0.0-1.0) */
    public double progressToNextLevel(int currentExperience) {
        if (this == values()[values().length - 1]) return 1.0;
        int currentLevelExp = this.experienceRequired;
        int nextLevelExp = nextLevel().experienceRequired;
        if (nextLevelExp <= currentLevelExp) return 1.0;
        return (double) (currentExperience - currentLevelExp) / (nextLevelExp - currentLevelExp);
    }

    /** 获取等级可解锁的权益数量 */
    public int getUnlockablePerks() {
        return Math.max(0, level / 3);
    }

    // ---- getters ----
    public int getLevel() { return level; }
    public int getExperienceRequired() { return experienceRequired; }
    public int getSpacesRequired() { return spacesRequired; }
    public String getDisplayName() { return displayName; }
}
