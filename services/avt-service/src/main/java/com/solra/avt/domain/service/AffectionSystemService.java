package com.solra.avt.domain.service;

import com.solra.avt.domain.model.AffectionLevel;
import com.solra.avt.domain.model.AffectionLevel.AffectionSource;
import com.solra.avt.domain.model.AffectionLevel.UnlockableInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AffectionSystemService — AVT-009 好感度系统领域服务。
 *
 * 管理用户与虚拟人之间的好感度生命周期：
 * - 好感度计算（多维度加权）
 * - 等级提升/降低规则
 * - 好感度对对话行为的影响
 * - 与 GreetingTrigger、SurpriseEngine、FiveDimensionalEmotion 的集成点
 */
public class AffectionSystemService {

    private static final Logger log = LoggerFactory.getLogger(AffectionSystemService.class);

    // In-memory store: key = userId::avatarId
    private final Map<String, AffectionLevel> affectionStore = new ConcurrentHashMap<>();

    /**
     * Get or create affection level for a user-avatar pair.
     */
    public AffectionLevel getOrCreate(String userId, String avatarId) {
        String key = buildKey(userId, avatarId);
        return affectionStore.computeIfAbsent(key, k -> {
            log.info("AVT-009 Created affection record: user={} avatar={}", userId, avatarId);
            return new AffectionLevel(userId, avatarId);
        });
    }

    /**
     * Record an affection event from a specific source.
     * Returns the new level if leveled up, or -1 if no change.
     */
    public int recordAffection(String userId, String avatarId,
                                AffectionSource source, int basePoints, String reason) {
        AffectionLevel affection = getOrCreate(userId, avatarId);
        int newLevel = affection.addAffection(source, basePoints, reason);

        if (newLevel > 0) {
            log.info("AVT-009 Affection leveled up: user={} avatar={} level={} title={} score={}",
                    userId, avatarId, newLevel, affection.getTitle(), affection.getScore());
        }

        return newLevel;
    }

    /**
     * Record affection after a conversation turn.
     * Automatically determines points based on conversation quality.
     */
    public void recordConversationAffection(String userId, String avatarId,
                                             int messageLength, boolean hasEmotion,
                                             boolean hasRecall, int turnCount) {
        // Base: longer messages = more affection
        int basePoints = Math.min(10, messageLength / 50);

        // Bonus: emotional content
        if (hasEmotion) basePoints += 3;

        // Bonus: memory recall
        if (hasRecall) basePoints += 5;

        // Deep conversation detection
        AffectionSource source = (turnCount > 10 && messageLength > 100)
                ? AffectionSource.DEEP_CONVERSATION
                : AffectionSource.CASUAL_CHAT;

        recordAffection(userId, avatarId, source, basePoints,
                "Conversation turn #" + turnCount);
    }

    /**
     * Record daily visit affection.
     */
    public void recordDailyVisit(String userId, String avatarId, int consecutiveDays) {
        recordAffection(userId, avatarId, AffectionSource.DAILY_VISIT, 5,
                "Daily visit (day " + consecutiveDays + ")");

        if (consecutiveDays > 1) {
            recordAffection(userId, avatarId, AffectionSource.CONSECUTIVE_DAYS,
                    Math.min(consecutiveDays, 30),
                    consecutiveDays + " consecutive days");
        }
    }

    /**
     * Record extended session affection.
     */
    public void recordExtendedSession(String userId, String avatarId, int durationMinutes) {
        if (durationMinutes >= 15) {
            int points = durationMinutes / 5; // 3 points per 15 min, 6 per 30 min
            recordAffection(userId, avatarId, AffectionSource.EXTENDED_SESSION, points,
                    durationMinutes + " min session");
        }
    }

    /**
     * Get affection level for a user-avatar pair.
     */
    public Optional<AffectionLevel> getAffection(String userId, String avatarId) {
        String key = buildKey(userId, avatarId);
        return Optional.ofNullable(affectionStore.get(key));
    }

    /**
     * Get the intimacy tone for dialogue generation.
     */
    public String getIntimacyTone(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getIntimacyTone();
    }

    /**
     * Get greeting frequency multiplier based on affection.
     */
    public float getGreetingFrequencyMultiplier(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getGreetingFrequencyMultiplier();
    }

    /**
     * Get surprise quality level based on affection.
     */
    public int getSurpriseQualityLevel(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getSurpriseQualityLevel();
    }

    /**
     * Check if a specific interaction is unlocked at current affection level.
     */
    public boolean isInteractionUnlocked(String userId, String avatarId,
                                          UnlockableInteraction interaction) {
        return getOrCreate(userId, avatarId).isInteractionUnlocked(interaction);
    }

    /**
     * Get affection history for a user-avatar pair.
     */
    public List<AffectionLevel.AffectionEvent> getAffectionHistory(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getHistory();
    }

    /**
     * Get top N avatars by affection score for a user.
     */
    public List<AffectionLevel> getTopAvatars(String userId, int limit) {
        return affectionStore.entrySet().stream()
                .filter(e -> e.getKey().startsWith(userId + "::"))
                .map(Map.Entry::getValue)
                .sorted(Comparator.comparingInt(AffectionLevel::getScore).reversed())
                .limit(limit)
                .toList();
    }

    /**
     * Apply time decay to all affection records for a user.
     */
    public void applyTimeDecay(String userId) {
        affectionStore.entrySet().stream()
                .filter(e -> e.getKey().startsWith(userId + "::"))
                .forEach(e -> e.getValue().applyTimeDecay());
    }

    /**
     * Get the overall affection score (for displaying progress bar).
     */
    public int getAffectionScore(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getScore();
    }

    /**
     * Get progress to next level as percentage (0-100).
     */
    public int getLevelProgress(String userId, String avatarId) {
        AffectionLevel affection = getOrCreate(userId, avatarId);
        int currentLevel = affection.getLevel();
        if (currentLevel >= 10) return 100;

        int currentThreshold = AffectionLevel.LEVEL_THRESHOLDS[currentLevel - 1];
        int nextThreshold = AffectionLevel.LEVEL_THRESHOLDS[currentLevel];
        int score = affection.getScore();

        return (score - currentThreshold) * 100 / (nextThreshold - currentThreshold);
    }

    private String buildKey(String userId, String avatarId) {
        return userId + "::" + avatarId;
    }
}
