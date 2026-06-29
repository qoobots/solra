package com.solra.avt.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * LongTermMemory — AVT-003 虚拟人长期记忆聚合根。
 *
 * 跨会话持久化记忆，包含≥3个记忆点，持久化≥30天。
 * 支持记忆衰减、记忆检索和记忆融合。
 */
public class LongTermMemory {

    private String memoryId;
    private String userId;
    private String avatarId;
    private List<MemorySnapshot> snapshots = new ArrayList<>();
    private MemorySummary summary;          // 用户画像摘要
    private Instant createdAt;
    private Instant lastConsolidated;       // 最后记忆整合时间
    private int totalMemories;              // 累计记忆条数
    private Map<String, String> metadata = new HashMap<>();

    private LongTermMemory() {}

    public static LongTermMemory create(String userId, String avatarId) {
        LongTermMemory ltm = new LongTermMemory();
        ltm.memoryId = UUID.randomUUID().toString();
        ltm.userId = userId;
        ltm.avatarId = avatarId;
        ltm.summary = new MemorySummary();
        ltm.createdAt = Instant.now();
        ltm.lastConsolidated = Instant.now();
        ltm.totalMemories = 0;
        return ltm;
    }

    /**
     * Add a new memory snapshot (from a conversation).
     */
    public void addSnapshot(String conversationId, String content,
                             MemorySnapshotType type, float importance, String emotionContext) {
        // Remove old snapshots beyond retention period (30 days)
        Instant cutoff = Instant.now().minusSeconds(30 * 24 * 3600);
        snapshots.removeIf(s -> s.getCapturedAt().isBefore(cutoff));

        MemorySnapshot snapshot = new MemorySnapshot(
                UUID.randomUUID().toString(), conversationId, content,
                type, importance, emotionContext);
        snapshots.add(snapshot);
        totalMemories++;

        // Update summary
        summary.updateFromSnapshot(snapshot);

        // Limit to max 500 snapshots
        if (snapshots.size() > 500) {
            snapshots.sort(Comparator.comparingDouble(MemorySnapshot::getImportance).reversed());
            snapshots = new ArrayList<>(snapshots.subList(0, 500));
        }
    }

    /**
     * Retrieve relevant memories for context enrichment.
     */
    public List<MemorySnapshot> retrieveRelevant(String query, int maxResults) {
        return snapshots.stream()
                .filter(s -> !s.isExpired())
                .sorted(Comparator.comparingDouble(
                        s -> s.getImportance() * 0.7 + s.relevanceScore(query) * 0.3))
                .limit(maxResults)
                .toList();
    }

    /**
     * Consolidate old memories (merge similar snapshots).
     */
    public void consolidate() {
        if (snapshots.size() < 10) return;

        // Group by type and merge similar entries
        Map<MemorySnapshotType, List<MemorySnapshot>> byType = new HashMap<>();
        for (MemorySnapshot s : snapshots) {
            byType.computeIfAbsent(s.getType(), k -> new ArrayList<>()).add(s);
        }

        List<MemorySnapshot> consolidated = new ArrayList<>();
        for (var entry : byType.entrySet()) {
            List<MemorySnapshot> group = entry.getValue();
            if (group.size() >= 5) {
                // Merge into a summary snapshot
                String mergedContent = "Summary of " + group.size() + " " +
                        entry.getKey().name().toLowerCase() + " memories: " +
                        summarizeContent(group);
                MemorySnapshot merged = new MemorySnapshot(
                        UUID.randomUUID().toString(), "consolidation",
                        mergedContent, MemorySnapshotType.SUMMARY,
                        averageImportance(group), "consolidated");
                consolidated.add(merged);
            } else {
                consolidated.addAll(group);
            }
        }

        this.snapshots = consolidated;
        this.lastConsolidated = Instant.now();
    }

    /**
     * Check if memory needs consolidation (>100 snapshots since last consolidation).
     */
    public boolean needsConsolidation() {
        return snapshots.size() >= 100;
    }

    /**
     * Get summary of what the avatar "knows" about this user.
     */
    public MemorySummary getSummary() { return summary; }

