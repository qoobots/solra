package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Conversation 聚合根 — 虚拟人与用户的对话会话。
 * 一条对话拥有多个 DialogueTurn 和一条生命周期。
 */
public class Conversation {

    private String conversationId;
    private String userId;
    private String spaceId;
    private String avatarId;
    private ConversationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, String> metadata;

    // ---- constructors ----
    public Conversation() {}

    public Conversation(String conversationId, String userId, String spaceId, String avatarId) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.spaceId = spaceId;
        this.avatarId = avatarId;
        this.status = ConversationStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ---- business methods ----
    public void pause() {
        if (this.status == ConversationStatus.ACTIVE) {
            this.status = ConversationStatus.PAUSED;
            this.updatedAt = Instant.now();
        }
    }

    public void resume() {
        if (this.status == ConversationStatus.PAUSED) {
            this.status = ConversationStatus.ACTIVE;
            this.updatedAt = Instant.now();
        }
    }

    public void end() {
        this.status = ConversationStatus.ENDED;
        this.updatedAt = Instant.now();
    }

    public void touch() {
        this.updatedAt = Instant.now();
    }

    // ---- getters / setters ----
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
