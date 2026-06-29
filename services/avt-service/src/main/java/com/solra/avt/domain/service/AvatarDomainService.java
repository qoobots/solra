package com.solra.avt.domain.service;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * AvatarDomainService — 虚拟人交互核心领域服务。
 * 编排对话流程：接收消息 → 推理 → 记忆存储 → 情感更新 → 返回响应。
 */
public class AvatarDomainService {

    private static final Logger log = LoggerFactory.getLogger(AvatarDomainService.class);

    private final InferenceEngine inferenceEngine;
    private final ConversationRepository conversationRepo;
    private final DialogueTurnRepository turnRepo;
    private final AvatarRepository avatarRepo;
    private final MemoryRepository memoryRepo;

    public AvatarDomainService(InferenceEngine inferenceEngine,
                               ConversationRepository conversationRepo,
                               DialogueTurnRepository turnRepo,
                               AvatarRepository avatarRepo,
                               MemoryRepository memoryRepo) {
        this.inferenceEngine = inferenceEngine;
        this.conversationRepo = conversationRepo;
        this.turnRepo = turnRepo;
        this.avatarRepo = avatarRepo;
        this.memoryRepo = memoryRepo;
    }

    /** AVT-001: 发送消息 → 虚拟人同步响应 */
    public DialogueTurn sendMessage(String userId, String spaceId, String conversationId,
                                     String content, List<MessageAttachment> attachments,
                                     Map<String, String> context) {
        // 1. 获取或创建对话
        Conversation conv = conversationRepo.findById(conversationId)
                .orElseGet(() -> createConversation(conversationId, userId, spaceId, "default-avatar"));

        if (conv.getStatus() == ConversationStatus.ENDED) {
            throw new IllegalStateException("Conversation already ended: " + conversationId);
        }

        // 2. 保存用户发言
        DialogueTurn userTurn = new DialogueTurn(UUID.randomUUID().toString(), conversationId, TurnRole.USER, content);
        userTurn.setMetadata(context);
        turnRepo.save(userTurn);

        // 3. 获取最近对话历史（最近20轮）
        List<DialogueTurn> history = turnRepo.findByConversationId(conversationId, 0, 40);

        // 4. 检索相关记忆
        List<MemoryEntry> memories = memoryRepo.findByUserId(userId,
                List.of(MemoryType.EPISODIC, MemoryType.SEMANTIC, MemoryType.FACT, MemoryType.PREFERENCE),
                0.3f, 5);
        Map<String, String> enrichedContext = new HashMap<>(context != null ? context : Map.of());
        if (!memories.isEmpty()) {
            enrichedContext.put("relevant_memories", serializeMemories(memories));
        }

        // 5. 调用推理引擎
        InferenceEngine.InferenceResult result = inferenceEngine.infer(
                conversationId, content, history, enrichedContext);

        // 6. 保存虚拟人回复
        DialogueTurn avatarTurn = new DialogueTurn(
                UUID.randomUUID().toString(), conversationId, TurnRole.AVATAR, result.responseText());
        avatarTurn.setMetadata(result.metadata());
        turnRepo.save(avatarTurn);

        // 7. 更新虚拟人情感状态
        if (result.detectedEmotion() != null) {
            avatarRepo.findById(conv.getAvatarId()).ifPresent(avatar -> {
                avatar.getState().setEmotion(result.detectedEmotion());
                avatar.getState().setLastActive(java.time.Instant.now());
                avatarRepo.save(avatar);
            });
        }

        // 8. 异步存储长期记忆（高重要性内容）
        if (!content.isBlank() && content.length() > 20) {
            MemoryEntry mem = new MemoryEntry(UUID.randomUUID().toString(), userId,
                    MemoryType.EPISODIC, "对话: " + truncate(content, 200),
                    inferImportance(content, result.responseText()));
            mem.setConversationId(conversationId);
            memoryRepo.save(mem);
        }

        conv.touch();
        conversationRepo.save(conv);

        log.info("AVT-001 message processed: conv={} user={} tokens_in={}", conversationId, userId, content.length());
        return avatarTurn;
    }

    /** AVT-001 流式: 获取流式 Token 发布者 */
    public InferenceEngine getInferenceEngine() {
        return inferenceEngine;
    }

    /** AVT-012: 对话安全过滤（委托给 saf-service 或本地过滤器） */
    public boolean isContentSafe(String content) {
        // 基础本地过滤：拦截明显违规关键词
        if (content == null || content.isBlank()) return true;
        return !containsBlockedPattern(content);
    }

    /** 获取对话历史 */
    public List<DialogueTurn> getHistory(String conversationId, int offset, int limit) {
        return turnRepo.findByConversationId(conversationId, offset, limit);
    }

    /** 获取虚拟人状态 */
    public Optional<AvatarState> getAvatarState(String avatarId) {
        return avatarRepo.findById(avatarId).map(Avatar::getState);
    }

    /** 更新情感状态 */
    public Optional<EmotionState> updateEmotionState(String avatarId, EmotionState newEmotion) {
        return avatarRepo.findById(avatarId).map(avatar -> {
            avatar.getState().setEmotion(newEmotion);
            avatar.getState().setLastActive(java.time.Instant.now());
            avatarRepo.save(avatar);
            return newEmotion;
        });
    }

    /** 查询记忆 */
    public List<MemoryEntry> queryMemory(String userId, List<MemoryType> types,
                                          float minImportance, int maxResults) {
        return memoryRepo.findByUserId(userId, types != null ? types : List.of(MemoryType.values()),
                minImportance, maxResults);
    }

    // ---- private helpers ----

    private Conversation createConversation(String convId, String userId, String spaceId, String avatarId) {
        Conversation conv = new Conversation(convId, userId, spaceId, avatarId);
        conversationRepo.save(conv);

        // 确保虚拟人存在
        if (avatarRepo.findById(avatarId).isEmpty()) {
            Avatar defaultAvatar = new Avatar(avatarId, "Solra AI");
            avatarRepo.save(defaultAvatar);
        }
        return conv;
    }

    private String serializeMemories(List<MemoryEntry> memories) {
        StringBuilder sb = new StringBuilder();
        for (MemoryEntry m : memories) {
            sb.append("[").append(m.getType()).append("] ").append(truncate(m.getContent(), 100)).append("; ");
        }
        return sb.toString();
    }

    private float inferImportance(String userMsg, String avatarResponse) {
        // 简单启发式：长消息 + 情感关键词 → 高重要性
        float score = Math.min(1f, userMsg.length() / 200f);
        String lower = userMsg.toLowerCase();
        if (lower.contains("记住") || lower.contains("喜欢") || lower.contains("讨厌") || lower.contains("重要")) {
            score += 0.3f;
        }
        return Math.min(1f, score);
    }

    private boolean containsBlockedPattern(String content) {
        String lower = content.toLowerCase();
        // 基础敏感词列表（生产环境会委托给 saf-service）
        String[] blocked = {"自杀", "自残", "毒品", "赌博", "武器交易", "儿童色情"};
        for (String word : blocked) {
            if (lower.contains(word)) return true;
        }
        return false;
    }

    private String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 3) + "...";
    }
}
