package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * MemoryEntry 实体 — 虚拟人对用户的记忆条目（情景/语义/事实/偏好）。
 */
public class MemoryEntry {

    private String memoryId;
    private String userId;
    private String conversationId;
    private MemoryType type;
    private String content;
    private float importance;    // 0..1
    private Instant createdAt;
    private Instant lastAccessed;
    private Instant expiresAt;
    private Map<String, String> metadata;

    public MemoryEntry() {}

    public MemoryEntry(String memoryId, String userId, MemoryType type, String content, float importance) {
        this.memoryId = memoryId;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.importance = clamp(importance, 0f, 1f);
        this.createdAt = Instant.now();
        this.lastAccessed = this.createdAt;
    }

    public void access() {
        this.lastAccessed = Instant.now();
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    // ---- getters / setters ----
    public String getMemoryId() { return memoryId; }
    public void setMemoryId(String memoryId) { this.memoryId = memoryId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public MemoryType getType() { return type; }
    public void setType(MemoryType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public float getImportance() { return importance; }
    public void setImportance(float importance) { this.importance = clamp(importance, 0f, 1f); }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getLastAccessed() { return lastAccessed; }
    public void setLastAccessed(Instant lastAccessed) { this.lastAccessed = lastAccessed; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
