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

    // ========== AVT-005: Personalization Training ==========

    /** 获取个性化训练档案 */
    public AvtResultDTO.PersonalizationProfileDTO getPersonalizationProfile(String userId,
                                                                               String avatarId) {
        PersonalizationProfile profile = domainService.getPersonalizationProfile(userId, avatarId);
        return AvtResultDTO.PersonalizationProfileDTO.from(profile);
    }

    /** 应用个性化反馈 */
    public AvtResultDTO.PersonalizationProfileDTO applyPersonalizationFeedback(String userId,
                                                                                 String avatarId,
                                                                                 String feedbackType,
                                                                                 float intensity) {
        PersonalizationProfile.FeedbackType type =
                PersonalizationProfile.FeedbackType.valueOf(feedbackType.toUpperCase());
        PersonalizationProfile profile = domainService.applyPersonalizationFeedback(
                userId, avatarId, type, intensity);
        return AvtResultDTO.PersonalizationProfileDTO.from(profile);
    }

    // ========== AVT-007: Spatial Movement ==========

    /** 获取虚拟人空间位置 */
    public AvtResultDTO.SpatialPositionDTO getSpatialPosition(String avatarId) {
        return domainService.getSpatialPosition(avatarId)
                .map(AvtResultDTO.SpatialPositionDTO::from)
                .orElse(null);
    }

    /** 更新虚拟人空间位置 */
    public AvtResultDTO.SpatialPositionDTO updateSpatialPosition(String avatarId,
                                                                   float x, float y, float z,
                                                                   float rotationY, String zoneId) {
        SpatialPosition pos = domainService.updateSpatialPosition(avatarId, x, y, z, rotationY, zoneId);
        return AvtResultDTO.SpatialPositionDTO.from(pos);
    }

    /** 移动虚拟人朝向目标 */
    public AvtResultDTO.SpatialPositionDTO moveAvatarTowards(String avatarId,
                                                               float targetX, float targetY,
                                                               float targetZ, float deltaSeconds) {
        SpatialPosition pos = domainService.moveAvatarTowards(avatarId, targetX, targetY,
                targetZ, deltaSeconds);
        return AvtResultDTO.SpatialPositionDTO.from(pos);
    }

    /** 设置虚拟人移动路径 */
    public AvtResultDTO.SpatialPositionDTO setAvatarPath(String avatarId,
                                                           List<SpatialPosition.Waypoint> waypoints) {
        SpatialPosition pos = domainService.setAvatarPath(avatarId, waypoints);
        return AvtResultDTO.SpatialPositionDTO.from(pos);
    }

    /** 开始自主巡逻 */
    public AvtResultDTO.SpatialPositionDTO startPatrol(String avatarId, String zoneId,
                                                         int waypointCount) {
        SpatialPosition pos = domainService.startPatrol(avatarId, zoneId, waypointCount);
        return AvtResultDTO.SpatialPositionDTO.from(pos);
    }

    /** 注册导航区域 */
    public void registerZone(String zoneId, float minX, float maxX, float minZ, float maxZ) {
        domainService.registerZone(zoneId, minX, maxX, minZ, maxZ);
    }

    // ========== AVT-009: Affection System ==========

    /** 获取好感度等级 */
    public AvtResultDTO.AffectionLevelDTO getAffection(String userId, String avatarId) {
        return domainService.getAffection(userId, avatarId)
                .map(AvtResultDTO.AffectionLevelDTO::from)
                .orElse(null);
    }

    /** 记录好感度事件 */
    public AvtResultDTO.AffectionLevelDTO recordAffection(String userId, String avatarId,
                                                            String source, int basePoints,
                                                            String reason) {
        AffectionLevel.AffectionSource affectionSource =
                AffectionLevel.AffectionSource.valueOf(source.toUpperCase());
        domainService.recordAffection(userId, avatarId, affectionSource, basePoints, reason);
        return getAffection(userId, avatarId);
    }

    /** 记录每日访问 */
    public AvtResultDTO.AffectionLevelDTO recordDailyVisit(String userId, String avatarId,
                                                             int consecutiveDays) {
        domainService.recordDailyVisit(userId, avatarId, consecutiveDays);
        return getAffection(userId, avatarId);
    }

    /** 获取好感度升级进度 */
    public AvtResultDTO.AffectionProgressDTO getAffectionProgress(String userId, String avatarId) {
        AffectionLevel affection = domainService.getAffection(userId, avatarId).orElse(null);
        if (affection == null) return null;
        int progress = domainService.getAffectionLevelProgress(userId, avatarId);
        return new AvtResultDTO.AffectionProgressDTO(
                affection.getLevel(), affection.getTitle(), affection.getScore(),
                progress, affection.getUpdatedAt());
    }
}
