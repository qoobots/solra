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
}
