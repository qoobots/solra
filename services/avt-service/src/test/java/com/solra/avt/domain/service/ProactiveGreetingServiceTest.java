package com.solra.avt.domain.service;

import com.solra.avt.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProactiveGreetingService covering AVT-002.
 */
@DisplayName("ProactiveGreetingService")
class ProactiveGreetingServiceTest {

    private ProactiveGreetingService proactiveGreetingService;

    private static final String TEST_USER_ID = "user-001";
    private static final String TEST_SPACE_ID = "space-001";
    private static final String TEST_AVATAR_ID = "avatar-001";

    @BeforeEach
    void setUp() {
        proactiveGreetingService = new ProactiveGreetingService();
    }

    // ============================================================
    // AVT-002: handlePresenceEvent
    // ============================================================
    @Nested
    @DisplayName("handlePresenceEvent (AVT-002)")
    class HandlePresenceEventTests {

        @Test
        @DisplayName("should return empty when event is null")
        void shouldReturnEmptyForNullEvent() {
            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");
            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(null, avatar);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty when avatar is null")
        void shouldReturnEmptyForNullAvatar() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_ENTERED);
            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for USER_LEFT event")
        void shouldReturnEmptyForUserLeft() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_LEFT);
            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, avatar);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should generate FIRST_TIME_WELCOME for new user entering")
        void shouldGenerateFirstTimeWelcome() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_ENTERED);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(true);
            event.setUserContext(ctx);

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, avatar);

            assertThat(result).isPresent();
            assertThat(result.get().getTrigger()).isEqualTo(GreetingTrigger.FIRST_TIME_WELCOME);
            assertThat(result.get().getMessage()).contains("Solra AI");
            assertThat(result.get().getEnthusiasm()).isGreaterThanOrEqualTo(0.8f);
            assertThat(result.get().getSuggestedAnimation()).isEqualTo("wave_enthusiastic");
        }

        @Test
        @DisplayName("should generate RETURNING_GREETING for user with long absence")
        void shouldGenerateReturningGreeting() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_ENTERED);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(false);
            ctx.setTimeSinceLastVisit(604801); // > 7 days
            ctx.setDisplayName("TestUser");
            event.setUserContext(ctx);

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, avatar);

            assertThat(result).isPresent();
            assertThat(result.get().getTrigger()).isEqualTo(GreetingTrigger.RETURNING_GREETING);
            assertThat(result.get().getMessage()).contains("Solra AI");
        }

        @Test
        @DisplayName("should generate APPROACH_REACTION for USER_APPROACHED")
        void shouldGenerateApproachReaction() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_APPROACHED);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(false);
            ctx.setTotalVisits(3);
            event.setUserContext(ctx);

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, avatar);

            assertThat(result).isPresent();
            assertThat(result.get().getTrigger()).isEqualTo(GreetingTrigger.APPROACH_REACTION);
            assertThat(result.get().getSuggestedAnimation()).isEqualTo("nod_smile");
        }

        @Test
        @DisplayName("should generate ENGAGED_USER_PROMPT for lingering user with >5 visits")
        void shouldGenerateEngagedUserPrompt() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_LINGERING);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(false);
            ctx.setTotalVisits(10);
            event.setUserContext(ctx);

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, avatar);

            assertThat(result).isPresent();
            assertThat(result.get().getTrigger()).isEqualTo(GreetingTrigger.ENGAGED_USER_PROMPT);
        }

        @Test
        @DisplayName("should generate LINGERING_PROMPT for lingering user with <=5 visits")
        void shouldGenerateLingeringPrompt() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_LINGERING);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(false);
            ctx.setTotalVisits(3);
            event.setUserContext(ctx);

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, avatar);

            assertThat(result).isPresent();
            assertThat(result.get().getTrigger()).isEqualTo(GreetingTrigger.LINGERING_PROMPT);
        }

        @Test
        @DisplayName("should return empty when in cooldown period")
        void shouldReturnEmptyWhenInCooldown() {
            PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_APPROACHED);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(false);
            event.setUserContext(ctx);

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");

            // First call should succeed
            Optional<ProactiveAction> first = proactiveGreetingService.handlePresenceEvent(event, avatar);
            assertThat(first).isPresent();

            // Second call within cooldown should be empty
            Optional<ProactiveAction> second = proactiveGreetingService.handlePresenceEvent(event, avatar);
            assertThat(second).isEmpty();
        }
    }

    // ============================================================
    // onUsersEntered — batch processing
    // ============================================================
    @Nested
    @DisplayName("onUsersEntered (batch)")
    class OnUsersEnteredTests {

        @Test
        @DisplayName("should generate actions for multiple entering users")
        void shouldGenerateActionsForMultipleUsers() {
            PresenceEvent event1 = new PresenceEvent("user-1", TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_ENTERED);
            PresenceEvent.UserContext ctx1 = new PresenceEvent.UserContext();
            ctx1.setNewUser(true);
            event1.setUserContext(ctx1);

            PresenceEvent event2 = new PresenceEvent("user-2", TEST_SPACE_ID, TEST_AVATAR_ID,
                    PresenceEvent.PresenceEventType.USER_ENTERED);
            PresenceEvent.UserContext ctx2 = new PresenceEvent.UserContext();
            ctx2.setNewUser(true);
            event2.setUserContext(ctx2);

            List<PresenceEvent> events = List.of(event1, event2);
            List<Avatar> avatars = List.of(new Avatar(TEST_AVATAR_ID, "Solra AI"));

            List<ProactiveAction> actions = proactiveGreetingService.onUsersEntered(events, avatars);

            assertThat(actions).hasSize(2);
        }

        @Test
        @DisplayName("should skip events without matching avatar")
        void shouldSkipEventsWithoutMatchingAvatar() {
            PresenceEvent event = new PresenceEvent("user-1", TEST_SPACE_ID, "other-avatar",
                    PresenceEvent.PresenceEventType.USER_ENTERED);
            PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
            ctx.setNewUser(true);
            event.setUserContext(ctx);

            List<PresenceEvent> events = List.of(event);
            List<Avatar> avatars = List.of(new Avatar(TEST_AVATAR_ID, "Solra AI"));

            List<ProactiveAction> actions = proactiveGreetingService.onUsersEntered(events, avatars);

            assertThat(actions).isEmpty();
        }
    }

    // ============================================================
    // Rate limit tests
    // ============================================================
    @Nested
    @DisplayName("rate limiting")
    class RateLimitTests {

        @Test
        @DisplayName("should enforce max 5 proactive actions per hour per user")
        void shouldEnforceMaxProactivePerHour() {
            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");
            int successCount = 0;
            int emptyCount = 0;

            // Try 10 times — only first 5 should succeed
            for (int i = 0; i < 10; i++) {
                PresenceEvent event = new PresenceEvent(TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID,
                        PresenceEvent.PresenceEventType.USER_APPROACHED);
                PresenceEvent.UserContext ctx = new PresenceEvent.UserContext();
                ctx.setNewUser(false);
                ctx.setTotalVisits(i + 1);
                event.setUserContext(ctx);

                // Need unique avatarId per attempt to bypass cooldown
                Avatar uniqueAvatar = new Avatar("avatar-" + i, "Solra AI " + i);
                Optional<ProactiveAction> result = proactiveGreetingService.handlePresenceEvent(event, uniqueAvatar);

                if (result.isPresent()) {
                    successCount++;
                } else {
                    emptyCount++;
                }
            }

            // First 5 should succeed (different avatars bypass cooldown, same user hits rate limit)
            // After 5 attempts on same user, rate limit kicks in
            assertThat(successCount).isEqualTo(5);
            assertThat(emptyCount).isEqualTo(5);
        }
    }
}
