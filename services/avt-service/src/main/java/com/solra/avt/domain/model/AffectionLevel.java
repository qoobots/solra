package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * AffectionLevel — AVT-009 虚拟人好感度系统。
 *
 * 1-10级好感度体系，每级解锁新的互动能力。
 * 好感度基于多维度行为加权计算：对话质量、互动频率、用户行为、时间衰减。
 * 好感度影响虚拟人的对话行为（语气变化、主动互动频率、惊喜内容等）。
 *
 * 等级映射：
 *   L1 (0-99)    陌生人 — 基础问候
 *   L2 (100-249)  初识 — 记住名字
 *   L3 (250-449)  熟悉 — 个性化称呼
 *   L4 (450-699)  朋友 — 分享日常
 *   L5 (700-999)  好友 — 主动关心
 *   L6 (1000-1349) 密友 — 分享秘密
 *   L7 (1350-1749) 知己 — 情感共鸣
 *   L8 (1750-2199) 灵魂伴侣 — 深度共情
 *   L9 (2200-2699) 羁绊 — 专属互动
 *   L10 (2700+)   命中注定 — 完全信任
 */
public class AffectionLevel {

    private String userId;
    private String avatarId;
    private int score;                    // 好感度总分数
    private int level;                    // 当前等级 1-10
    private String title;                 // 当前等级称号
    private List<AffectionEvent> history; // 好感度变更历史
    private Instant createdAt;
    private Instant updatedAt;

    // 多维度分项分数（用于调试和展示）
    private int dialogueQualityScore;     // 对话质量分
    private int interactionFrequencyScore; // 互动频率分
    private int userBehaviorScore;        // 用户行为分（送礼/分享等）
    private int timeBonusScore;           // 时间累积分

    // 等级阈值
    public static final int[] LEVEL_THRESHOLDS = {0, 100, 250, 450, 700, 1000, 1350, 1750, 2200, 2700};
    public static final String[] LEVEL_TITLES = {
        "陌生人", "初识", "熟悉", "朋友", "好友",
        "密友", "知己", "灵魂伴侣", "羁绊", "命中注定"
    };

