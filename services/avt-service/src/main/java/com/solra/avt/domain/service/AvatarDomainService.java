package com.solra.avt.domain.service;

import com.solra.avt.domain.model.*;
import com.solra.avt.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AvatarDomainService — 虚拟人交互核心领域服务。
 * 编排对话流程：接收消息 → 推理 → 记忆存储 → 情感更新 → 返回响应。
 *
 * Covers: AVT-001 (实时对话), AVT-002 (社交主动性), AVT-003 (长期记忆),
 *         AVT-004 (5维情感模型), AVT-005 (个性化训练), AVT-006 (肢体表情),
 *         AVT-007 (空间移动追踪), AVT-008 (惊喜时刻), AVT-009 (好感度系统),
 *         AVT-012 (安全过滤).
 */
public class AvatarDomainService {

    private static final Logger log = LoggerFactory.getLogger(AvatarDomainService.class);

    private final InferenceEngine inferenceEngine;
    private final ConversationRepository conversationRepo;
    private final DialogueTurnRepository turnRepo;
    private final AvatarRepository avatarRepo;
    private final MemoryRepository memoryRepo;
    private final SurpriseEngine surpriseEngine;
    private final AffectionSystemService affectionService;
    private final SpatialMovementService spatialService;
    private final PersonalizationTrainingService personalizationService;

    // AVT-004: 5D emotion state per avatar
    private final Map<String, FiveDimensionalEmotion> emotionStates = new HashMap<>();

    // AVT-003: Long-term memory per user+avatar
    private final Map<String, LongTermMemory> longTermMemories = new HashMap<>();

