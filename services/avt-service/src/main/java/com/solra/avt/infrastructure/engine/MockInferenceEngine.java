package com.solra.avt.infrastructure.engine;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.service.InferenceEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * MockInferenceEngine — 模拟推理引擎。
 * 在 core/ C++ LLM/SLM 引擎就绪前提供基础对话能力。
 * 实现 AVT-010（端侧SLM）和 AVT-011（云端LLM）的 Mock。
 */
@Component
public class MockInferenceEngine implements InferenceEngine {

    private static final Logger log = LoggerFactory.getLogger(MockInferenceEngine.class);

    private static final String[] GREETINGS = {
        "你好！欢迎来到索拉空间。", "嗨！很高兴见到你。", "今天有什么想聊的吗？"
    };
    private static final String[] RESPONSES = {
        "这真的很有意思呢！", "让我想想...这确实值得深思。", "我理解你的感受。",
        "哇，这个想法很独特！", "谢谢你告诉我这些。", "我从你身上学到了新东西。"
    };
    private static final String[] FAREWELLS = {
        "回头见！", "下次再聊！", "期待与你再次相遇。"
    };

    private int counter = 0;

    @Override
    public InferenceResult infer(String conversationId, String userMessage,
                                  List<DialogueTurn> history, Map<String, String> context) {
        log.info("MockInferenceEngine.infer: conv={} msg_len={} history_size={}", conversationId, userMessage.length(), history.size());

        // 情感分析
        EmotionState emotion = analyzeEmotion(userMessage);

        // 生成响应
        String response = generateResponse(userMessage, history);

        return new InferenceResult(response, emotion, Map.of("engine", "mock", "model", "solra-mock-v1"));
    }

    @Override
    public Flow.Publisher<TokenChunk> inferStream(String conversationId, String userMessage,
                                                   List<DialogueTurn> history, Map<String, String> context) {
        String fullResponse = generateResponse(userMessage, history);
        SubmissionPublisher<TokenChunk> publisher = new SubmissionPublisher<>();

        Thread.ofVirtual().start(() -> {
            try {
                String[] words = fullResponse.split("(?<=\\S)(?=\\s)|\\s+");
                for (int i = 0; i < words.length; i++) {
                    boolean isFinal = (i == words.length - 1);
                    publisher.submit(new TokenChunk(i, words[i] + (isFinal ? "" : " "), isFinal));
                    Thread.sleep(50); // 模拟流式延迟
                }
                publisher.close();
            } catch (Exception e) {
                publisher.closeExceptionally(e);
            }
        });

        return publisher;
    }

    @Override
    public EmotionState analyzeEmotion(String text) {
        if (text == null || text.isBlank()) return new EmotionState(EmotionCategory.NEUTRAL, 0f);

        String lower = text.toLowerCase();
        if (containsAny(lower, "哈哈", "开心", "喜欢", "太棒", "love", "happy", "great")) {
            return new EmotionState(EmotionCategory.JOY, 0.8f);
        } else if (containsAny(lower, "难过", "伤心", "哭", "sad", "cry")) {
            return new EmotionState(EmotionCategory.SADNESS, 0.7f);
        } else if (containsAny(lower, "生气", "愤怒", "讨厌", "angry", "hate")) {
            return new EmotionState(EmotionCategory.ANGER, 0.75f);
        } else if (containsAny(lower, "害怕", "恐惧", "fear", "scared")) {
            return new EmotionState(EmotionCategory.FEAR, 0.7f);
        } else if (containsAny(lower, "惊讶", "哇", "真的吗", "surprise", "wow")) {
            return new EmotionState(EmotionCategory.SURPRISE, 0.8f);
        }
        return new EmotionState(EmotionCategory.NEUTRAL, 0.3f);
    }

    @Override
    public String summarizeMemory(List<DialogueTurn> recentTurns) {
        if (recentTurns == null || recentTurns.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("对话摘要: ");
        for (DialogueTurn t : recentTurns) {
            sb.append("[").append(t.getRole()).append("] ").append(truncate(t.getContent(), 50)).append(" ");
        }
        return sb.toString();
    }

    @Override
    public boolean isAvailable() { return true; }

    @Override
    public String engineName() { return "mock-v1"; }

    private String generateResponse(String userMessage, List<DialogueTurn> history) {
        String lower = userMessage != null ? userMessage.toLowerCase() : "";

        if (history == null || history.size() <= 1) {
            return GREETINGS[counter++ % GREETINGS.length];
        }
        if (containsAny(lower, "再见", "拜拜", "bye", "goodbye")) {
            return FAREWELLS[counter++ % FAREWELLS.length];
        }
        return RESPONSES[counter++ % RESPONSES.length];
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
