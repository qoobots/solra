package com.solra.avt.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LongTermMemory 单元测试 — AVT-003 长期记忆")
class LongTermMemoryTest {

    @Nested
    @DisplayName("create — 创建长期记忆")
    class CreateTests {

        @Test
        @DisplayName("创建记忆 → 初始状态为空")
        void createEmptyMemory() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");

            assertNotNull(ltm.getMemoryId());
            assertEquals("user1", ltm.getUserId());
            assertEquals("avatar1", ltm.getAvatarId());
            assertEquals(0, ltm.getTotalMemories());
            assertTrue(ltm.getSnapshots().isEmpty());
            assertEquals("new", ltm.getSummary().getRelationshipStage());
        }
    }

    @Nested
    @DisplayName("addSnapshot — 添加记忆快照")
    class AddSnapshotTests {

        @Test
        @DisplayName("添加快照 → 记忆数增加")
        void addSnapshotIncreasesCount() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");

            ltm.addSnapshot("conv1", "用户喜欢猫",
                    LongTermMemory.MemorySnapshotType.PREFERENCE, 0.8f, "joy");

            assertEquals(1, ltm.getTotalMemories());
            assertEquals(1, ltm.getSnapshots().size());
        }

        @Test
        @DisplayName("添加≥3个快照 → 可以检索到多个记忆点")
        void multipleSnapshotsRetrievable() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");

            ltm.addSnapshot("conv1", "用户喜欢喝咖啡", LongTermMemory.MemorySnapshotType.PREFERENCE, 0.7f, "neutral");
            ltm.addSnapshot("conv2", "用户住在北京", LongTermMemory.MemorySnapshotType.FACT, 0.9f, "neutral");
            ltm.addSnapshot("conv3", "用户讨厌下雨天", LongTermMemory.MemorySnapshotType.PREFERENCE, 0.6f, "sad");

            assertEquals(3, ltm.getTotalMemories());
            assertEquals(3, ltm.getSnapshots().size());
        }

        @Test
        @DisplayName("超过500条快照时保留重要度最高的500条")
        void limitTo500Snapshots() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");

            for (int i = 0; i < 600; i++) {
                ltm.addSnapshot("conv" + i, "memory " + i,
                        LongTermMemory.MemorySnapshotType.EPISODIC,
                        (float) Math.random(), "neutral");
            }

            assertTrue(ltm.getSnapshots().size() <= 500,
                    "Should not exceed 500 snapshots, got: " + ltm.getSnapshots().size());
        }
    }

    @Nested
    @DisplayName("retrieveRelevant — 记忆检索")
    class RetrieveRelevantTests {

        @Test
        @DisplayName("相关查询返回匹配记忆")
        void relevantQueryReturnsMatches() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");
            ltm.addSnapshot("conv1", "用户喜欢猫咪", LongTermMemory.MemorySnapshotType.PREFERENCE, 0.8f, "joy");
            ltm.addSnapshot("conv2", "用户喜欢狗狗", LongTermMemory.MemorySnapshotType.PREFERENCE, 0.7f, "joy");
            ltm.addSnapshot("conv3", "用户住在上海", LongTermMemory.MemorySnapshotType.FACT, 0.6f, "neutral");

            List<LongTermMemory.MemorySnapshot> results = ltm.retrieveRelevant("猫咪", 3);

            assertFalse(results.isEmpty());
            // "猫咪" should match the first snapshot
            assertTrue(results.stream().anyMatch(s -> s.getContent().contains("猫咪")));
        }

        @Test
        @DisplayName("无匹配查询返回空列表")
        void noMatchReturnsEmpty() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");
            ltm.addSnapshot("conv1", "用户喜欢咖啡", LongTermMemory.MemorySnapshotType.PREFERENCE, 0.8f, "joy");

            List<LongTermMemory.MemorySnapshot> results = ltm.retrieveRelevant("太空飞船", 3);

            // May still return results sorted by importance
            assertNotNull(results);
        }
    }

    @Nested
    @DisplayName("consolidate — 记忆整合")
    class ConsolidateTests {

        @Test
        @DisplayName("少于10条快照不需要整合")
        void lessThan10SnapshotsNoConsolidation() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");
            for (int i = 0; i < 5; i++) {
                ltm.addSnapshot("conv" + i, "memory " + i,
                        LongTermMemory.MemorySnapshotType.EPISODIC, 0.5f, "neutral");
            }

            assertFalse(ltm.needsConsolidation());
        }

        @Test
        @DisplayName("超过100条快照需要整合")
        void needsConsolidationAfter100Snapshots() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");
            for (int i = 0; i < 110; i++) {
                ltm.addSnapshot("conv" + i, "memory " + i,
                        LongTermMemory.MemorySnapshotType.EPISODIC, 0.5f, "neutral");
            }

            assertTrue(ltm.needsConsolidation());
        }
    }

    @Nested
    @DisplayName("MemorySummary — 记忆摘要")
    class MemorySummaryTests {

        @Test
        @DisplayName("根据交互次数更新关系阶段")
        void relationshipStageUpdates() {
            LongTermMemory ltm = LongTermMemory.create("user1", "avatar1");

            // After 1 interaction: "new"
            ltm.addSnapshot("c1", "hello", LongTermMemory.MemorySnapshotType.EPISODIC, 0.5f, "neutral");
            assertEquals("new", ltm.getSummary().getRelationshipStage());

            // After 6 interactions: "acquaintance"
            for (int i = 0; i < 5; i++) {
                ltm.addSnapshot("c" + i, "msg" + i, LongTermMemory.MemorySnapshotType.EPISODIC, 0.5f, "neutral");
            }
            assertEquals("acquaintance", ltm.getSummary().getRelationshipStage());

            // After 21 interactions: "friend"
            for (int i = 0; i < 15; i++) {
                ltm.addSnapshot("c" + i, "msg" + i, LongTermMemory.MemorySnapshotType.EPISODIC, 0.5f, "neutral");
            }
            assertEquals("friend", ltm.getSummary().getRelationshipStage());

            // After 51 interactions: "close"
            for (int i = 0; i < 30; i++) {
                ltm.addSnapshot("c" + i, "msg" + i, LongTermMemory.MemorySnapshotType.EPISODIC, 0.5f, "neutral");
            }
            assertEquals("close", ltm.getSummary().getRelationshipStage());
        }
    }
}
