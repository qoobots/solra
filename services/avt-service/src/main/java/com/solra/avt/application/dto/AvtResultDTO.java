package com.solra.avt.application.dto;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.service.SurpriseEngine;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AvtResultDTO {

    public record MessageSentResponse(String conversationId, String turnId, String content,
                                       TurnRole role, List<TokenChunkDTO> chunks, Instant timestamp) {
        public static MessageSentResponse from(DialogueTurn turn) {
            List<TokenChunkDTO> chunkDtos = turn.getChunks() != null
                    ? turn.getChunks().stream().map(TokenChunkDTO::from).collect(Collectors.toList())
                    : List.of();
            return new MessageSentResponse(turn.getConversationId(), turn.getTurnId(),
                    turn.getContent(), turn.getRole(), chunkDtos, turn.getTimestamp());
        }
    }

    public record TokenChunkDTO(int sequence, String token, boolean isFinal) {
        public static TokenChunkDTO from(TokenChunk c) {
            return new TokenChunkDTO(c.getSequence(), c.getToken(), c.isFinal());
        }
    }

    public record ConversationHistoryDTO(List<DialogueTurnDTO> turns, long total) {}

    public record DialogueTurnDTO(String turnId, String conversationId, String role,
                                   String content, Instant timestamp) {
        public static DialogueTurnDTO from(DialogueTurn t) {
            return new DialogueTurnDTO(t.getTurnId(), t.getConversationId(),
                    t.getRole().name(), t.getContent(), t.getTimestamp());
        }
    }

    public record AvatarStateDTO(String avatarId, String primaryEmotion, float emotionIntensity,
                                  String activity, Instant lastActive) {
        public static AvatarStateDTO from(AvatarState state) {
            EmotionState em = state.getEmotion();
            return new AvatarStateDTO(state.getAvatarId(),
                    em != null ? em.getPrimaryEmotion().name() : "NEUTRAL",
                    em != null ? em.getIntensity() : 0f,
                    state.getActivity().name(), state.getLastActive());
        }
    }

    public record EmotionStateDTO(String primaryEmotion, float intensity, Instant detectedAt) {
        public static EmotionStateDTO from(EmotionState es) {
            return new EmotionStateDTO(es.getPrimaryEmotion().name(), es.getIntensity(), es.getDetectedAt());
        }
    }

    public record MemoryDTO(String memoryId, String userId, String type, String content,
                             float importance, Instant createdAt, Instant lastAccessed) {
        public static MemoryDTO from(MemoryEntry m) {
            return new MemoryDTO(m.getMemoryId(), m.getUserId(), m.getType().name(),
                    m.getContent(), m.getImportance(), m.getCreatedAt(), m.getLastAccessed());
        }
    }

    // ========== AVT-003: Long-Term Memory DTOs ==========

    public record LongTermMemoryDTO(String memoryId, String userId, String avatarId,
                                     MemorySummaryDTO summary, List<MemorySnapshotDTO> snapshots,
                                     int totalMemories, Instant createdAt) {
        public static LongTermMemoryDTO from(LongTermMemory ltm) {
            return new LongTermMemoryDTO(ltm.getMemoryId(), ltm.getUserId(), ltm.getAvatarId(),
                    MemorySummaryDTO.from(ltm.getSummary()),
                    ltm.getSnapshots().stream().map(MemorySnapshotDTO::from).collect(Collectors.toList()),
                    ltm.getTotalMemories(), ltm.getCreatedAt());
        }
    }

    public record MemorySummaryDTO(List<String> knownFacts, List<String> preferences,
                                    List<String> topics, String relationshipStage,
                                    int totalInteractions) {
        public static MemorySummaryDTO from(LongTermMemory.MemorySummary summary) {
            return new MemorySummaryDTO(summary.getKnownFacts(), summary.getPreferences(),
                    summary.getTopics(), summary.getRelationshipStage(),
                    summary.getTotalInteractions());
        }
    }

    public record MemorySnapshotDTO(String snapshotId, String conversationId, String content,
                                     String type, float importance, String emotionContext,
                                     Instant capturedAt) {
        public static MemorySnapshotDTO from(LongTermMemory.MemorySnapshot s) {
            return new MemorySnapshotDTO(s.getSnapshotId(), s.getConversationId(),
                    s.getContent(), s.getType().name(), s.getImportance(),
                    s.getEmotionContext(), s.getCapturedAt());
        }
    }

    // ========== AVT-004: 5D Emotion DTOs ==========

    public record FiveDEmotionDTO(float joy, float curiosity, float coldness, float jealousy,
                                   float sadness, String dominantDimension, String currentMood,
                                   Instant lastUpdated) {
        public static FiveDEmotionDTO from(FiveDimensionalEmotion emotion) {
            return new FiveDEmotionDTO(emotion.getJoy(), emotion.getCuriosity(),
                    emotion.getColdness(), emotion.getJealousy(), emotion.getSadness(),
                    emotion.getDominantDimension(), emotion.getCurrentMood(),
                    emotion.getLastUpdated());
        }
    }

    // ========== AVT-006: Avatar Expression DTOs ==========

    public record AvatarExpressionDTO(String expressionId, String expressionType, float intensity,
                                       List<BlendshapeWeightDTO> blendshapes, String gesture,
                                       String animationClip) {
        public static AvatarExpressionDTO from(AvatarExpression expr) {
            return new AvatarExpressionDTO(expr.getExpressionId(),
                    expr.getExpressionType().name(), expr.getIntensity(),
                    expr.getBlendshapes().stream()
                            .map(b -> new BlendshapeWeightDTO(b.blendshapeName(), b.weight()))
                            .collect(Collectors.toList()),
                    expr.getGesture() != null ? expr.getGesture().name() : "IDLE",
                    expr.getAnimationClip());
        }
    }

    public record BlendshapeWeightDTO(String blendshapeName, float weight) {}

    // ========== AVT-008: Surprise Engine DTOs ==========

    public record SurpriseMomentDTO(String surpriseId, String userId, String conversationId,
                                     String surpriseType, String message,
                                     AvatarExpressionDTO expression, Instant generatedAt) {
        public static SurpriseMomentDTO from(SurpriseEngine.SurpriseMoment moment) {
            return new SurpriseMomentDTO(moment.surpriseId(), moment.userId(),
                    moment.conversationId(), moment.type().name(), moment.message(),
                    AvatarExpressionDTO.from(moment.expression()), moment.generatedAt());
        }
    }

    public record SurpriseStatsDTO(String userId, int totalSurprises, int todaySurprises) {
        public static SurpriseStatsDTO from(SurpriseEngine.SurpriseStats stats) {
            return new SurpriseStatsDTO(stats.userId(), stats.totalSurprises(),
                    stats.todaySurprises());
        }
    }

    // ========== AVT-005: Personalization Training DTOs ==========

    public record PersonalizationProfileDTO(
            String userId, String avatarId,
            String dominantStyle, float verbosityPreference,
            float emojiUsagePreference, float proactiveLevel,
            Map<String, Float> topicPreferences,
            float positiveFeedbackRate, int totalInteractions,
            Instant updatedAt) {
        public static PersonalizationProfileDTO from(PersonalizationProfile profile) {
            return new PersonalizationProfileDTO(
                    profile.getUserId(), profile.getAvatarId(),
                    profile.getDominantStyle(), profile.getVerbosityPreference(),
                    profile.getEmojiUsagePreference(), profile.getProactiveLevel(),
                    new HashMap<>(profile.getTopicPreferences()),
                    profile.getPositiveFeedbackRate(), profile.getTotalInteractions(),
                    profile.getUpdatedAt());
        }
    }

    // ========== AVT-007: Spatial Movement DTOs ==========

    public record SpatialPositionDTO(
            String avatarId, float x, float y, float z,
            float rotationY, float rotationX,
            String movementState, String currentZoneId,
            float moveSpeed, Instant lastMoved) {
        public static SpatialPositionDTO from(SpatialPosition pos) {
            return new SpatialPositionDTO(
                    pos.getAvatarId(), pos.getX(), pos.getY(), pos.getZ(),
                    pos.getRotationY(), pos.getRotationX(),
                    pos.getMovementState().name(), pos.getCurrentZoneId(),
                    pos.getMoveSpeed(), pos.getLastMoved());
        }
    }

    public record WaypointDTO(float x, float y, float z, float pauseSeconds, String label) {
        public static WaypointDTO from(SpatialPosition.Waypoint w) {
            return new WaypointDTO(w.x(), w.y(), w.z(), w.pauseSeconds(), w.label());
        }
        public SpatialPosition.Waypoint toDomain() {
            return new SpatialPosition.Waypoint(x, y, z, pauseSeconds, label);
        }
    }

    // ========== AVT-009: Affection System DTOs ==========

    public record AffectionLevelDTO(
            String userId, String avatarId,
            int score, int level, String title,
            int dialogueQualityScore, int interactionFrequencyScore,
            int userBehaviorScore, int timeBonusScore,
            Instant updatedAt) {
        public static AffectionLevelDTO from(AffectionLevel affection) {
            return new AffectionLevelDTO(
                    affection.getUserId(), affection.getAvatarId(),
                    affection.getScore(), affection.getLevel(), affection.getTitle(),
                    affection.getDialogueQualityScore(),
                    affection.getInteractionFrequencyScore(),
                    affection.getUserBehaviorScore(),
                    affection.getTimeBonusScore(),
                    affection.getUpdatedAt());
        }
    }

    public record AffectionProgressDTO(
            int level, String title, int score,
            int progressPercent, Instant updatedAt) {}

    public record AffectionEventDTO(
            String source, int points, String reason,
            int oldLevel, int newLevel, Instant timestamp) {
        public static AffectionEventDTO from(AffectionLevel.AffectionEvent event) {
            return new AffectionEventDTO(
                    event.source(), event.points(), event.reason(),
                    event.oldLevel(), event.newLevel(), event.timestamp());
        }
    }
}
