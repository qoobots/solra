package com.solra.avt.domain.service;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AvatarDomainService covering AVT-001 and AVT-012.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AvatarDomainService")
class AvatarDomainServiceTest {

    @Mock
    private InferenceEngine inferenceEngine;
    @Mock
    private ConversationRepository conversationRepo;
    @Mock
    private DialogueTurnRepository turnRepo;
    @Mock
    private AvatarRepository avatarRepo;
    @Mock
    private MemoryRepository memoryRepo;

    private AvatarDomainService avatarDomainService;

    private static final String TEST_USER_ID = "user-001";
    private static final String TEST_SPACE_ID = "space-001";
    private static final String TEST_CONV_ID = "conv-001";
    private static final String TEST_AVATAR_ID = "default-avatar";

    @BeforeEach
    void setUp() {
        avatarDomainService = new AvatarDomainService(
                inferenceEngine, conversationRepo, turnRepo, avatarRepo, memoryRepo
        );
    }

    // ============================================================
    // AVT-001: sendMessage — 虚拟人实时对话
    // ============================================================
    @Nested
    @DisplayName("sendMessage (AVT-001)")
    class SendMessageTests {

        @Test
        @DisplayName("should process message and return avatar response for existing conversation")
        void shouldProcessMessageForExistingConversation() {
            Conversation conv = new Conversation(TEST_CONV_ID, TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID);
            when(conversationRepo.findById(TEST_CONV_ID)).thenReturn(Optional.of(conv));
            when(turnRepo.findByConversationId(eq(TEST_CONV_ID), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(memoryRepo.findByUserId(anyString(), anyList(), anyFloat(), anyInt()))
                    .thenReturn(Collections.emptyList());

            EmotionState detected = new EmotionState(EmotionCategory.JOY, 0.8f);
            InferenceEngine.InferenceResult mockResult = new InferenceEngine.InferenceResult(
                    "你好！欢迎回来！", detected, Map.of("engine", "mock")
            );
            when(inferenceEngine.infer(eq(TEST_CONV_ID), anyString(), anyList(), anyMap()))
                    .thenReturn(mockResult);

            DialogueTurn result = avatarDomainService.sendMessage(
                    TEST_USER_ID, TEST_SPACE_ID, TEST_CONV_ID,
                    "你好！", List.of(), Map.of()
            );

            assertThat(result).isNotNull();
            assertThat(result.getRole()).isEqualTo(TurnRole.AVATAR);
            assertThat(result.getContent()).isEqualTo("你好！欢迎回来！");
            verify(turnRepo, times(2)).save(any(DialogueTurn.class)); // user + avatar
            verify(conversationRepo).save(any(Conversation.class));
        }

        @Test
        @DisplayName("should create conversation and avatar if not exist")
        void shouldCreateConversationIfNotExist() {
            when(conversationRepo.findById(TEST_CONV_ID)).thenReturn(Optional.empty());
            when(turnRepo.findByConversationId(eq(TEST_CONV_ID), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(memoryRepo.findByUserId(anyString(), anyList(), anyFloat(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(avatarRepo.findById(TEST_AVATAR_ID)).thenReturn(Optional.empty());

            InferenceEngine.InferenceResult mockResult = new InferenceEngine.InferenceResult(
                    "你好！欢迎来到索拉空间。", new EmotionState(EmotionCategory.NEUTRAL, 0.5f), Map.of()
            );
            when(inferenceEngine.infer(eq(TEST_CONV_ID), anyString(), anyList(), anyMap()))
                    .thenReturn(mockResult);

            DialogueTurn result = avatarDomainService.sendMessage(
                    TEST_USER_ID, TEST_SPACE_ID, TEST_CONV_ID,
                    "你好！", List.of(), Map.of()
            );

            assertThat(result).isNotNull();
            verify(conversationRepo).save(any(Conversation.class));
            verify(avatarRepo).save(any(Avatar.class));
        }

        @Test
        @DisplayName("should throw exception when conversation already ended")
        void shouldThrowWhenConversationEnded() {
            Conversation conv = new Conversation(TEST_CONV_ID, TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID);
            conv.end();
            when(conversationRepo.findById(TEST_CONV_ID)).thenReturn(Optional.of(conv));

            assertThatThrownBy(() ->
                    avatarDomainService.sendMessage(TEST_USER_ID, TEST_SPACE_ID, TEST_CONV_ID,
                            "你好！", List.of(), Map.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already ended");
        }

        @Test
        @DisplayName("should update avatar emotion state after inference")
        void shouldUpdateEmotionStateAfterInference() {
            Conversation conv = new Conversation(TEST_CONV_ID, TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID);
            when(conversationRepo.findById(TEST_CONV_ID)).thenReturn(Optional.of(conv));
            when(turnRepo.findByConversationId(eq(TEST_CONV_ID), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(memoryRepo.findByUserId(anyString(), anyList(), anyFloat(), anyInt()))
                    .thenReturn(Collections.emptyList());

            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");
            when(avatarRepo.findById(TEST_AVATAR_ID)).thenReturn(Optional.of(avatar));

            EmotionState detectedEmotion = new EmotionState(EmotionCategory.JOY, 0.9f);
            InferenceEngine.InferenceResult mockResult = new InferenceEngine.InferenceResult(
                    "太开心了！", detectedEmotion, Map.of()
            );
            when(inferenceEngine.infer(eq(TEST_CONV_ID), anyString(), anyList(), anyMap()))
                    .thenReturn(mockResult);

            avatarDomainService.sendMessage(TEST_USER_ID, TEST_SPACE_ID, TEST_CONV_ID,
                    "我今天很开心！", List.of(), Map.of());

            verify(avatarRepo).save(argThat(a -> a.getState().getEmotion().getPrimaryEmotion() == EmotionCategory.JOY));
        }

        @Test
        @DisplayName("should store memory for long messages (>20 chars)")
        void shouldStoreMemoryForLongMessages() {
            Conversation conv = new Conversation(TEST_CONV_ID, TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID);
            when(conversationRepo.findById(TEST_CONV_ID)).thenReturn(Optional.of(conv));
            when(turnRepo.findByConversationId(eq(TEST_CONV_ID), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(memoryRepo.findByUserId(anyString(), anyList(), anyFloat(), anyInt()))
                    .thenReturn(Collections.emptyList());

            InferenceEngine.InferenceResult mockResult = new InferenceEngine.InferenceResult(
                    "这是一个非常有深度的回答。", new EmotionState(), Map.of()
            );
            when(inferenceEngine.infer(eq(TEST_CONV_ID), anyString(), anyList(), anyMap()))
                    .thenReturn(mockResult);

            String longMessage = "这是一条超过20个字符的长消息用于测试记忆存储功能";
            avatarDomainService.sendMessage(TEST_USER_ID, TEST_SPACE_ID, TEST_CONV_ID,
                    longMessage, List.of(), Map.of());

            verify(memoryRepo).save(any(MemoryEntry.class));
        }

        @Test
        @DisplayName("should not store memory for short messages (<=20 chars)")
        void shouldNotStoreMemoryForShortMessages() {
            Conversation conv = new Conversation(TEST_CONV_ID, TEST_USER_ID, TEST_SPACE_ID, TEST_AVATAR_ID);
            when(conversationRepo.findById(TEST_CONV_ID)).thenReturn(Optional.of(conv));
            when(turnRepo.findByConversationId(eq(TEST_CONV_ID), anyInt(), anyInt()))
                    .thenReturn(Collections.emptyList());
            when(memoryRepo.findByUserId(anyString(), anyList(), anyFloat(), anyInt()))
                    .thenReturn(Collections.emptyList());

            InferenceEngine.InferenceResult mockResult = new InferenceEngine.InferenceResult(
                    "好", new EmotionState(), Map.of()
            );
            when(inferenceEngine.infer(eq(TEST_CONV_ID), anyString(), anyList(), anyMap()))
                    .thenReturn(mockResult);

            avatarDomainService.sendMessage(TEST_USER_ID, TEST_SPACE_ID, TEST_CONV_ID,
                    "你好", List.of(), Map.of());

            verify(memoryRepo, never()).save(any(MemoryEntry.class));
        }
    }

    // ============================================================
    // AVT-012: isContentSafe — 对话安全过滤
    // ============================================================
    @Nested
    @DisplayName("isContentSafe (AVT-012)")
    class ContentSafetyTests {

        @Test
        @DisplayName("should return true for normal content")
        void shouldReturnTrueForNormalContent() {
            assertThat(avatarDomainService.isContentSafe("你好，今天天气真好！")).isTrue();
        }

        @Test
        @DisplayName("should return true for null or blank content")
        void shouldReturnTrueForNullContent() {
            assertThat(avatarDomainService.isContentSafe(null)).isTrue();
            assertThat(avatarDomainService.isContentSafe("")).isTrue();
            assertThat(avatarDomainService.isContentSafe("   ")).isTrue();
        }

        @Test
        @DisplayName("should return false for blocked keywords")
        void shouldReturnFalseForBlockedKeywords() {
            assertThat(avatarDomainService.isContentSafe("这是关于毒品的内容")).isFalse();
            assertThat(avatarDomainService.isContentSafe("涉及赌博交易")).isFalse();
            assertThat(avatarDomainService.isContentSafe("关于自杀的话题")).isFalse();
        }

        @Test
        @DisplayName("should return false for blocked keywords (case insensitive)")
        void shouldReturnFalseForBlockedKeywordsCaseInsensitive() {
            // The implementation uses toLowerCase, so it's case insensitive
            assertThat(avatarDomainService.isContentSafe("关于DU品")).isFalse();
        }
    }

    // ============================================================
    // Query methods
    // ============================================================
    @Nested
    @DisplayName("query methods")
    class QueryMethodTests {

        @Test
        @DisplayName("getHistory should delegate to turnRepo")
        void getHistoryShouldDelegate() {
            List<DialogueTurn> expected = List.of(
                    new DialogueTurn("t1", TEST_CONV_ID, TurnRole.USER, "你好")
            );
            when(turnRepo.findByConversationId(TEST_CONV_ID, 0, 20)).thenReturn(expected);

            List<DialogueTurn> result = avatarDomainService.getHistory(TEST_CONV_ID, 0, 20);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("getAvatarState should return state when avatar exists")
        void getAvatarStateShouldReturnState() {
            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");
            when(avatarRepo.findById(TEST_AVATAR_ID)).thenReturn(Optional.of(avatar));

            Optional<AvatarState> result = avatarDomainService.getAvatarState(TEST_AVATAR_ID);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("getAvatarState should return empty when avatar not found")
        void getAvatarStateShouldReturnEmpty() {
            when(avatarRepo.findById("nonexistent")).thenReturn(Optional.empty());

            Optional<AvatarState> result = avatarDomainService.getAvatarState("nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("updateEmotionState should update and return emotion")
        void updateEmotionStateShouldUpdate() {
            Avatar avatar = new Avatar(TEST_AVATAR_ID, "Solra AI");
            when(avatarRepo.findById(TEST_AVATAR_ID)).thenReturn(Optional.of(avatar));

            EmotionState newEmotion = new EmotionState(EmotionCategory.JOY, 0.9f);
            Optional<EmotionState> result = avatarDomainService.updateEmotionState(TEST_AVATAR_ID, newEmotion);

            assertThat(result).isPresent();
            assertThat(result.get().getPrimaryEmotion()).isEqualTo(EmotionCategory.JOY);
            verify(avatarRepo).save(any(Avatar.class));
        }

        @Test
        @DisplayName("queryMemory should default to all types when null")
        void queryMemoryShouldDefaultAllTypes() {
            when(memoryRepo.findByUserId(eq(TEST_USER_ID), anyList(), eq(0.5f), eq(10)))
                    .thenReturn(Collections.emptyList());

            List<MemoryEntry> result = avatarDomainService.queryMemory(TEST_USER_ID, null, 0.5f, 10);

            assertThat(result).isEmpty();
            verify(memoryRepo).findByUserId(eq(TEST_USER_ID), anyList(), eq(0.5f), eq(10));
        }
    }
}
