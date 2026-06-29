package com.solra.avt.domain.service;

import com.solra.avt.domain.model.DialogueTurn;
import com.solra.avt.domain.model.EmotionState;
import com.solra.avt.domain.model.MessageAttachment;
import com.solra.avt.domain.model.TokenChunk;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

/**
 * InferenceEngine — 推理引擎抽象（AVT-010/AVT-011）。
 * 具体实现由 core/ C++ 端侧/云端推理引擎提供。
 * avt-service 通过此接口编排对话，不绑定具体 AI 后端。
 */
public interface InferenceEngine {

    /** 同步推理：发送用户输入，返回完整响应（AVT-011 云端LLM） */
    InferenceResult infer(String conversationId, String userMessage,
                          List<DialogueTurn> history, Map<String, String> context);

    /** 流式推理：逐token推送（AVT-010 端侧SLM / AVT-011 云端LLM流式） */
    Flow.Publisher<TokenChunk> inferStream(String conversationId, String userMessage,
                                           List<DialogueTurn> history, Map<String, String> context);

    /** 情感分析 */
    EmotionState analyzeEmotion(String text);

    /** 生成记忆摘要 */
    String summarizeMemory(List<DialogueTurn> recentTurns);

    /** 判断是否可用 */
    boolean isAvailable();

    /** 获取引擎名称 */
    String engineName();

    /** 推理结果 */
    record InferenceResult(String responseText, EmotionState detectedEmotion, Map<String, String> metadata) {}
}
