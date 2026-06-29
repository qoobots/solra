package com.solra.avt.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "avt_dialogue_turns")
public class DialogueTurnEntity {

    @Id
    @Column(name = "turn_id")
    private String turnId;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(length = 4096)
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    @Embedded
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "avt_turn_chunks", joinColumns = @JoinColumn(name = "turn_id"))
    private List<TokenChunkEmbeddable> chunks;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "avt_turn_metadata", joinColumns = @JoinColumn(name = "turn_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private java.util.Map<String, String> metadata;

    @Embeddable
    public static class TokenChunkEmbeddable {
        private int sequence;
        private String token;
        private boolean isFinal;

        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    }

    public String getTurnId() { return turnId; }
    public void setTurnId(String turnId) { this.turnId = turnId; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public List<TokenChunkEmbeddable> getChunks() { return chunks; }
    public void setChunks(List<TokenChunkEmbeddable> chunks) { this.chunks = chunks; }
    public java.util.Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(java.util.Map<String, String> metadata) { this.metadata = metadata; }
}