    public AvatarDomainService(InferenceEngine inferenceEngine,
                               ConversationRepository conversationRepo,
                               DialogueTurnRepository turnRepo,
                               AvatarRepository avatarRepo,
                               MemoryRepository memoryRepo,
                               SurpriseEngine surpriseEngine,
                               AffectionSystemService affectionService,
                               SpatialMovementService spatialService,
                               PersonalizationTrainingService personalizationService) {
        this.inferenceEngine = inferenceEngine;
        this.conversationRepo = conversationRepo;
        this.turnRepo = turnRepo;
        this.avatarRepo = avatarRepo;
        this.memoryRepo = memoryRepo;
        this.surpriseEngine = surpriseEngine;
        this.affectionService = affectionService;
        this.spatialService = spatialService;
        this.personalizationService = personalizationService;
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

        // AVT-005: 注入个性化偏好到推理上下文
        String personalization = personalizationService.buildSystemPromptCustomization(
                userId, conv.getAvatarId());
        enrichedContext.put("personalization", personalization);

        // AVT-009: 注入好感度亲密语调
        String intimacyTone = affectionService.getIntimacyTone(userId, conv.getAvatarId());
        enrichedContext.put("intimacy_tone", intimacyTone);

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

        // AVT-009: 记录对话好感度
        boolean hasEmotion = result.detectedEmotion() != null;
        boolean hasRecall = !memories.isEmpty();
        affectionService.recordConversationAffection(userId, conv.getAvatarId(),
                content.length(), hasEmotion, hasRecall, history.size() / 2 + 1);

        // AVT-005: 更新话题偏好
        personalizationService.updateTopicsFromConversation(userId, conv.getAvatarId(), content);

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

    // ========== AVT-003: Long-Term Memory ==========

    /**
     * AVT-003: Get or create long-term memory for a user+avatar pair.
     */
    public LongTermMemory getOrCreateLongTermMemory(String userId, String avatarId) {
        String key = userId + "::" + avatarId;
        return longTermMemories.computeIfAbsent(key,
                k -> LongTermMemory.create(userId, avatarId));
    }

    /**
     * AVT-003: Add a memory snapshot after conversation.
     */
    public void addMemorySnapshot(String userId, String avatarId, String conversationId,
                                   String content, LongTermMemory.MemorySnapshotType type,
                                   float importance, String emotionContext) {
        LongTermMemory ltm = getOrCreateLongTermMemory(userId, avatarId);
        ltm.addSnapshot(conversationId, content, type, importance, emotionContext);

        if (ltm.needsConsolidation()) {
            ltm.consolidate();
            log.info("AVT-003 memory consolidated: user={} avatar={} totalMemories={}",
                    userId, avatarId, ltm.getTotalMemories());
        }
    }

    /**
     * AVT-003: Retrieve relevant long-term memories for context.
     */
    public List<LongTermMemory.MemorySnapshot> retrieveMemories(String userId, String avatarId,
                                                                  String query, int maxResults) {
        LongTermMemory ltm = getOrCreateLongTermMemory(userId, avatarId);
        return ltm.retrieveRelevant(query, maxResults);
    }

    /**
     * AVT-003: Get memory summary (what avatar knows about user).
     */
    public LongTermMemory.MemorySummary getMemorySummary(String userId, String avatarId) {
        return getOrCreateLongTermMemory(userId, avatarId).getSummary();
    }

    // ========== AVT-004: 5D Emotion Model ==========

    /**
     * AVT-004: Get or create 5D emotion state for an avatar.
     */
    public FiveDimensionalEmotion getOrCreateEmotionState(String avatarId) {
        return emotionStates.computeIfAbsent(avatarId, k -> new FiveDimensionalEmotion());
    }

    /**
     * AVT-004: Apply an emotion event to the avatar's 5D state.
     */
    public FiveDimensionalEmotion applyEmotionEvent(String avatarId,
                                                     FiveDimensionalEmotion.EmotionEventType eventType,
                                                     float intensity) {
        FiveDimensionalEmotion emotion = getOrCreateEmotionState(avatarId);
        emotion.applyEvent(eventType, intensity);
        log.debug("AVT-004 emotion event: avatar={} event={} mood={}",
                avatarId, eventType, emotion.getCurrentMood());
        return emotion;
    }

    /**
     * AVT-004: Decay emotions towards neutral (called periodically).
     */
    public void decayEmotions(String avatarId, float rate) {
        FiveDimensionalEmotion emotion = emotionStates.get(avatarId);
        if (emotion != null) {
            emotion.decay(rate);
        }
    }

    // ========== AVT-006: Avatar Expression ==========

    /**
     * AVT-006: Generate expression from 5D emotion state.
     */
    public AvatarExpression getExpressionFromEmotion(String avatarId) {
        FiveDimensionalEmotion emotion = getOrCreateEmotionState(avatarId);
        return AvatarExpression.fromEmotion(emotion);
    }

    /**
     * AVT-006: Generate specific expression by type.
     */
    public AvatarExpression generateExpression(AvatarExpression.ExpressionType type, float intensity) {
        return AvatarExpression.fromType(type, intensity);
    }

    // ========== AVT-008: Surprise Engine ==========

    /**
     * AVT-008: Evaluate and potentially trigger a surprise moment.
     */
    public Optional<SurpriseEngine.SurpriseMoment> evaluateSurprise(String userId,
                                                                      String conversationId,
                                                                      String avatarId) {
        List<DialogueTurn> recentHistory = turnRepo.findByConversationId(conversationId, 0, 10);
        List<LongTermMemory.MemorySnapshot> memories = retrieveMemories(userId, avatarId, "", 5);
        FiveDimensionalEmotion emotion = getOrCreateEmotionState(avatarId);

        return surpriseEngine.evaluate(userId, conversationId, recentHistory, memories, emotion);
    }

    /**
     * AVT-008: Get surprise statistics for a user.
     */
    public SurpriseEngine.SurpriseStats getSurpriseStats(String userId) {
        return surpriseEngine.getStats(userId);
    }

    // ========== AVT-005: Personalization Training ==========

    /** Get personalization profile */
    public PersonalizationProfile getPersonalizationProfile(String userId, String avatarId) {
        return personalizationService.getOrCreate(userId, avatarId);
    }

    /** Apply feedback to personalization */
    public PersonalizationProfile applyPersonalizationFeedback(String userId, String avatarId,
                                                                 PersonalizationProfile.FeedbackType type,
                                                                 float intensity) {
        return personalizationService.applyFeedback(userId, avatarId, type, intensity);
    }

    /** Get dominant conversation style */
    public String getDominantStyle(String userId, String avatarId) {
        return personalizationService.getDominantStyle(userId, avatarId);
    }

    /** Get verbosity preference */
    public float getVerbosityPreference(String userId, String avatarId) {
        return personalizationService.getVerbosityPreference(userId, avatarId);
    }

    // ========== AVT-007: Spatial Movement ==========

    /** Get spatial position of an avatar */
    public Optional<SpatialPosition> getSpatialPosition(String avatarId) {
        return spatialService.getPosition(avatarId);
    }

    /** Update avatar spatial position */
    public SpatialPosition updateSpatialPosition(String avatarId, float x, float y, float z,
                                                  float rotationY, String zoneId) {
        return spatialService.updatePosition(avatarId, x, y, z, rotationY, zoneId);
    }

    /** Move avatar towards a target */
    public SpatialPosition moveAvatarTowards(String avatarId, float targetX, float targetY,
                                              float targetZ, float deltaSeconds) {
        return spatialService.moveTowards(avatarId, targetX, targetY, targetZ, deltaSeconds);
    }

    /** Set avatar movement path */
    public SpatialPosition setAvatarPath(String avatarId, List<SpatialPosition.Waypoint> waypoints) {
        return spatialService.setPath(avatarId, waypoints);
    }

    /** Start autonomous patrol */
    public SpatialPosition startPatrol(String avatarId, String zoneId, int waypointCount) {
        return spatialService.startPatrol(avatarId, zoneId, waypointCount);
    }

    /** Register a navigation zone */
    public void registerZone(String zoneId, float minX, float maxX, float minZ, float maxZ) {
        spatialService.registerZone(zoneId, minX, maxX, minZ, maxZ);
    }

    // ========== AVT-009: Affection System ==========

    /** Get affection level for user-avatar pair */
    public Optional<AffectionLevel> getAffection(String userId, String avatarId) {
        return affectionService.getAffection(userId, avatarId);
    }

    /** Record affection from a specific source */
    public int recordAffection(String userId, String avatarId,
                                AffectionLevel.AffectionSource source, int basePoints, String reason) {
        return affectionService.recordAffection(userId, avatarId, source, basePoints, reason);
    }

    /** Record daily visit affection */
    public void recordDailyVisit(String userId, String avatarId, int consecutiveDays) {
        affectionService.recordDailyVisit(userId, avatarId, consecutiveDays);
    }

    /** Get affection level progress to next level (0-100) */
    public int getAffectionLevelProgress(String userId, String avatarId) {
        return affectionService.getLevelProgress(userId, avatarId);
    }

    /** Get intimacy tone based on affection level */
    public String getIntimacyTone(String userId, String avatarId) {
        return affectionService.getIntimacyTone(userId, avatarId);
    }

    /** Check if an interaction is unlocked at current affection level */
    public boolean isInteractionUnlocked(String userId, String avatarId,
                                          AffectionLevel.UnlockableInteraction interaction) {
        return affectionService.isInteractionUnlocked(userId, avatarId, interaction);
    }
}
