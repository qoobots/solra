package com.solra.avt.application.service;

import com.solra.avt.application.dto.*;
import com.solra.avt.domain.model.*;
import com.solra.avt.domain.service.AvatarDomainService;
import com.solra.avt.domain.service.InferenceEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

/**
 * AvtApplicationService — AVT 应用层服务。
 * 编排领域服务、权限校验（stub）、日志/审计。
 */
@Service
public class AvtApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AvtApplicationService.class);

    private final AvatarDomainService domainService;

    public AvtApplicationService(AvatarDomainService domainService) {
        this.domainService = domainService;
    }

    /** AVT-001: 发送消息 */
    public AvtResultDTO.MessageSentResponse sendMessage(SendMessageCommand cmd) {
        log.info("AVT SendMessage: user={} conv={}", cmd.getUserId(), cmd.getConversationId());

        // 安全过滤 (AVT-012)
        if (!domainService.isContentSafe(cmd.getContent())) {
            throw new IllegalArgumentException("Content violates safety policy");
        }

        String convId = cmd.getConversationId() != null ? cmd.getConversationId() : UUID.randomUUID().toString();
        DialogueTurn turn = domainService.sendMessage(cmd.getUserId(), cmd.getSpaceId(), convId,
                cmd.getContent(), cmd.getAttachments(), cmd.getContext());
        return AvtResultDTO.MessageSentResponse.from(turn);
    }

    /** AVT-001 流式: 获取推理引擎用于流式响应 */
    public InferenceEngine getInferenceEngine() {
        return domainService.getInferenceEngine();
    }

    /** 获取对话历史 */
    public AvtResultDTO.ConversationHistoryDTO getHistory(String conversationId, int offset, int limit) {
        List<DialogueTurn> turns = domainService.getHistory(conversationId, offset, limit);
        return new AvtResultDTO.ConversationHistoryDTO(
                turns.stream().map(AvtResultDTO.DialogueTurnDTO::from).collect(Collectors.toList()),
                turns.size());
    }

    /** 获取虚拟人状态 */
    public AvtResultDTO.AvatarStateDTO getAvatarState(String avatarId) {
        return domainService.getAvatarState(avatarId)
                .map(AvtResultDTO.AvatarStateDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Avatar not found: " + avatarId));
    }

    /** 更新情感状态 */
    public AvtResultDTO.EmotionStateDTO updateEmotionState(String avatarId, EmotionCategory category, float intensity) {
        EmotionState es = new EmotionState(category, intensity);
        return domainService.updateEmotionState(avatarId, es)
                .map(AvtResultDTO.EmotionStateDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Avatar not found: " + avatarId));
    }

    /** 查询记忆 */
    public List<AvtResultDTO.MemoryDTO> queryMemory(String userId, List<MemoryType> types,
                                                     float minImportance, int maxResults) {
        return domainService.queryMemory(userId, types, minImportance, maxResults)
                .stream().map(AvtResultDTO.MemoryDTO::from).collect(Collectors.toList());
    }

    // ========== AVT-003: Long-Term Memory ==========

    /** 获取长期记忆摘要 */
    public AvtResultDTO.LongTermMemoryDTO getLongTermMemory(String userId, String avatarId) {
        LongTermMemory ltm = domainService.getOrCreateLongTermMemory(userId, avatarId);
        return AvtResultDTO.LongTermMemoryDTO.from(ltm);
    }

    /** 添加记忆快照 */
    public void addMemorySnapshot(String userId, String avatarId, String conversationId,
                                   String content, String snapshotType, float importance,
                                   String emotionContext) {
        LongTermMemory.MemorySnapshotType type = LongTermMemory.MemorySnapshotType.valueOf(
                snapshotType.toUpperCase());
        domainService.addMemorySnapshot(userId, avatarId, conversationId,
                content, type, importance, emotionContext);
    }

    /** 检索相关记忆 */
    public List<AvtResultDTO.MemorySnapshotDTO> retrieveMemories(String userId, String avatarId,
                                                                   String query, int maxResults) {
        return domainService.retrieveMemories(userId, avatarId, query, maxResults)
                .stream().map(AvtResultDTO.MemorySnapshotDTO::from)
                .collect(Collectors.toList());
    }

    // ========== AVT-004: 5D Emotion Model ==========

    /** 获取5维情感状态 */
    public AvtResultDTO.FiveDEmotionDTO getEmotionState(String avatarId) {
        FiveDimensionalEmotion emotion = domainService.getOrCreateEmotionState(avatarId);
        return AvtResultDTO.FiveDEmotionDTO.from(emotion);
    }

    /** 应用情感事件 */
    public AvtResultDTO.FiveDEmotionDTO applyEmotionEvent(String avatarId,
                                                           String eventType, float intensity) {
        FiveDimensionalEmotion.EmotionEventType type =
                FiveDimensionalEmotion.EmotionEventType.valueOf(eventType.toUpperCase());
        FiveDimensionalEmotion emotion = domainService.applyEmotionEvent(avatarId, type, intensity);
        return AvtResultDTO.FiveDEmotionDTO.from(emotion);
    }

    /** 情感衰减 */
    public AvtResultDTO.FiveDEmotionDTO decayEmotions(String avatarId, float rate) {
        domainService.decayEmotions(avatarId, rate);
        return getEmotionState(avatarId);
    }

    // ========== AVT-006: Avatar Expression ==========

    /** 获取虚拟人表情 */
    public AvtResultDTO.AvatarExpressionDTO getAvatarExpression(String avatarId,
                                                                  String expressionType,
                                                                  float intensity) {
        if (expressionType != null && !expressionType.isBlank()) {
            AvatarExpression.ExpressionType type =
                    AvatarExpression.ExpressionType.valueOf(expressionType.toUpperCase());
            AvatarExpression expr = domainService.generateExpression(type,
                    intensity > 0 ? intensity : 0.8f);
            return AvtResultDTO.AvatarExpressionDTO.from(expr);
        }
        AvatarExpression expr = domainService.getExpressionFromEmotion(avatarId);
        return AvtResultDTO.AvatarExpressionDTO.from(expr);
    }

    // ========== AVT-008: Surprise Engine ==========

    /** 评估并触发惊喜时刻 */
    public AvtResultDTO.SurpriseMomentDTO evaluateSurprise(String userId,
                                                             String conversationId,
                                                             String avatarId) {
        return domainService.evaluateSurprise(userId, conversationId, avatarId)
                .map(AvtResultDTO.SurpriseMomentDTO::from)
                .orElse(null);
    }

    /** 获取惊喜统计 */
    public AvtResultDTO.SurpriseStatsDTO getSurpriseStats(String userId) {
        SurpriseEngine.SurpriseStats stats = domainService.getSurpriseStats(userId);
        return AvtResultDTO.SurpriseStatsDTO.from(stats);
    }
}
