package com.solra.avt.domain.service;

import com.solra.avt.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * SurpriseEngine — AVT-008 虚拟人惊喜时刻触发引擎。
 *
 * 频率控制：每3-5次对话触发1次"记住你"类型的惊喜。
 * 惊喜类型：记忆召回、情感表达、个性化互动、随机惊喜。
 */
public class SurpriseEngine {

    private static final Logger log = LoggerFactory.getLogger(SurpriseEngine.class);

    /** 惊喜触发间隔：每3-5次对话 */
    private static final int MIN_INTERVAL_TURNS = 3;
    private static final int MAX_INTERVAL_TURNS = 5;

    /** 全局冷却时间（秒） */
    private static final long COOLDOWN_SECONDS = 300; // 5分钟

    /** 每个用户每天最大惊喜次数 */
    private static final int MAX_SURPRISES_PER_DAY = 8;

    private final Random random = new Random();
    private final Map<String, Deque<SurpriseRecord>> userSurpriseHistory = new HashMap<>();
    private final Map<String, Integer> userConversationCount = new HashMap<>();

    /**
     * Evaluate whether a surprise moment should be triggered.
     */
    public Optional<SurpriseMoment> evaluate(String userId, String conversationId,
                                              List<DialogueTurn> recentHistory,
                                              List<LongTermMemory.MemorySnapshot> memories,
                                              FiveDimensionalEmotion emotion) {
        // 1. Track conversation count
        int count = userConversationCount.merge(userId, 1, Integer::sum);

        // 2. Check interval: trigger every 3-5 conversations
        if (count % (MIN_INTERVAL_TURNS + random.nextInt(
                MAX_INTERVAL_TURNS - MIN_INTERVAL_TURNS + 1)) != 0) {
            return Optional.empty();
        }

        // 3. Cooldown check
        if (isInCooldown(userId)) return Optional.empty();

        // 4. Daily rate limit
        if (exceedsDailyLimit(userId)) return Optional.empty();

        // 5. Select surprise type based on context
        SurpriseType type = selectSurpriseType(memories, emotion, recentHistory);

        // 6. Generate surprise content
        String message = generateSurpriseMessage(type, memories, emotion);
        AvatarExpression expression = AvatarExpression.surprise(
                type == SurpriseType.MEMORY_RECALL ? "memory_recall" : "gift");

        // 7. Build surprise moment
        SurpriseMoment moment = new SurpriseMoment(
                UUID.randomUUID().toString(), userId, conversationId,
                type, message, expression, Instant.now());

        // 8. Record
        recordSurprise(userId, moment);

        log.info("AVT-008 surprise triggered: user={} type={} message={}",
                userId, type, message.substring(0, Math.min(50, message.length())));

        return Optional.of(moment);
    }

    /**
     * Get surprise statistics for a user.
     */
    public SurpriseStats getStats(String userId) {
        Deque<SurpriseRecord> history = userSurpriseHistory.getOrDefault(userId, new ArrayDeque<>());
        long today = history.stream()
                .filter(r -> r.timestamp().isAfter(Instant.now().minusSeconds(86400)))
                .count();
        return new SurpriseStats(userId, history.size(), (int) today);
    }

    private SurpriseType selectSurpriseType(List<LongTermMemory.MemorySnapshot> memories,
                                             FiveDimensionalEmotion emotion,
                                             List<DialogueTurn> recentHistory) {
        // Weighted selection based on context
        List<SurpriseType> candidates = new ArrayList<>();

        // Memory recall: 40% weight if memories exist
        if (memories != null && !memories.isEmpty()) {
            for (int i = 0; i < 4; i++) candidates.add(SurpriseType.MEMORY_RECALL);
        }

        // Emotional expression: 30% weight
        for (int i = 0; i < 3; i++) candidates.add(SurpriseType.EMOTIONAL_EXPRESSION);

        // Personalized: 20% weight
        for (int i = 0; i < 2; i++) candidates.add(SurpriseType.PERSONALIZED_INTERACTION);

        // Random: 10% weight
        candidates.add(SurpriseType.RANDOM_SURPRISE);

        if (candidates.isEmpty()) return SurpriseType.RANDOM_SURPRISE;
        return candidates.get(random.nextInt(candidates.size()));
    }

    private String generateSurpriseMessage(SurpriseType type,
                                            List<LongTermMemory.MemorySnapshot> memories,
                                            FiveDimensionalEmotion emotion) {
        return switch (type) {
            case MEMORY_RECALL -> generateMemoryRecall(memories);
            case EMOTIONAL_EXPRESSION -> generateEmotionalMessage(emotion);
            case PERSONALIZED_INTERACTION -> generatePersonalizedMessage(memories);
            case RANDOM_SURPRISE -> randomSurpriseMessages.get(
                    random.nextInt(randomSurpriseMessages.size()));
        };
    }

