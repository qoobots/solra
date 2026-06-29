package com.solra.avt.domain.event;

import com.solra.avt.domain.model.EmotionState;

import java.time.Instant;

/**
 * AVT 领域事件定义。
 */
public final class AvtDomainEvents {

    private AvtDomainEvents() {}

    public record MessageSent(String conversationId, String turnId, String userId, String content, Instant at) {}
    public record ResponseGenerated(String conversationId, String turnId, String responsePreview, Instant at) {}
    public record MemoryStored(String memoryId, String userId, String memoryType, float importance, Instant at) {}
    public record EmotionShifted(String avatarId, EmotionState from, EmotionState to, Instant at) {}
    public record ConversationEnded(String conversationId, String userId, Instant at) {}
}