    public AffectionLevel() {
        this.score = 0;
        this.level = 1;
        this.title = LEVEL_TITLES[0];
        this.history = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public AffectionLevel(String userId, String avatarId) {
        this();
        this.userId = userId;
        this.avatarId = avatarId;
    }

    /**
     * Add affection points from a specific source.
     * Returns the new level if leveled up, or -1 if no change.
     */
    public int addAffection(AffectionSource source, int basePoints, String reason) {
        int multiplier = source.getMultiplier();
        int points = basePoints * multiplier;

        // Apply time decay before adding
        applyTimeDecay();

        int oldLevel = this.level;
        this.score += points;

        // Update dimension scores
        switch (source.getDimension()) {
            case "dialogue_quality" -> this.dialogueQualityScore += points;
            case "interaction_frequency" -> this.interactionFrequencyScore += points;
            case "user_behavior" -> this.userBehaviorScore += points;
            case "time_bonus" -> this.timeBonusScore += points;
        }

        // Recalculate level
        recalculateLevel();
        this.updatedAt = Instant.now();

        // Record history
        history.add(new AffectionEvent(source.name(), points, reason, oldLevel, this.level, this.updatedAt));

        // Trim history to last 50 events
        if (history.size() > 50) {
            history = history.subList(history.size() - 50, history.size());
        }

        return this.level > oldLevel ? this.level : -1;
    }

    /**
     * Apply time-based decay (lose ~1% of score per day of inactivity).
     */
    public void applyTimeDecay() {
        if (updatedAt == null) return;
        long daysSinceUpdate = (Instant.now().getEpochSecond() - updatedAt.getEpochSecond()) / 86400;
        if (daysSinceUpdate > 7) {
            // After 7 days of inactivity, decay 1% per day, max 30% decay
            int decayPercent = (int) Math.min(30, (daysSinceUpdate - 7));
            int decayAmount = score * decayPercent / 100;
            score = Math.max(0, score - decayAmount);
        }
    }

    /**
     * Get the intimacy tone for dialogue based on affection level.
     */
    public String getIntimacyTone() {
        return switch (level) {
            case 1, 2 -> "polite";       // 礼貌
            case 3, 4 -> "friendly";     // 友好
            case 5, 6 -> "warm";         // 温暖
            case 7, 8 -> "intimate";     // 亲密
            case 9, 10 -> "devoted";     // 挚爱
            default -> "neutral";
        };
    }

    /**
     * Get proactive greeting frequency multiplier (higher affection = more frequent greetings).
     */
    public float getGreetingFrequencyMultiplier() {
        return 1.0f + (level - 1) * 0.1f; // L1: 1.0x, L10: 1.9x
    }

    /**
     * Get surprise event quality level (higher affection = more personalized surprises).
     */
    public int getSurpriseQualityLevel() {
        return (level + 1) / 2; // L1-2: quality 1, L3-4: quality 2, ..., L9-10: quality 5
    }

    /**
     * Check if a specific interaction type is unlocked at this level.
     */
    public boolean isInteractionUnlocked(UnlockableInteraction interaction) {
        return level >= interaction.getRequiredLevel();
    }

    private void recalculateLevel() {
        for (int i = LEVEL_THRESHOLDS.length - 1; i >= 0; i--) {
            if (score >= LEVEL_THRESHOLDS[i]) {
                this.level = i + 1;
                this.title = LEVEL_TITLES[i];
                return;
            }
        }
        this.level = 1;
        this.title = LEVEL_TITLES[0];
    }

    // -- Getters --
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public int getScore() { return score; }
    public int getLevel() { return level; }
    public String getTitle() { return title; }
    public List<AffectionEvent> getHistory() { return history; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getDialogueQualityScore() { return dialogueQualityScore; }
    public int getInteractionFrequencyScore() { return interactionFrequencyScore; }
    public int getUserBehaviorScore() { return userBehaviorScore; }
    public int getTimeBonusScore() { return timeBonusScore; }

    /**
     * Affection source types with multipliers.
     */
    public enum AffectionSource {
        DEEP_CONVERSATION("dialogue_quality", 3),       // 深度对话
        CASUAL_CHAT("dialogue_quality", 1),              // 日常聊天
        SHARED_MEMORY("dialogue_quality", 4),            // 分享记忆
        DAILY_VISIT("interaction_frequency", 2),         // 每日访问
        EXTENDED_SESSION("interaction_frequency", 3),    // 长时间会话
        RETURN_AFTER_ABSENCE("interaction_frequency", 5),// 久别重逢
        GAVE_GIFT("user_behavior", 8),                   // 赠送礼物
        SHARED_SPACE("user_behavior", 6),                // 分享空间
        DEFENDED_AVATAR("user_behavior", 10),            // 维护虚拟人
        COMPLIMENT("user_behavior", 3),                  // 赞美
        CONSECUTIVE_DAYS("time_bonus", 1),               // 连续天数
        ANNIVERSARY("time_bonus", 15);                   // 纪念日

        private final String dimension;
        private final int multiplier;

        AffectionSource(String dimension, int multiplier) {
            this.dimension = dimension;
            this.multiplier = multiplier;
        }

        public String getDimension() { return dimension; }
        public int getMultiplier() { return multiplier; }
    }

    /**
     * Interaction types that unlock at specific affection levels.
     */
    public enum UnlockableInteraction {
        PERSONALIZED_GREETING(3),    // L3: 个性化称呼
        DAILY_CHECK_IN(4),           // L4: 每日问候
        PROACTIVE_CARE(5),           // L5: 主动关心
        SECRET_SHARING(6),           // L6: 分享秘密
        EMOTIONAL_RESONANCE(7),      // L7: 情感共鸣
        EXCLUSIVE_INTERACTION(9),    // L9: 专属互动
        FULL_TRUST(10);              // L10: 完全信任

        private final int requiredLevel;

        UnlockableInteraction(int requiredLevel) {
            this.requiredLevel = requiredLevel;
        }

        public int getRequiredLevel() { return requiredLevel; }
    }

    /**
     * Immutable record of an affection change event.
     */
    public record AffectionEvent(
        String source,
        int points,
        String reason,
        int oldLevel,
        int newLevel,
        Instant timestamp
    ) {}
}