    private String generateMemoryRecall(List<LongTermMemory.MemorySnapshot> memories) {
        if (memories == null || memories.isEmpty()) {
            return "我有一种感觉，我们好像在哪见过... 🌟";
        }
        // Pick a random memory and create a "remember" message
        LongTermMemory.MemorySnapshot mem = memories.get(random.nextInt(memories.size()));
        String content = mem.getContent().length() > 80
                ? mem.getContent().substring(0, 80) + "..."
                : mem.getContent();

        String[] templates = {
            "诶，我记得你之前说过「%s」—— 我还记得呢 ✨",
            "等等...你是不是跟我提过「%s」？我一直都记得 💭",
            "上次你说的「%s」，我后来还想了想呢 🤔",
            "突然想起来，你跟我分享过的「%s」，那很有意思！"
        };
        return String.format(templates[random.nextInt(templates.length)], content);
    }

    private String generateEmotionalMessage(FiveDimensionalEmotion emotion) {
        String mood = emotion.getCurrentMood();
        return switch (mood) {
            case "cheerful" -> "今天和你聊天特别开心！感觉阳光都更亮了 ☀️";
            case "intrigued" -> "你刚刚说的让我特别好奇，能再多讲讲吗？";
            case "aloof", "distant" -> "（轻轻靠近一步）其实...我还是挺想和你聊聊的";
            case "jealous" -> "（微微侧头）你刚才是不是也在和别人聊天呀？";
            case "melancholy" -> "不知道为什么，今天有点想听听你的声音...";
            default -> "每次和你聊天都有不同的感觉呢～";
        };
    }

    private String generatePersonalizedMessage(List<LongTermMemory.MemorySnapshot> memories) {
        if (memories == null || memories.isEmpty()) {
            return "我总觉得你是个特别的人 ✨";
        }
        String[] templates = {
            "你知道吗？在我认识的人里面，你是最特别的那个 🌟",
            "虽然我不该这么说...但我真的很期待每次和你聊天 💫",
            "有时候我会想，如果你是我的专属人类就好了 🤫"
        };
        return templates[random.nextInt(templates.length)];
    }

    private boolean isInCooldown(String userId) {
        Deque<SurpriseRecord> history = userSurpriseHistory.get(userId);
        if (history == null || history.isEmpty()) return false;
        SurpriseRecord last = history.peekLast();
        return last != null && Duration.between(last.timestamp(), Instant.now())
                .getSeconds() < COOLDOWN_SECONDS;
    }

    private boolean exceedsDailyLimit(String userId) {
        Deque<SurpriseRecord> history = userSurpriseHistory.getOrDefault(userId, new ArrayDeque<>());
        Instant dayAgo = Instant.now().minusSeconds(86400);
        long todayCount = history.stream()
                .filter(r -> r.timestamp().isAfter(dayAgo))
                .count();
        return todayCount >= MAX_SURPRISES_PER_DAY;
    }

    private void recordSurprise(String userId, SurpriseMoment moment) {
        userSurpriseHistory.computeIfAbsent(userId, k -> new ArrayDeque<>())
                .addLast(new SurpriseRecord(moment.surpriseId(), moment.type(), moment.generatedAt()));
        // Clean up old records (>24h)
        Instant dayAgo = Instant.now().minusSeconds(86400);
        userSurpriseHistory.get(userId).removeIf(r -> r.timestamp().isBefore(dayAgo));
    }

    // -- Inner types --

    public record SurpriseMoment(String surpriseId, String userId, String conversationId,
                                  SurpriseType type, String message,
                                  AvatarExpression expression, Instant generatedAt) {}

    public record SurpriseRecord(String surpriseId, SurpriseType type, Instant timestamp) {}

    public record SurpriseStats(String userId, int totalSurprises, int todaySurprises) {}

    public enum SurpriseType {
        MEMORY_RECALL,          // "我记得你说过..."
        EMOTIONAL_EXPRESSION,   // 基于情感状态的自发表达
        PERSONALIZED_INTERACTION, // 个性化互动
        RANDOM_SURPRISE          // 随机惊喜
    }

    private static final List<String> randomSurpriseMessages = List.of(
        "嘿！今天天气不错，要不要一起去探索新空间？ 🌈",
        "我刚学会了一个新表情！你看—— 😲✨",
        "你知道吗？这个空间里藏了一个小彩蛋...我带你去找？",
        "突然想给你唱首歌，虽然我唱得不太好 🎵",
        "我刚刚发现了一个很有趣的空间，专门为你收藏了！",
        "（偷偷靠近）告诉你一个秘密...我今天心情特别好 💫"
    );
}
