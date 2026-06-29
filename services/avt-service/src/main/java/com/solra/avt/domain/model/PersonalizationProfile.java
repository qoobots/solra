package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * PersonalizationProfile — AVT-005 虚拟人个性化训练。
 *
 * 根据用户交互历史调整虚拟人行为偏好。
 * 支持：
 * - 对话风格偏好（正式/随意/幽默/温柔）
 * - 话题偏好（用户感兴趣的话题）
 * - 回应速度偏好（即时/思考后）
 * - 互动频率偏好（频繁/适度/偶尔）
 * - 基于反馈的持续学习
 */
public class PersonalizationProfile {

    private String userId;
    private String avatarId;

    // 对话风格权重 (0.0-1.0, sum=1.0)
    private float formalWeight;       // 正式
    private float casualWeight;       // 随意
    private float humorousWeight;     // 幽默
    private float gentleWeight;       // 温柔
    private float enthusiasticWeight; // 热情

    // 话题偏好 (topic -> interest score 0.0-1.0)
    private Map<String, Float> topicPreferences;

    // 回应偏好
    private float responseSpeedPreference;  // 0=即时, 1=思考后 (慢)
    private float verbosityPreference;      // 0=简洁, 1=详细
    private float emojiUsagePreference;     // 0=不用, 1=频繁用
    private float proactiveLevel;           // 0=被动, 1=主动

    // 行为模板选择权重
    private Map<String, Float> behaviorTemplateWeights;

    // 训练数据统计
    private int totalInteractions;        // 总互动次数
    private int positiveFeedbackCount;    // 正面反馈次数
    private int negativeFeedbackCount;    // 负面反馈次数
    private List<FeedbackRecord> recentFeedback; // 最近反馈记录

    private Instant createdAt;
    private Instant updatedAt;

    // 默认话题列表
    public static final String[] DEFAULT_TOPICS = {
        "科技", "艺术", "音乐", "旅行", "美食",
        "游戏", "电影", "运动", "哲学", "生活"
    };

