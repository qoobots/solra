package com.solra.grw.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * Achievement 聚合根 — 成就系统。
 * GRW-005: ≥30个成就，每成就含专属徽章+动效+音效。
 */
public class Achievement {

    public enum Category {
        EXPLORATION,   // 探索类
        SOCIAL,        // 社交类
        CREATION,      // 创作类
        ENGAGEMENT,    // 活跃类
        COLLECTION,    // 收藏类
        MASTERY,       // 精通类
        SPECIAL        // 特殊类
    }

    public enum Rarity {
        COMMON,      // 普通 — 灰色徽章
        UNCOMMON,    // 稀有 — 绿色徽章
        RARE,        // 罕见 — 蓝色徽章
        EPIC,        // 史诗 — 紫色徽章
        LEGENDARY    // 传说 — 金色徽章
    }

    private String achievementId;
    private String code;            // 唯一标识码 e.g. "FIRST_10_SPACES"
    private String name;            // 成就名称
    private String description;     // 成就描述
    private Category category;
    private Rarity rarity;
    private String badgeIconUrl;    // 徽章图标URL
    private String badgeEffect;     // 动效名称 e.g. "sparkle", "glow", "confetti"
    private String soundEffect;     // 音效名称 e.g. "achievement_unlock", "rare_fanfare"
    private int requiredCount;      // 达成条件：需要多少次/多少个
    private int experienceReward;   // 经验值奖励
    private List<String> prerequisiteCodes; // 前置成就编码列表
    private boolean hidden;         // 是否隐藏（达成前不显示）
    private Instant createdAt;

    public Achievement() {
        this.prerequisiteCodes = new ArrayList<>();
    }

    public Achievement(String achievementId, String code, String name, String description,
                       Category category, Rarity rarity, int requiredCount, int experienceReward) {
        this();
        this.achievementId = achievementId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.requiredCount = requiredCount;
        this.experienceReward = experienceReward;
        this.hidden = false;
        this.createdAt = Instant.now();
    }

    /** 判断该成就的进度是否满足解锁条件 */
    public boolean isUnlockable(int currentProgress) {
        return currentProgress >= requiredCount;
    }

    /** 判断是否所有前置成就都已解锁 */
    public boolean hasAllPrerequisites(Set<String> unlockedCodes) {
        if (prerequisiteCodes == null || prerequisiteCodes.isEmpty()) return true;
        return unlockedCodes.containsAll(prerequisiteCodes);
    }

    /** 根据进度计算百分比 (0.0-1.0) */
    public double progressPercentage(int currentProgress) {
        if (requiredCount == 0) return 1.0;
        return Math.min(1.0, (double) currentProgress / requiredCount);
    }

    // ---- 预定义30+成就工厂方法 ----

    /** 探索类成就 (8个) */
    public static List<Achievement> explorationAchievements() {
        return List.of(
            new Achievement("ACH-001", "FIRST_SPACE", "初次到访", "首次进入一个空间",
                Category.EXPLORATION, Rarity.COMMON, 1, 10),
            new Achievement("ACH-002", "SPACE_EXPLORER_5", "空间旅人", "探索5个不同的空间",
                Category.EXPLORATION, Rarity.COMMON, 5, 30),
            new Achievement("ACH-003", "SPACE_EXPLORER_20", "空间冒险家", "探索20个不同的空间",
                Category.EXPLORATION, Rarity.UNCOMMON, 20, 80),
            new Achievement("ACH-004", "SPACE_EXPLORER_50", "空间征服者", "探索50个不同的空间",
                Category.EXPLORATION, Rarity.RARE, 50, 200),
            new Achievement("ACH-005", "SPACE_EXPLORER_100", "空间大师", "探索100个不同的空间",
                Category.EXPLORATION, Rarity.EPIC, 100, 500),
            new Achievement("ACH-006", "FIRST_RETURN", "再次光临", "同一天内回访同一空间",
                Category.EXPLORATION, Rarity.COMMON, 1, 15),
            new Achievement("ACH-007", "DAILY_VISITOR_7", "常客", "连续7天登录",
                Category.EXPLORATION, Rarity.UNCOMMON, 7, 60),
            new Achievement("ACH-008", "DAILY_VISITOR_30", "忠实访客", "连续30天登录",
                Category.EXPLORATION, Rarity.RARE, 30, 250)
        );
    }

    /** 社交类成就 (8个) */
    public static List<Achievement> socialAchievements() {
        return List.of(
            new Achievement("ACH-009", "FIRST_CONVERSATION", "初次交谈", "与虚拟人完成首次对话",
                Category.SOCIAL, Rarity.COMMON, 1, 10),
            new Achievement("ACH-010", "CONVERSATION_MASTER_50", "话痨", "累计对话50次",
                Category.SOCIAL, Rarity.UNCOMMON, 50, 100),
            new Achievement("ACH-011", "CONVERSATION_MASTER_500", "社交达人", "累计对话500次",
                Category.SOCIAL, Rarity.RARE, 500, 500),
            new Achievement("ACH-012", "FIRST_FRIEND", "第一个朋友", "添加第一个好友",
                Category.SOCIAL, Rarity.COMMON, 1, 15),
            new Achievement("ACH-013", "FRIEND_COLLECTOR_10", "交友广泛", "拥有10个好友",
                Category.SOCIAL, Rarity.UNCOMMON, 10, 60),
            new Achievement("ACH-014", "FRIEND_COLLECTOR_50", "人脉之王", "拥有50个好友",
                Category.SOCIAL, Rarity.RARE, 50, 250),
            new Achievement("ACH-015", "FIRST_GESTURE", "举手之劳", "首次使用社交手势",
                Category.SOCIAL, Rarity.COMMON, 1, 5),
            new Achievement("ACH-016", "GESTURE_ENTHUSIAST_100", "手势狂热者", "使用100次社交手势",
                Category.SOCIAL, Rarity.UNCOMMON, 100, 150)
        );
    }

