package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * ChatMessage — SOC-002 空间内聊天消息实体。
 *
 * 支持文字消息和语音消息信令。
 */
public class ChatMessage {

    private String messageId;
    private String sessionId;
    private String fromUserId;
    private String toUserId;        // null = broadcast to all
    private ChatMessageType type;
    private String content;         // text content or voice transcription
    private String voiceUrl;        // URL for voice message audio
    private int voiceDurationMs;    // voice message duration
    private Instant sentAt;
    private boolean read;

    private ChatMessage() {}

    public static ChatMessage text(String sessionId, String fromUserId,
                                    String toUserId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.messageId = UUID.randomUUID().toString();
        msg.sessionId = sessionId;
        msg.fromUserId = fromUserId;
        msg.toUserId = toUserId;
        msg.type = ChatMessageType.TEXT;
        msg.content = content;
        msg.sentAt = Instant.now();
        return msg;
    }

    public static ChatMessage voice(String sessionId, String fromUserId,
                                     String toUserId, String voiceUrl, int durationMs) {
        ChatMessage msg = new ChatMessage();
        msg.messageId = UUID.randomUUID().toString();
        msg.sessionId = sessionId;
        msg.fromUserId = fromUserId;
        msg.toUserId = toUserId;
        msg.type = ChatMessageType.VOICE;
        msg.voiceUrl = voiceUrl;
        msg.voiceDurationMs = durationMs;
        msg.sentAt = Instant.now();
        return msg;
    }

    public static ChatMessage system(String sessionId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.messageId = UUID.randomUUID().toString();
        msg.sessionId = sessionId;
        msg.fromUserId = "SYSTEM";
        msg.type = ChatMessageType.SYSTEM;
        msg.content = content;
        msg.sentAt = Instant.now();
        return msg;
    }

    public void markRead() { this.read = true; }

    public boolean isBroadcast() { return toUserId == null; }

    // -- Getters/Setters --
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public ChatMessageType getType() { return type; }
    public void setType(ChatMessageType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVoiceUrl() { return voiceUrl; }
    public void setVoiceUrl(String voiceUrl) { this.voiceUrl = voiceUrl; }
    public int getVoiceDurationMs() { return voiceDurationMs; }
    public void setVoiceDurationMs(int ms) { this.voiceDurationMs = ms; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public enum ChatMessageType { TEXT, VOICE, SYSTEM, INTERACTION }
}