    public PersonalizationProfile() {
        // Default style: balanced
        this.formalWeight = 0.15f;
        this.casualWeight = 0.30f;
        this.humorousWeight = 0.20f;
        this.gentleWeight = 0.20f;
        this.enthusiasticWeight = 0.15f;

        // Default topic preferences (neutral)
        this.topicPreferences = new HashMap<>();
        for (String topic : DEFAULT_TOPICS) {
            topicPreferences.put(topic, 0.5f);
        }

        // Default response preferences
        this.responseSpeedPreference = 0.5f;
        this.verbosityPreference = 0.5f;
        this.emojiUsagePreference = 0.3f;
        this.proactiveLevel = 0.4f;

        // Default behavior templates
        this.behaviorTemplateWeights = new HashMap<>();
        this.behaviorTemplateWeights.put("friendly_helper", 0.4f);
        this.behaviorTemplateWeights.put("storyteller", 0.2f);
        this.behaviorTemplateWeights.put("curious_explorer", 0.2f);
        this.behaviorTemplateWeights.put("wise_mentor", 0.1f);
        this.behaviorTemplateWeights.put("playful_companion", 0.1f);

        this.recentFeedback = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public PersonalizationProfile(String userId, String avatarId) {
        this();
        this.userId = userId;
        this.avatarId = avatarId;
    }

    /**
     * Update topic interest based on conversation content.
     */
    public void updateTopicInterest(String topic, float interestDelta) {
        topicPreferences.merge(topic, Math.max(0, Math.min(1, 0.5f + interestDelta)),
                (old, delta) -> Math.max(0, Math.min(1, old + delta)));
        this.updatedAt = Instant.now();
    }

    /**
     * Apply feedback to adjust conversation style preferences.
     */
    public void applyFeedback(FeedbackType type, float intensity) {
        this.totalInteractions++;

        float adjustment = intensity * 0.05f; // Small incremental adjustments

        switch (type) {
            case LIKED_HUMOR -> {
                humorousWeight = clamp(humorousWeight + adjustment);
                formalWeight = clamp(formalWeight - adjustment * 0.5f);
                positiveFeedbackCount++;
            }
            case LIKED_SERIOUS -> {
                formalWeight = clamp(formalWeight + adjustment);
                humorousWeight = clamp(humorousWeight - adjustment * 0.5f);
                positiveFeedbackCount++;
            }
            case LIKED_GENTLE -> {
                gentleWeight = clamp(gentleWeight + adjustment);
                enthusiasticWeight = clamp(enthusiasticWeight - adjustment * 0.3f);
                positiveFeedbackCount++;
            }
            case LIKED_ENTHUSIASTIC -> {
                enthusiasticWeight = clamp(enthusiasticWeight + adjustment);
                gentleWeight = clamp(gentleWeight - adjustment * 0.3f);
                positiveFeedbackCount++;
            }
            case WANT_MORE_DETAIL -> verbosityPreference = clamp(verbosityPreference + adjustment);
            case WANT_SHORTER -> verbosityPreference = clamp(verbosityPreference - adjustment);
            case WANT_MORE_EMOJI -> emojiUsagePreference = clamp(emojiUsagePreference + adjustment);
            case WANT_LESS_EMOJI -> emojiUsagePreference = clamp(emojiUsagePreference - adjustment);
            case WANT_MORE_PROACTIVE -> proactiveLevel = clamp(proactiveLevel + adjustment);
            case WANT_LESS_PROACTIVE -> proactiveLevel = clamp(proactiveLevel - adjustment);
            case DISLIKED_RESPONSE -> negativeFeedbackCount++;
        }

        // Normalize style weights
        normalizeWeights();

        // Record feedback
        recentFeedback.add(new FeedbackRecord(type, intensity, Instant.now()));
        if (recentFeedback.size() > 100) {
            recentFeedback = recentFeedback.subList(recentFeedback.size() - 100, recentFeedback.size());
        }

        this.updatedAt = Instant.now();
    }

    /**
     * Get the dominant conversation style.
     */
    public String getDominantStyle() {
        Map<String, Float> styles = new LinkedHashMap<>();
        styles.put("formal", formalWeight);
        styles.put("casual", casualWeight);
        styles.put("humorous", humorousWeight);
        styles.put("gentle", gentleWeight);
        styles.put("enthusiastic", enthusiasticWeight);

        return styles.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("casual");
    }

    /**
     * Get top N topic interests.
     */
    public List<Map.Entry<String, Float>> getTopTopics(int n) {
        return topicPreferences.entrySet().stream()
                .sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .limit(n)
                .toList();
    }

    /**
     * Get the best behavior template for this user.
     */
    public String getBestBehaviorTemplate() {
        return behaviorTemplateWeights.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("friendly_helper");
    }

    /**
     * Get system prompt customization based on learned preferences.
     */
    public String buildSystemPromptCustomization() {
        StringBuilder sb = new StringBuilder();
        sb.append("对话风格: ").append(getDominantStyle()).append("; ");
        sb.append("详细程度: ").append(verbosityPreference > 0.6 ? "详细" : "简洁").append("; ");
        sb.append("表情使用: ").append(emojiUsagePreference > 0.5 ? "频繁" : "适度").append("; ");
        sb.append("主动性: ").append(proactiveLevel > 0.5 ? "主动" : "被动").append("; ");

        List<Map.Entry<String, Float>> topTopics = getTopTopics(3);
        sb.append("感兴趣话题: ");
        for (var t : topTopics) {
            sb.append(t.getKey()).append("(").append(String.format("%.0f%%", t.getValue() * 100)).append("), ");
        }
        return sb.toString();
    }

    /**
     * Get positive feedback rate (0-1).
     */
    public float getPositiveFeedbackRate() {
        if (totalInteractions == 0) return 0.5f;
        return (float) positiveFeedbackCount / totalInteractions;
    }

    private void normalizeWeights() {
        float sum = formalWeight + casualWeight + humorousWeight + gentleWeight + enthusiasticWeight;
        if (sum > 0) {
            formalWeight /= sum;
            casualWeight /= sum;
            humorousWeight /= sum;
            gentleWeight /= sum;
            enthusiasticWeight /= sum;
        }
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    // -- Getters and Setters --
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public float getFormalWeight() { return formalWeight; }
    public float getCasualWeight() { return casualWeight; }
    public float getHumorousWeight() { return humorousWeight; }
    public float getGentleWeight() { return gentleWeight; }
    public float getEnthusiasticWeight() { return enthusiasticWeight; }
    public Map<String, Float> getTopicPreferences() { return topicPreferences; }
    public float getResponseSpeedPreference() { return responseSpeedPreference; }
    public float getVerbosityPreference() { return verbosityPreference; }
    public float getEmojiUsagePreference() { return emojiUsagePreference; }
    public float getProactiveLevel() { return proactiveLevel; }
    public Map<String, Float> getBehaviorTemplateWeights() { return behaviorTemplateWeights; }
    public int getTotalInteractions() { return totalInteractions; }
    public int getPositiveFeedbackCount() { return positiveFeedbackCount; }
    public int getNegativeFeedbackCount() { return negativeFeedbackCount; }
    public List<FeedbackRecord> getRecentFeedback() { return recentFeedback; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Feedback types for personalization training.
     */
    public enum FeedbackType {
        LIKED_HUMOR,
        LIKED_SERIOUS,
        LIKED_GENTLE,
        LIKED_ENTHUSIASTIC,
        WANT_MORE_DETAIL,
        WANT_SHORTER,
        WANT_MORE_EMOJI,
        WANT_LESS_EMOJI,
        WANT_MORE_PROACTIVE,
        WANT_LESS_PROACTIVE,
        DISLIKED_RESPONSE
    }

    /**
     * Immutable feedback record.
     */
    public record FeedbackRecord(FeedbackType type, float intensity, Instant timestamp) {}
}
