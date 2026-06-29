package com.solra.avt.application.dto;

import com.solra.avt.domain.model.*;

import java.time.Instant;
import java.util.List;
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
}
