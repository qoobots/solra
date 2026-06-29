package com.solra.soc.application.dto;

import com.solra.soc.domain.model.ChatMessage;

import java.time.Instant;
import java.util.List;

/**
 * ChatMessageDTO — SOC-002 聊天消息数据传输对象。
 */
public record ChatMessageDTO(
        String messageId,
        String sessionId,
        String fromUserId,
        String toUserId,
        String type,
        String content,
        String voiceUrl,
        int voiceDurationMs,
        Instant sentAt,
        boolean read
) {
    public static ChatMessageDTO from(ChatMessage msg) {
        return new ChatMessageDTO(
                msg.getMessageId(),
                msg.getSessionId(),
                msg.getFromUserId(),
                msg.getToUserId(),
                msg.getType().name(),
                msg.getContent(),
                msg.getVoiceUrl(),
                msg.getVoiceDurationMs(),
                msg.getSentAt(),
                msg.isRead()
        );
    }
}

/**
 * ChatListDTO — 聊天消息列表包装。
 */
public record ChatListDTO(List<ChatMessageDTO> messages, int total) {
    public static ChatListDTO of(List<ChatMessage> messages) {
        return new ChatListDTO(
                messages.stream().map(ChatMessageDTO::from).toList(),
                messages.size()
        );
    }
}

/**
 * ChatStatsDTO — 聊天统计。
 */
public record ChatStatsDTO(long totalMessages, long textMessages, long voiceMessages, long systemMessages) {}
