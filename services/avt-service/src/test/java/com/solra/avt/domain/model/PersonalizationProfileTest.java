package com.solra.avt.domain.model;

import com.solra.avt.domain.model.PersonalizationProfile.FeedbackType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PersonalizationProfile — AVT-005 个性化训练领域模型。
 */
@DisplayName("AVT-005: PersonalizationProfile Domain Model")
class PersonalizationProfileTest {

    private PersonalizationProfile profile;

    @BeforeEach
    void setUp() {
        profile = new PersonalizationProfile("user-001", "avatar-001");
    }

    @Test
    @DisplayName("Should initialize with balanced style weights")
    void shouldInitializeWithBalancedWeights() {
        assertTrue(profile.getCasualWeight() > 0);
        assertTrue(profile.getFormalWeight() > 0);
        assertTrue(profile.getHumorousWeight() > 0);
        assertTrue(profile.getGentleWeight() > 0);
        assertTrue(profile.getEnthusiasticWeight() > 0);

        // Weights should sum to ~1.0
        float sum = profile.getFormalWeight() + profile.getCasualWeight()
                + profile.getHumorousWeight() + profile.getGentleWeight()
                + profile.getEnthusiasticWeight();
        assertEquals(1.0f, sum, 0.01f);
    }

    @Test
    @DisplayName("Should start with neutral topic preferences")
    void shouldStartWithNeutralTopics() {
        Map<String, Float> topics = profile.getTopicPreferences();
        assertFalse(topics.isEmpty());

        // All default topics should have ~0.5 interest
        for (float interest : topics.values()) {
            assertEquals(0.5f, interest, 0.01f);
        }
    }

    @Test
    @DisplayName("Should update topic interest from conversation")
    void shouldUpdateTopicInterest() {
        profile.updateTopicInterest("科技", 0.3f);
        assertTrue(profile.getTopicPreferences().get("科技") > 0.5f);
    }

    @Test
    @DisplayName("Should adjust style weights from feedback")
    void shouldAdjustStyleFromFeedback() {
        float beforeHumorous = profile.getHumorousWeight();
        float beforeFormal = profile.getFormalWeight();

        profile.applyFeedback(FeedbackType.LIKED_HUMOR, 0.8f);

        assertTrue(profile.getHumorousWeight() > beforeHumorous,
                "Humorous weight should increase after LIKED_HUMOR feedback");
        assertTrue(profile.getFormalWeight() < beforeFormal,
                "Formal weight should decrease after LIKED_HUMOR feedback");
        assertEquals(1, profile.getTotalInteractions());
    }

    @Test
    @DisplayName("Should adjust verbosity from feedback")
    void shouldAdjustVerbosity() {
        float before = profile.getVerbosityPreference();

        profile.applyFeedback(FeedbackType.WANT_MORE_DETAIL, 1.0f);
        assertTrue(profile.getVerbosityPreference() > before);

        profile.applyFeedback(FeedbackType.WANT_SHORTER, 1.0f);
        assertTrue(profile.getVerbosityPreference() < 1.0f); // Should have decreased
    }

    @Test
    @DisplayName("Should adjust emoji usage from feedback")
    void shouldAdjustEmojiUsage() {
        float before = profile.getEmojiUsagePreference();

        profile.applyFeedback(FeedbackType.WANT_MORE_EMOJI, 1.0f);
        assertTrue(profile.getEmojiUsagePreference() > before);

        profile.applyFeedback(FeedbackType.WANT_LESS_EMOJI, 1.0f);
        assertTrue(profile.getEmojiUsagePreference() < 1.0f); // Should have decreased
    }

    @Test
    @DisplayName("Should adjust proactive level from feedback")
    void shouldAdjustProactiveLevel() {
        float before = profile.getProactiveLevel();

        profile.applyFeedback(FeedbackType.WANT_MORE_PROACTIVE, 1.0f);
        assertTrue(profile.getProactiveLevel() > before);

        profile.applyFeedback(FeedbackType.WANT_LESS_PROACTIVE, 1.0f);
        assertTrue(profile.getProactiveLevel() < 1.0f); // Should have decreased
    }

    @Test
    @DisplayName("Should identify dominant style")
    void shouldIdentifyDominantStyle() {
        // Initially casual is dominant (0.30)
        assertEquals("casual", profile.getDominantStyle());

        // Make formal dominant through feedback
        for (int i = 0; i < 10; i++) {
            profile.applyFeedback(FeedbackType.LIKED_SERIOUS, 1.0f);
        }

        assertEquals("formal", profile.getDominantStyle());
    }

    @Test
    @DisplayName("Should get top topics by interest")
    void shouldGetTopTopics() {
        profile.updateTopicInterest("游戏", 0.4f);
        profile.updateTopicInterest("音乐", 0.3f);
        profile.updateTopicInterest("科技", 0.2f);

        List<Map.Entry<String, Float>> topTopics = profile.getTopTopics(3);

        assertEquals(3, topTopics.size());
        assertEquals("游戏", topTopics.get(0).getKey());
        assertTrue(topTopics.get(0).getValue() > topTopics.get(2).getValue());
    }

    @Test
    @DisplayName("Should track positive and negative feedback")
    void shouldTrackFeedback() {
        profile.applyFeedback(FeedbackType.LIKED_HUMOR, 0.5f);
        profile.applyFeedback(FeedbackType.LIKED_GENTLE, 0.5f);
        profile.applyFeedback(FeedbackType.DISLIKED_RESPONSE, 0.5f);

        assertEquals(2, profile.getPositiveFeedbackCount());
        assertEquals(1, profile.getNegativeFeedbackCount());
        assertEquals(3, profile.getTotalInteractions());
    }

    @Test
    @DisplayName("Should calculate positive feedback rate")
    void shouldCalculatePositiveFeedbackRate() {
        // No feedback yet
        assertEquals(0.5f, profile.getPositiveFeedbackRate(), 0.01f);

        profile.applyFeedback(FeedbackType.LIKED_HUMOR, 0.5f);
        profile.applyFeedback(FeedbackType.LIKED_GENTLE, 0.5f);
        profile.applyFeedback(FeedbackType.DISLIKED_RESPONSE, 0.5f);

        // 2 positive out of 3
        assertEquals(2.0f / 3.0f, profile.getPositiveFeedbackRate(), 0.01f);
    }

    @Test
    @DisplayName("Should build system prompt customization string")
    void shouldBuildSystemPromptCustomization() {
        String customization = profile.buildSystemPromptCustomization();

        assertNotNull(customization);
        assertTrue(customization.contains("对话风格"));
        assertTrue(customization.contains("详细程度"));
        assertTrue(customization.contains("感兴趣话题"));
    }

    @Test
    @DisplayName("Should have default behavior templates")
    void shouldHaveDefaultBehaviorTemplates() {
        Map<String, Float> templates = profile.getBehaviorTemplateWeights();
        assertFalse(templates.isEmpty());
        assertEquals("friendly_helper", profile.getBestBehaviorTemplate());
    }

    @Test
    @DisplayName("Should normalize weights after feedback")
    void shouldNormalizeWeights() {
        // Apply many humorous feedbacks
        for (int i = 0; i < 20; i++) {
            profile.applyFeedback(FeedbackType.LIKED_HUMOR, 1.0f);
        }

        // Weights should still sum to ~1.0
        float sum = profile.getFormalWeight() + profile.getCasualWeight()
                + profile.getHumorousWeight() + profile.getGentleWeight()
                + profile.getEnthusiasticWeight();
        assertEquals(1.0f, sum, 0.01f);
    }
}