    // -- Getters --
    public String getMemoryId() { return memoryId; }
    public String getUserId() { return userId; }
    public String getAvatarId() { return avatarId; }
    public List<MemorySnapshot> getSnapshots() { return Collections.unmodifiableList(snapshots); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastConsolidated() { return lastConsolidated; }
    public int getTotalMemories() { return totalMemories; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }

    private String summarizeContent(List<MemorySnapshot> group) {
        return group.stream()
                .limit(3)
                .map(MemorySnapshot::getContent)
                .reduce((a, b) -> a + "; " + b)
                .orElse("no memories");
    }

    private float averageImportance(List<MemorySnapshot> group) {
        return (float) group.stream()
                .mapToDouble(MemorySnapshot::getImportance)
                .average()
                .orElse(0.3f);
    }

    /**
     * A single memory snapshot captured from a conversation.
     */
    public static class MemorySnapshot {
        private String snapshotId;
        private String conversationId;
        private String content;
        private MemorySnapshotType type;
        private float importance;           // 0..1
        private String emotionContext;      // 当时的情感状态
        private Instant capturedAt;
        private Instant expiresAt;          // 30 days retention

        public MemorySnapshot() {}

        public MemorySnapshot(String snapshotId, String conversationId, String content,
                               MemorySnapshotType type, float importance, String emotionContext) {
            this.snapshotId = snapshotId;
            this.conversationId = conversationId;
            this.content = content;
            this.type = type;
            this.importance = Math.max(0f, Math.min(1f, importance));
            this.emotionContext = emotionContext;
            this.capturedAt = Instant.now();
            this.expiresAt = capturedAt.plusSeconds(30 * 24 * 3600);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public double relevanceScore(String query) {
            if (query == null || query.isBlank()) return 0.0;
            String lowerContent = content != null ? content.toLowerCase() : "";
            String lowerQuery = query.toLowerCase();
            // Simple word overlap score
            String[] queryWords = lowerQuery.split("\\s+");
            int matches = 0;
            for (String word : queryWords) {
                if (word.length() > 1 && lowerContent.contains(word)) matches++;
            }
            return queryWords.length > 0 ? (double) matches / queryWords.length : 0.0;
        }

        public String getSnapshotId() { return snapshotId; }
        public void setSnapshotId(String snapshotId) { this.snapshotId = snapshotId; }
        public String getConversationId() { return conversationId; }
        public void setConversationId(String conversationId) { this.conversationId = conversationId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public MemorySnapshotType getType() { return type; }
        public void setType(MemorySnapshotType type) { this.type = type; }
        public float getImportance() { return importance; }
        public void setImportance(float importance) { this.importance = Math.max(0f, Math.min(1f, importance)); }
        public String getEmotionContext() { return emotionContext; }
        public void setEmotionContext(String emotionContext) { this.emotionContext = emotionContext; }
        public Instant getCapturedAt() { return capturedAt; }
        public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    }

    public enum MemorySnapshotType {
        EPISODIC,       // 具体事件
        SEMANTIC,       // 一般知识
        FACT,           // 用户告知的事实
        PREFERENCE,     // 用户偏好
        EMOTIONAL,      // 情感记忆
        SUMMARY         // 整合摘要
    }

    /**
     * User profile summary derived from long-term memories.
     */
    public static class MemorySummary {
        private List<String> knownFacts = new ArrayList<>();      // 已知事实
        private List<String> preferences = new ArrayList<>();     // 用户偏好
        private List<String> topics = new ArrayList<>();           // 常聊话题
        private String relationshipStage = "new";                 // new/acquaintance/friend/close
        private int totalInteractions;

        public void updateFromSnapshot(MemorySnapshot snapshot) {
            totalInteractions++;
            switch (snapshot.getType()) {
                case FACT -> knownFacts.add(snapshot.getContent());
                case PREFERENCE -> preferences.add(snapshot.getContent());
                case EPISODIC, EMOTIONAL -> topics.add(extractTopic(snapshot.getContent()));
            }

            // Update relationship stage
            if (totalInteractions >= 50) relationshipStage = "close";
            else if (totalInteractions >= 20) relationshipStage = "friend";
            else if (totalInteractions >= 5) relationshipStage = "acquaintance";
        }

        private String extractTopic(String content) {
            if (content == null) return "general";
            if (content.length() > 50) content = content.substring(0, 50);
            return content;
        }

        public List<String> getKnownFacts() { return Collections.unmodifiableList(knownFacts); }
        public List<String> getPreferences() { return Collections.unmodifiableList(preferences); }
        public List<String> getTopics() { return Collections.unmodifiableList(topics); }
        public String getRelationshipStage() { return relationshipStage; }
        public int getTotalInteractions() { return totalInteractions; }
    }
}
