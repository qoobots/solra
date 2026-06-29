package com.solra.avt.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "avt_memories")
public class MemoryEntity {

    @Id
    @Column(name = "memory_id")
    private String memoryId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(length = 2048)
    private String content;

    private float importance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_accessed")
    private Instant lastAccessed;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public String getMemoryId() { return memoryId; }
    public void setMemoryId(String memoryId) { this.memoryId = memoryId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float getImportance() { return importance; }
    public void setImportance(float importance) { this.importance = importance; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
