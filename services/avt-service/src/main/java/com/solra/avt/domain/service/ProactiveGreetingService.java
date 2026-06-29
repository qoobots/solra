package com.solra.avt.domain.service;

import com.solra.avt.domain.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * ProactiveGreetingService — AVT-002 虚拟人社交主动性领域服务。
 * 根据用户的在场事件，生成虚拟人主动互动行为。
 *
 * <p>设计目标：
 * <ul>
 *   <li>≤3秒内主动注视+打招呼</li>
 *   <li>根据用户类型（新用户/回访/好友）选择不同策略</li>
 *   <li>热度控制：避免过于频繁的打扰</li>
 * </ul>
 */
public class ProactiveGreetingService {

    private static final Logger log = LoggerFactory.getLogger(ProactiveGreetingService.class);

    /** 全局冷却时间：同一用户对同一虚拟人的最小互动间隔（秒） */
    private static final long COOLDOWN_SECONDS = 120;

    /** 每个用户每小时最大主动互动次数 */
    private static final int MAX_PROACTIVE_PER_HOUR = 5;

    /** 主动互动频率控制 */
    private final Map<String, Deque<Instant>> recentGreetings = new HashMap<>();
    private final Map<String, Instant> lastGreetingTime = new HashMap<>();

    /**
     * 处理在场事件，决定是否触发主动互动。
     *
     * @param event 在场事件
     * @param avatar 虚拟人实体
     * @return Optional of ProactiveAction，如果不需要互动则返回 empty
     */
    public Optional<ProactiveAction> handlePresenceEvent(PresenceEvent event, Avatar avatar) {
        if (event == null || avatar == null) return Optional.empty();

        // 1. 事件是否需要主动互动
        if (!event.requiresProactiveAction()) return Optional.empty();

        // 2. 冷却检查
        if (isInCooldown(event.getUserId(), avatar.getAvatarId())) {
            log.debug("AVT-002 cooldown: user={} avatar={}", event.getUserId(), avatar.getAvatarId());
            return Optional.empty();
        }

        // 3. 频率限制
        if (exceedsRateLimit(event.getUserId())) {
            log.debug("AVT-002 rate limit exceeded: user={}", event.getUserId());
            return Optional.empty();
        }

        // 4. 选择招呼策略
        GreetingTrigger trigger = GreetingTrigger.select(event);

        // 5. 构建个性化消息
        String message = personalizeMessage(trigger, event, avatar);

        // 6. 构建主动互动
        ProactiveAction action = ProactiveAction.builder()
                .actionId(UUID.randomUUID().toString())
                .trigger(trigger)
                .message(message)
                .enthusiasm(trigger.getEnthusiasm())
                .avatarId(avatar.getAvatarId())
                .avatarName(avatar.getDisplayName())
                .userId(event.getUserId())
                .spaceId(event.getSpaceId())
                .requiresResponse(trigger.getEnthusiasm() >= 0.6f)
                .metadata(buildMetadata(event, trigger))
                .suggestedAnimation(suggestAnimation(trigger))
                .generatedAt(Instant.now())
                .build();

        // 7. 记录冷却
        recordGreeting(event.getUserId(), avatar.getAvatarId());

        log.info("AVT-002 proactive greeting: user={} avatar={} trigger={} enthusiasm={}",
                event.getUserId(), avatar.getAvatarId(), trigger.getCode(), trigger.getEnthusiasm());

        return Optional.of(action);
    }

    /**
     * 生成虚拟人进入空间时的欢迎消息（批量）
     */
    public List<ProactiveAction> onUsersEntered(List<PresenceEvent> events, List<Avatar> avatars) {
        List<ProactiveAction> actions = new ArrayList<>();
        Map<String, Avatar> avatarMap = new HashMap<>();
        for (Avatar a : avatars) avatarMap.put(a.getAvatarId(), a);

        for (PresenceEvent event : events) {
            Avatar avatar = avatarMap.get(event.getAvatarId());
            if (avatar != null) {
                handlePresenceEvent(event, avatar).ifPresent(actions::add);
            }
        }
        return actions;
    }

    // ---- private helpers ----

    private boolean isInCooldown(String userId, String avatarId) {
        String key = userId + "::" + avatarId;
        Instant last = lastGreetingTime.get(key);
        if (last == null) return false;
        return Duration.between(last, Instant.now()).getSeconds() < COOLDOWN_SECONDS;
    }

    private boolean exceedsRateLimit(String userId) {
        Deque<Instant> history = recentGreetings.computeIfAbsent(userId, k -> new ArrayDeque<>());
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        // 清理过期记录
        while (!history.isEmpty() && history.peekFirst().isBefore(oneHourAgo)) {
            history.pollFirst();
        }
        return history.size() >= MAX_PROACTIVE_PER_HOUR;
    }

    private void recordGreeting(String userId, String avatarId) {
        String key = userId + "::" + avatarId;
        lastGreetingTime.put(key, Instant.now());
        recentGreetings.computeIfAbsent(userId, k -> new ArrayDeque<>()).addLast(Instant.now());
    }

    private String personalizeMessage(GreetingTrigger trigger, PresenceEvent event, Avatar avatar) {
        String template = trigger.getTemplate();
        String message = template
                .replace("{avatar_name}", avatar.getDisplayName());

        if (event.getUserContext() != null) {
            String displayName = event.getUserContext().getDisplayName();
            message = message.replace("{user_name}",
                    displayName != null ? displayName : "朋友");
        }
        message = message.replace("{user_name}", "朋友");
        message = message.replace("{friend_name}", "好友");
        message = message.replace("{feature_hint}", "一起探索新空间");

        return message;
    }

    private Map<String, String> buildMetadata(PresenceEvent event, GreetingTrigger trigger) {
        Map<String, String> meta = new HashMap<>();
        meta.put("trigger", trigger.getCode());
        meta.put("eventType", event.getEventType().name());
        meta.put("enthusiasm", String.valueOf(trigger.getEnthusiasm()));
        if (event.getUserContext() != null) {
            meta.put("isNewUser", String.valueOf(event.getUserContext().isNewUser()));
            meta.put("totalVisits", String.valueOf(event.getUserContext().getTotalVisits()));
        }
        return meta;
    }

    private String suggestAnimation(GreetingTrigger trigger) {
        return switch (trigger) {
            case FIRST_TIME_WELCOME -> "wave_enthusiastic";
            case RETURNING_GREETING -> "wave_friendly";
            case APPROACH_REACTION -> "nod_smile";
            case GAZE_RESPONSE -> "head_tilt";
            case LINGERING_PROMPT -> "gesture_invite";
            case FRIEND_PRESENCE -> "point_excited";
            case ENGAGED_USER_PROMPT -> "gesture_suggest";
        };
    }
}
