package com.solra.soc.domain.service;

import com.solra.soc.domain.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ChatService — SOC-002 空间内聊天领域服务。
 *
 * 管理会话内的文字/语音消息发送、接收和历史查询。
 * 支持广播消息（所有参与者可见）和私聊消息。
 */
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** 会话消息存储：sessionId -> List<ChatMessage> */
    private final Map<String, List<ChatMessage>> sessionMessages = new ConcurrentHashMap<>();

    /** 最大消息历史保留数 */
    private static final int MAX_MESSAGES_PER_SESSION = 500;

    public ChatService() {}

    /**
     * SOC-002: Send a text message.
     */
    public ChatMessage sendTextMessage(String sessionId, String fromUserId,
                                        String toUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        ChatMessage msg = ChatMessage.text(sessionId, fromUserId, toUserId, content);
        storeMessage(sessionId, msg);
        log.info("SOC-002 text message sent: session={} from={} to={}",
                sessionId, fromUserId, toUserId != null ? toUserId : "broadcast");
        return msg;
    }

    /**
     * SOC-002: Send a voice message.
     */
    public ChatMessage sendVoiceMessage(String sessionId, String fromUserId,
                                         String toUserId, String voiceUrl, int durationMs) {
        if (voiceUrl == null || voiceUrl.isBlank()) {
            throw new IllegalArgumentException("Voice URL cannot be empty");
        }
        ChatMessage msg = ChatMessage.voice(sessionId, fromUserId, toUserId, voiceUrl, durationMs);
        storeMessage(sessionId, msg);
        log.info("SOC-002 voice message sent: session={} from={} duration={}ms",
                sessionId, fromUserId, durationMs);
        return msg;
    }

    /**
     * SOC-002: Send a system message.
     */
    public ChatMessage sendSystemMessage(String sessionId, String content) {
        ChatMessage msg = ChatMessage.system(sessionId, content);
        storeMessage(sessionId, msg);
        log.info("SOC-002 system message: session={} content={}", sessionId, content);
        return msg;
    }

    /**
     * Get recent messages for a session.
     */
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> messages = sessionMessages.getOrDefault(sessionId, List.of());
        int count = Math.min(limit, messages.size());
        int start = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(start, messages.size()));
    }

    /**
     * Get messages since a given message ID.
     */
    public List<ChatMessage> getMessagesSince(String sessionId, String sinceMessageId) {
        List<ChatMessage> messages = sessionMessages.getOrDefault(sessionId, List.of());
        List<ChatMessage> result = new ArrayList<>();
        boolean found = (sinceMessageId == null);
        for (ChatMessage msg : messages) {
            if (found) {
                result.add(msg);
            }
            if (msg.getMessageId().equals(sinceMessageId)) {
                found = true;
            }
        }
        return result;
    }

    /**
     * Mark a message as read.
     */
    public void markAsRead(String sessionId, String messageId) {
        List<ChatMessage> messages = sessionMessages.get(sessionId);
        if (messages != null) {
            messages.stream()
                    .filter(m -> m.getMessageId().equals(messageId))
                    .findFirst()
                    .ifPresent(ChatMessage::markRead);
        }
    }

    /**
     * Get chat statistics for a session.
     */
    public ChatStats getStats(String sessionId) {
        List<ChatMessage> messages = sessionMessages.getOrDefault(sessionId, List.of());
        long textCount = messages.stream().filter(m -> m.getType() == ChatMessage.ChatMessageType.TEXT).count();
        long voiceCount = messages.stream().filter(m -> m.getType() == ChatMessage.ChatMessageType.VOICE).count();
        long systemCount = messages.stream().filter(m -> m.getType() == ChatMessage.ChatMessageType.SYSTEM).count();
        return new ChatStats(messages.size(), textCount, voiceCount, systemCount);
    }

    /**
     * Clean up messages for an ended session.
     */
    public void cleanup(String sessionId) {
        sessionMessages.remove(sessionId);
        log.info("SOC-002 chat messages cleaned for session: {}", sessionId);
    }

    private void storeMessage(String sessionId, ChatMessage msg) {
        List<ChatMessage> messages = sessionMessages.computeIfAbsent(
                sessionId, k -> new CopyOnWriteArrayList<>());
        messages.add(msg);

        // Trim old messages if exceeding limit
        while (messages.size() > MAX_MESSAGES_PER_SESSION) {
            messages.remove(0);
        }
    }

    // -- Inner types --
    public record ChatStats(int totalMessages, long textMessages, long voiceMessages, long systemMessages) {}
}
