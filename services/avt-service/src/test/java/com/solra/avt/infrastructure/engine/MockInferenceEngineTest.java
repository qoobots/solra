package com.solra.avt.infrastructure.engine;

import com.solra.avt.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MockInferenceEngine covering AVT-010/AVT-011.
 */
@DisplayName("MockInferenceEngine")
class MockInferenceEngineTest {

    private MockInferenceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new MockInferenceEngine();
    }

    // ============================================================
    // infer — 同步推理 (AVT-011)
    // ============================================================
    @Nested
    @DisplayName("infer (sync)")
    class InferTests {

        @Test
        @DisplayName("should return greeting for new conversation")
        void shouldReturnGreetingForNewConversation() {
            var result = engine.infer("conv-1", "你好", List.of(), Map.of());

            assertThat(result).isNotNull();
            assertThat(result.responseText()).isNotEmpty();
            assertThat(result.detectedEmotion()).isNotNull();
            assertThat(result.metadata()).containsEntry("engine", "mock");
        }

        @Test
        @DisplayName("should return response for ongoing conversation")
        void shouldReturnResponseForOngoingConversation() {
            List<DialogueTurn> history = List.of(
                    new DialogueTurn("t1", "conv-1", TurnRole.USER, "你好"),
                    new DialogueTurn("t2", "conv-1", TurnRole.AVATAR, "你好！欢迎来到索拉空间。")
            );

            var result = engine.infer("conv-1", "最近怎么样？", history, Map.of());

            assertThat(result).isNotNull();
            assertThat(result.responseText()).isNotEmpty();
        }

        @Test
        @DisplayName("should return farewell for goodbye message")
        void shouldReturnFarewellForGoodbye() {
            List<DialogueTurn> history = List.of(
                    new DialogueTurn("t1", "conv-1", TurnRole.USER, "你好"),
                    new DialogueTurn("t2", "conv-1", TurnRole.AVATAR, "你好！")
            );

            var result = engine.infer("conv-1", "再见", history, Map.of());

            assertThat(result).isNotNull();
            assertThat(result.responseText()).containsAnyOf("回头见！", "下次再聊！", "期待与你再次相遇。");
        }

        @Test
        @DisplayName("should return farewell for bye message")
        void shouldReturnFarewellForBye() {
            List<DialogueTurn> history = List.of(
                    new DialogueTurn("t1", "conv-1", TurnRole.USER, "你好"),
                    new DialogueTurn("t2", "conv-1", TurnRole.AVATAR, "你好！")
            );

            var result = engine.infer("conv-1", "bye bye", history, Map.of());

            assertThat(result).isNotNull();
            assertThat(result.responseText()).containsAnyOf("回头见！", "下次再聊！", "期待与你再次相遇。");
        }
    }

    // ============================================================
    // analyzeEmotion — 情感分析
    // ============================================================
    @Nested
    @DisplayName("analyzeEmotion")
    class AnalyzeEmotionTests {

        @Test
        @DisplayName("should detect JOY for happy Chinese text")
        void shouldDetectJoyForHappyChinese() {
            EmotionState result = engine.analyzeEmotion("我今天很开心！哈哈");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.JOY);
            assertThat(result.getIntensity()).isGreaterThanOrEqualTo(0.7f);
        }

        @Test
        @DisplayName("should detect JOY for happy English text")
        void shouldDetectJoyForHappyEnglish() {
            EmotionState result = engine.analyzeEmotion("I am so happy today!");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.JOY);
        }

        @Test
        @DisplayName("should detect SADNESS for sad text")
        void shouldDetectSadness() {
            EmotionState result = engine.analyzeEmotion("我今天很难过");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.SADNESS);
            assertThat(result.getIntensity()).isGreaterThanOrEqualTo(0.7f);
        }

        @Test
        @DisplayName("should detect ANGER for angry text")
        void shouldDetectAnger() {
            EmotionState result = engine.analyzeEmotion("我很生气！");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.ANGER);
            assertThat(result.getIntensity()).isGreaterThanOrEqualTo(0.7f);
        }

        @Test
        @DisplayName("should detect FEAR for scared text")
        void shouldDetectFear() {
            EmotionState result = engine.analyzeEmotion("我很害怕");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.FEAR);
        }

        @Test
        @DisplayName("should detect SURPRISE for surprised text")
        void shouldDetectSurprise() {
            EmotionState result = engine.analyzeEmotion("真的吗？哇！");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.SURPRISE);
            assertThat(result.getIntensity()).isGreaterThanOrEqualTo(0.7f);
        }

        @Test
        @DisplayName("should return NEUTRAL for normal text")
        void shouldReturnNeutralForNormalText() {
            EmotionState result = engine.analyzeEmotion("今天天气不错");

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.NEUTRAL);
        }

        @Test
        @DisplayName("should return NEUTRAL for null text")
        void shouldReturnNeutralForNull() {
            EmotionState result = engine.analyzeEmotion(null);

            assertThat(result.getPrimaryEmotion()).isEqualTo(EmotionCategory.NEUTRAL);
            assertThat(result.getIntensity()).isEqualTo(0f);
        }
    }

    // ============================================================
    // summarizeMemory
    // ============================================================
    @Nested
    @DisplayName("summarizeMemory")
    class SummarizeMemoryTests {

        @Test
        @DisplayName("should return empty for null turns")
        void shouldReturnEmptyForNull() {
            String result = engine.summarizeMemory(null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return empty for empty list")
        void shouldReturnEmptyForEmptyList() {
            String result = engine.summarizeMemory(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should generate summary from turns")
        void shouldGenerateSummary() {
            List<DialogueTurn> turns = List.of(
                    new DialogueTurn("t1", "conv-1", TurnRole.USER, "你好"),
                    new DialogueTurn("t2", "conv-1", TurnRole.AVATAR, "你好！欢迎来到索拉空间。")
            );

            String result = engine.summarizeMemory(turns);

            assertThat(result).startsWith("对话摘要:");
            assertThat(result).contains("[USER]");
            assertThat(result).contains("[AVATAR]");
        }
    }

    // ============================================================
    // Engine status
    // ============================================================
    @Nested
    @DisplayName("engine status")
    class EngineStatusTests {

        @Test
        @DisplayName("should always be available")
        void shouldAlwaysBeAvailable() {
            assertThat(engine.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should return engine name")
        void shouldReturnEngineName() {
            assertThat(engine.engineName()).isEqualTo("mock-v1");
        }
    }
}
