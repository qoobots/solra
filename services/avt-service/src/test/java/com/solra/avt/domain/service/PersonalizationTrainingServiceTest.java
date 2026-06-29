package com.solra.avt.domain.service;

import com.solra.avt.domain.model.PersonalizationProfile;
import com.solra.avt.domain.model.PersonalizationProfile.FeedbackType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PersonalizationTrainingService — AVT-005 个性化训练领域服务。
 */
@DisplayName("AVT-005: PersonalizationTrainingService")
class PersonalizationTrainingServiceTest {

    private PersonalizationTrainingService service;

    @BeforeEach
    void setUp() {
        service = new PersonalizationTrainingService();
    }

    @Test
    @DisplayName("Should create personalization profile for new user-avatar pair")
    void shouldCreateNewProfile() {
        PersonalizationProfile profile = service.getOrCreate("user-001", "avatar-001");

        assertNotNull(profile);
        assertEquals("user-001", profile.getUserId());
        assertEquals("avatar-001", profile.getAvatarId());
        assertEquals("casual", profile.getDominantStyle());
    }

    @Test
    @DisplayName("Should return same instance for repeated calls")
    void shouldReturnSameInstance() {
        PersonalizationProfile p1 = service.getOrCreate("user-001", "avatar-001");
        PersonalizationProfile p2 = service.getOrCreate("user-001", "avatar-001");

        assertSame(p1, p2);
    }

    @Test
    @DisplayName("Should update topics from conversation content")
    void shouldUpdateTopicsFromConversation() {
        service.updateTopicsFromConversation("user-001", "avatar-001",
                "我最近在学习人工智能和编程，感觉科技发展太快了");

        PersonalizationProfile profile = service.getOrCreate("user-001", "avatar-001");
        assertTrue(profile.getTopicPreferences().get("科技") > 0.5f,
                "科技 topic should have increased interest");
    }

    @Test
    @DisplayName("Should update multiple topics from rich conversation")
    void shouldUpdateMultipleTopics() {
        service.updateTopicsFromConversation("user-001", "avatar-001",
                "昨天去看了电影，然后在餐厅吃了美食，周末还想去旅行");

        PersonalizationProfile profile = service.getOrCreate("user-001", "avatar-001");
        assertTrue(profile.getTopicPreferences().get("电影") > 0.5f);
        assertTrue(profile.getTopicPreferences().get("美食") > 0.5f);
        assertTrue(profile.getTopicPreferences().get("旅行") > 0.5f);
    }

    @Test
    @DisplayName("Should apply feedback to adjust style")
    void shouldApplyFeedback() {
        PersonalizationProfile profile = service.applyFeedback(
                "user-001", "avatar-001", FeedbackType.LIKED_HUMOR, 0.8f);

        assertEquals(1, profile.getTotalInteractions());
        assertEquals(1, profile.getPositiveFeedbackCount());
    }

    @Test
    @DisplayName("Should learn dominant style from repeated feedback")
    void shouldLearnDominantStyle() {
        // Apply serious feedback repeatedly
        for (int i = 0; i < 15; i++) {
            service.applyFeedback("user-001", "avatar-001",
                    FeedbackType.LIKED_SERIOUS, 1.0f);
        }

        assertEquals("formal", service.getDominantStyle("user-001", "avatar-001"));
    }

    @Test
    @DisplayName("Should build system prompt customization")
    void shouldBuildSystemPrompt() {
        // Train with specific preferences
        for (int i = 0; i < 10; i++) {
            service.applyFeedback("user-001", "avatar-001",
                    FeedbackType.WANT_MORE_DETAIL, 1.0f);
            service.applyFeedback("user-001", "avatar-001",
                    FeedbackType.WANT_MORE_EMOJI, 1.0f);
        }
        service.updateTopicsFromConversation("user-001", "avatar-001",
                "我特别喜欢游戏，每天都在玩游戏和看电影");

        String customization = service.buildSystemPromptCustomization("user-001", "avatar-001");

        assertNotNull(customization);
        assertTrue(customization.contains("详细"));
        assertTrue(customization.contains("频繁"));
        assertTrue(customization.contains("感兴趣话题"));
    }

