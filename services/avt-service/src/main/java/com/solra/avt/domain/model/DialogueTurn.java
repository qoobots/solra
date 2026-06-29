package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DialogueTurn 实体 — 对话中的一个轮次（用户发言 / 虚拟人回复 / 系统消息）。
 */
public class DialogueTurn {

    private String turnId;
    private String conversationId;
    private TurnRole role;
    private String content;
    private List<TokenChunk> chunks;
    private Instant timestamp;
    private Map<String, String> metadata;

    public DialogueTurn() {}

    public DialogueTurn(String turnId, String conversationId, TurnRole role, String content) {
        this.turnId = turnId;
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    public boolean isFinal() {
        return chunks != null && !chunks.isEmpty() && chunks.get(chunks.size() - 1).isFinal();
    }

    // ---- getters / setters ----
    public String getTurnId() { return turnId; }
    public void setTurnId(String turnId) { this.turnId = turnId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public TurnRole getRole() { return role; }
    public void setRole(TurnRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<TokenChunk> getChunks() { return chunks; }
    public void setChunks(List<TokenChunk> chunks) { this.chunks = chunks; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
}
