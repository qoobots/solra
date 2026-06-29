package com.solra.avt.domain.service;

import com.solra.avt.domain.model.PersonalizationProfile;
import com.solra.avt.domain.model.PersonalizationProfile.FeedbackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PersonalizationTrainingService — AVT-005 虚拟人个性化训练领域服务。
 *
 * 根据用户交互历史持续调整虚拟人的行为偏好：
 * - 对话风格学习（正式/随意/幽默/温柔）
 * - 话题偏好学习
 * - 回应风格学习
 * - 行为模板选择
 * - 反馈驱动的持续优化
 */
public class PersonalizationTrainingService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizationTrainingService.class);

    // In-memory store: key = userId::avatarId
    private final Map<String, PersonalizationProfile> profiles = new ConcurrentHashMap<>();

    /**
     * Get or create personalization profile for a user-avatar pair.
     */
    public PersonalizationProfile getOrCreate(String userId, String avatarId) {
        String key = buildKey(userId, avatarId);
        return profiles.computeIfAbsent(key, k -> {
            log.info("AVT-005 Created personalization profile: user={} avatar={}", userId, avatarId);
            return new PersonalizationProfile(userId, avatarId);
        });
    }

    /**
     * Get personalization profile if exists.
     */
    public Optional<PersonalizationProfile> getProfile(String userId, String avatarId) {
        return Optional.ofNullable(profiles.get(buildKey(userId, avatarId)));
    }

    /**
     * Update topic interest based on conversation keywords.
     */
    public void updateTopicsFromConversation(String userId, String avatarId, String messageContent) {
        PersonalizationProfile profile = getOrCreate(userId, avatarId);
        String lower = messageContent.toLowerCase();

        // Topic keyword matching
        Map<String, String[]> topicKeywords = Map.of(
            "科技", new String[]{"科技", "ai", "人工智能", "编程", "代码", "软件", "硬件", "机器人"},
            "艺术", new String[]{"艺术", "绘画", "设计", "美学", "雕塑", "摄影"},
            "音乐", new String[]{"音乐", "歌曲", "旋律", "乐器", "演唱会", "歌"},
            "旅行", new String[]{"旅行", "旅游", "景点", "酒店", "机票", "风景"},
            "美食", new String[]{"美食", "好吃", "餐厅", "料理", "烹饪", "食物"},
            "游戏", new String[]{"游戏", "电竞", "主机", "手游", "switch", "steam"},
            "电影", new String[]{"电影", "影院", "导演", "演员", "剧情", "大片"},
            "运动", new String[]{"运动", "健身", "跑步", "篮球", "足球", "游泳"},
            "哲学", new String[]{"哲学", "人生", "意义", "存在", "思考", "价值"},
            "生活", new String[]{"生活", "日常", "工作", "学习", "家庭", "朋友"}
        );

        for (var entry : topicKeywords.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lower.contains(kw)) {
                    profile.updateTopicInterest(entry.getKey(), 0.05f);
                    break;
                }
            }
        }
    }

    /**
     * Apply user feedback to refine personalization.
     */
    public PersonalizationProfile applyFeedback(String userId, String avatarId,
                                                  FeedbackType type, float intensity) {
        PersonalizationProfile profile = getOrCreate(userId, avatarId);
        profile.applyFeedback(type, intensity);
        log.debug("AVT-005 Feedback applied: user={} avatar={} type={} intensity={}",
                userId, avatarId, type, intensity);
        return profile;
    }

    /**
     * Get the dominant conversation style for personalized dialogue.
     */
    public String getDominantStyle(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getDominantStyle();
    }

    /**
     * Build a system prompt customization string based on learned preferences.
     */
    public String buildSystemPromptCustomization(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).buildSystemPromptCustomization();
    }

    /**
     * Get top N topic interests for the user.
     */
    public List<Map.Entry<String, Float>> getTopTopics(String userId, String avatarId, int n) {
        return getOrCreate(userId, avatarId).getTopTopics(n);
    }

    /**
     * Get verbosity preference (for controlling response length).
     */
    public float getVerbosityPreference(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getVerbosityPreference();
    }

    /**
     * Get emoji usage preference.
     */
    public float getEmojiUsagePreference(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getEmojiUsagePreference();
    }

    /**
     * Get proactive level preference.
     */
    public float getProactiveLevel(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getProactiveLevel();
    }

    /**
     * Get positive feedback rate.
     */
    public float getPositiveFeedbackRate(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getPositiveFeedbackRate();
    }

    /**
     * Get the best behavior template for this user.
     */
    public String getBestBehaviorTemplate(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getBestBehaviorTemplate();
    }

    /**
     * Get total interaction count.
     */
    public int getTotalInteractions(String userId, String avatarId) {
        return getOrCreate(userId, avatarId).getTotalInteractions();
    }

    /**
     * Get all profiles for a specific user (across all avatars).
     */
    public List<PersonalizationProfile> getUserProfiles(String userId) {
        return profiles.entrySet().stream()
                .filter(e -> e.getKey().startsWith(userId + "::"))
                .map(Map.Entry::getValue)
                .toList();
    }

    /**
     * Get recent feedback history.
     */
    public List<PersonalizationProfile.FeedbackRecord> getRecentFeedback(String userId,
                                                                           String avatarId) {
        return getOrCreate(userId, avatarId).getRecentFeedback();
    }

    private String buildKey(String userId, String avatarId) {
        return userId + "::" + avatarId;
    }
}