    @Test
    @DisplayName("Should get top topics")
    void shouldGetTopTopics() {
        service.updateTopicsFromConversation("user-001", "avatar-001",
                "游戏游戏游戏游戏游戏"); // Multiple mentions
        service.updateTopicsFromConversation("user-001", "avatar-001",
                "音乐音乐音乐"); // Fewer mentions
        service.updateTopicsFromConversation("user-001", "avatar-001",
                "科技"); // Single mention

        List<Map.Entry<String, Float>> topTopics = service.getTopTopics("user-001", "avatar-001", 3);

        assertEquals(3, topTopics.size());
        assertTrue(topTopics.get(0).getValue() >= topTopics.get(1).getValue());
        assertTrue(topTopics.get(1).getValue() >= topTopics.get(2).getValue());
    }

    @Test
    @DisplayName("Should get verbosity preference")
    void shouldGetVerbosityPreference() {
        float initial = service.getVerbosityPreference("user-001", "avatar-001");
        assertEquals(0.5f, initial, 0.01f);

        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.WANT_MORE_DETAIL, 1.0f);

        assertTrue(service.getVerbosityPreference("user-001", "avatar-001") > initial);
    }

    @Test
    @DisplayName("Should get emoji usage preference")
    void shouldGetEmojiPreference() {
        float initial = service.getEmojiUsagePreference("user-001", "avatar-001");

        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.WANT_MORE_EMOJI, 1.0f);

        assertTrue(service.getEmojiUsagePreference("user-001", "avatar-001") > initial);
    }

    @Test
    @DisplayName("Should get proactive level preference")
    void shouldGetProactiveLevel() {
        float initial = service.getProactiveLevel("user-001", "avatar-001");

        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.WANT_MORE_PROACTIVE, 1.0f);

        assertTrue(service.getProactiveLevel("user-001", "avatar-001") > initial);
    }

    @Test
    @DisplayName("Should track positive feedback rate")
    void shouldTrackPositiveFeedbackRate() {
        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.LIKED_HUMOR, 0.5f);
        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.LIKED_GENTLE, 0.5f);
        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.DISLIKED_RESPONSE, 0.5f);

        float rate = service.getPositiveFeedbackRate("user-001", "avatar-001");
        assertEquals(2.0f / 3.0f, rate, 0.01f);
    }

    @Test
    @DisplayName("Should get best behavior template")
    void shouldGetBestBehaviorTemplate() {
        String template = service.getBestBehaviorTemplate("user-001", "avatar-001");
        assertEquals("friendly_helper", template); // Default
    }

    @Test
    @DisplayName("Should get total interactions count")
    void shouldGetTotalInteractions() {
        assertEquals(0, service.getTotalInteractions("user-001", "avatar-001"));

        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.LIKED_HUMOR, 0.5f);
        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.LIKED_GENTLE, 0.5f);

        assertEquals(2, service.getTotalInteractions("user-001", "avatar-001"));
    }

    @Test
    @DisplayName("Should get all user profiles across avatars")
    void shouldGetAllUserProfiles() {
        service.getOrCreate("user-001", "avatar-001");
        service.getOrCreate("user-001", "avatar-002");
        service.getOrCreate("user-001", "avatar-003");

        List<PersonalizationProfile> profiles = service.getUserProfiles("user-001");

        assertEquals(3, profiles.size());
    }

    @Test
    @DisplayName("Should get recent feedback history")
    void shouldGetRecentFeedback() {
        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.LIKED_HUMOR, 0.5f);
        service.applyFeedback("user-001", "avatar-001",
                FeedbackType.WANT_MORE_DETAIL, 0.8f);

        var history = service.getRecentFeedback("user-001", "avatar-001");

        assertEquals(2, history.size());
        assertEquals(FeedbackType.LIKED_HUMOR, history.get(0).type());
        assertEquals(FeedbackType.WANT_MORE_DETAIL, history.get(1).type());
    }
}