    /** 创作类成就 (5个) */
    public static List<Achievement> creationAchievements() {
        return List.of(
            new Achievement("ACH-017", "FIRST_CREATION", "初试锋芒", "首次创建空间",
                Category.CREATION, Rarity.COMMON, 1, 20),
            new Achievement("ACH-018", "CREATOR_5", "空间工匠", "创建5个空间",
                Category.CREATION, Rarity.UNCOMMON, 5, 100),
            new Achievement("ACH-019", "CREATOR_20", "空间建筑师", "创建20个空间",
                Category.CREATION, Rarity.RARE, 20, 400),
            new Achievement("ACH-020", "FIRST_SHARE", "分享快乐", "首次分享空间",
                Category.CREATION, Rarity.COMMON, 1, 15),
            new Achievement("ACH-021", "SHARE_VIRAL_10", "病毒传播者", "分享被点击10次",
                Category.CREATION, Rarity.UNCOMMON, 10, 80)
        );
    }

    /** 活跃类成就 (5个) */
    public static List<Achievement> engagementAchievements() {
        return List.of(
            new Achievement("ACH-022", "TIME_SPENT_1H", "初来乍到", "累计在线1小时",
                Category.ENGAGEMENT, Rarity.COMMON, 1, 15),
            new Achievement("ACH-023", "TIME_SPENT_10H", "沉浸体验", "累计在线10小时",
                Category.ENGAGEMENT, Rarity.UNCOMMON, 10, 80),
            new Achievement("ACH-024", "TIME_SPENT_100H", "深度用户", "累计在线100小时",
                Category.ENGAGEMENT, Rarity.RARE, 100, 400),
            new Achievement("ACH-025", "LEVEL_10", "十级强者", "达到10级",
                Category.ENGAGEMENT, Rarity.UNCOMMON, 10, 100),
            new Achievement("ACH-026", "LEVEL_30", "三十级大师", "达到30级",
                Category.ENGAGEMENT, Rarity.RARE, 30, 500)
        );
    }

    /** 收藏类成就 (4个) */
    public static List<Achievement> collectionAchievements() {
        return List.of(
            new Achievement("ACH-027", "FIRST_AVATAR", "初次邂逅", "收集第一个虚拟人",
                Category.COLLECTION, Rarity.COMMON, 1, 10),
            new Achievement("ACH-028", "AVATAR_COLLECTOR_5", "虚拟人收藏家", "收集5个虚拟人",
                Category.COLLECTION, Rarity.UNCOMMON, 5, 60),
            new Achievement("ACH-029", "AVATAR_COLLECTOR_10", "虚拟人大师", "收集10个虚拟人",
                Category.COLLECTION, Rarity.RARE, 10, 200),
            new Achievement("ACH-030", "FULL_COLLECTION", "全图鉴达成", "收集所有基础虚拟人",
                Category.COLLECTION, Rarity.EPIC, 10, 800)
        );
    }

    /** 精通类成就 (3个) */
    public static List<Achievement> masteryAchievements() {
        return List.of(
            new Achievement("ACH-031", "EVANGELIST", "布道者", "成为认证布道者",
                Category.MASTERY, Rarity.EPIC, 1, 1000),
            new Achievement("ACH-032", "ALL_ACHIEVEMENTS_20", "成就猎人", "解锁20个成就",
                Category.MASTERY, Rarity.RARE, 20, 300),
            new Achievement("ACH-033", "ALL_ACHIEVEMENTS_30", "成就大师", "解锁全部30个成就",
                Category.MASTERY, Rarity.LEGENDARY, 30, 2000)
        );
    }

    /** 特殊类成就 (2个) */
    public static List<Achievement> specialAchievements() {
        return List.of(
            new Achievement("ACH-034", "BETA_TESTER", "先驱者", "参与Beta测试",
                Category.SPECIAL, Rarity.LEGENDARY, 1, 500),
            new Achievement("ACH-035", "ANNIVERSARY_1", "一周年", "注册满一周年",
                Category.SPECIAL, Rarity.EPIC, 1, 300)
        );
    }

    /** 获取全部35个预定义成就 */
    public static List<Achievement> allAchievements() {
        List<Achievement> all = new ArrayList<>();
        all.addAll(explorationAchievements());
        all.addAll(socialAchievements());
        all.addAll(creationAchievements());
        all.addAll(engagementAchievements());
        all.addAll(collectionAchievements());
        all.addAll(masteryAchievements());
        all.addAll(specialAchievements());
        return all;
    }

    // ---- getters/setters ----
    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public String getBadgeIconUrl() { return badgeIconUrl; }
    public void setBadgeIconUrl(String badgeIconUrl) { this.badgeIconUrl = badgeIconUrl; }
    public String getBadgeEffect() { return badgeEffect; }
    public void setBadgeEffect(String badgeEffect) { this.badgeEffect = badgeEffect; }
    public String getSoundEffect() { return soundEffect; }
    public void setSoundEffect(String soundEffect) { this.soundEffect = soundEffect; }
    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
    public int getExperienceReward() { return experienceReward; }
    public void setExperienceReward(int experienceReward) { this.experienceReward = experienceReward; }
    public List<String> getPrerequisiteCodes() { return prerequisiteCodes; }
    public void setPrerequisiteCodes(List<String> prerequisiteCodes) { this.prerequisiteCodes = prerequisiteCodes; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
