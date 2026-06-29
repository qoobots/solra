package com.solra.not.domain.service;

import com.solra.not.domain.model.NotificationPreference;
import com.solra.not.domain.model.NotificationType;
import com.solra.not.domain.model.Platform;
import com.solra.not.domain.repository.DeviceRegistrationRepository;
import com.solra.not.domain.repository.NotificationPreferenceRepository;
import com.solra.not.domain.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SmartPushEngine — 智能推送策略引擎。
 * NOT-003: 智能推送策略（时机/频率/内容个性化）。
 *
 * 功能：
 * 1. 静默时段检测 — 在用户设置的免打扰时段内延迟发送
 * 2. 推送频率控制 — 基于通知类型的频率限制，防止推送疲劳
 * 3. 用户偏好过滤 — 根据用户偏好决定是否推送某类型通知
 * 4. 优先级感知 — URGENT 优先级绕过所有限制
 * 5. 渠道选择 — 根据偏好选择 PUSH / IN_APP 渠道
 */
@Service
public class SmartPushEngine {

    private static final Logger log = LoggerFactory.getLogger(SmartPushEngine.class);

    private final NotificationPreferenceRepository prefRepo;
    private final NotificationRepository notificationRepo;

    // 频率控制：每种通知类型每用户每天的推送上限
    private static final Map<NotificationType, Integer> DAILY_LIMITS = Map.of(
            NotificationType.INTERACTION, 5,
            NotificationType.SESSION_INVITE, 3,
            NotificationType.SESSION_JOINED, 5,
            NotificationType.FRIEND_REQUEST, 3,
            NotificationType.SPACE_INVITE, 3,
            NotificationType.FOLLOW, 5,
            NotificationType.PROJECT_PUBLISHED, 2,
            NotificationType.PROJECT_LIKED, 5,
            NotificationType.ASSET_UPLOADED, 2,
            NotificationType.SYSTEM_ANNOUNCEMENT, 1,
            NotificationType.SYSTEM_ALERT, 3,
            NotificationType.ACHIEVEMENT, 3,
            NotificationType.FAITH_LEVEL_UP, 2,
            NotificationType.REVIEW_RESULT, 2,
            NotificationType.PURCHASE_SUCCESS, 2,
            NotificationType.SUBSCRIPTION_EXPIRY, 2,
            NotificationType.GIFT_RECEIVED, 3
    );

    private static final int DEFAULT_DAILY_LIMIT = 3;

    // 用户最近推送计数（生产环境应使用 Redis）
    private final Map<String, Map<NotificationType, Integer>> dailyCounters = new ConcurrentHashMap<>();

    public SmartPushEngine(NotificationPreferenceRepository prefRepo,
                           NotificationRepository notificationRepo) {
        this.prefRepo = prefRepo;
        this.notificationRepo = notificationRepo;
    }

    /**
     * 推送决策结果。
     */
    public enum Decision {
        /** 允许推送 */
        ALLOW,
        /** 降级为应用内消息 */
        DOWNGRADE_TO_IN_APP,
        /** 延迟到非静默时段 */
        DEFER,
        /** 被用户偏好阻止 */
        BLOCKED_BY_PREFERENCE,
        /** 被频率限制阻止 */
        BLOCKED_BY_RATE_LIMIT
    }

    /**
     * 智能推送决策记录。
     */
    public record PushDecision(Decision decision, String reason, boolean shouldSendPush,
                                boolean shouldSendInApp) {
        public static PushDecision allow() {
            return new PushDecision(Decision.ALLOW, "Allowed", true, false);
        }

        public static PushDecision allowWithInApp() {
            return new PushDecision(Decision.ALLOW, "Allowed", true, true);
        }

        public static PushDecision downgrade(String reason) {
            return new PushDecision(Decision.DOWNGRADE_TO_IN_APP, reason, false, true);
        }

        public static PushDecision defer(String reason) {
            return new PushDecision(Decision.DEFER, reason, false, false);
        }

        public static PushDecision blockedByPreference(String reason) {
            return new PushDecision(Decision.BLOCKED_BY_PREFERENCE, reason, false, false);
        }

        public static PushDecision blockedByRateLimit(String reason) {
            return new PushDecision(Decision.BLOCKED_BY_RATE_LIMIT, reason, false, false);
        }
    }

    /**
     * 评估是否应该向用户推送该类型的通知。
     * 综合检查静默时段、频率限制、用户偏好。
     *
     * @param userId 用户ID
     * @param type   通知类型
     * @param isUrgent 是否紧急（URGENT 优先级绕过所有限制）
     * @return 推送决策
     */
    public PushDecision evaluate(String userId, NotificationType type, boolean isUrgent) {
        // URGENT 优先级绕过所有限制
        if (isUrgent) {
            return PushDecision.allow();
        }

        // 1. 检查用户偏好
        Optional<NotificationPreference> pref = prefRepo.findByUserIdAndType(userId, type.name());
        if (pref.isPresent() && !pref.get().isEnabled()) {
            log.debug("User {} has disabled {} notifications", userId, type);
            return PushDecision.blockedByPreference("User disabled " + type + " notifications");
        }

        // 2. 检查静默时段
        if (isInQuietHours(userId)) {
            log.debug("User {} is in quiet hours", userId);
            return PushDecision.defer("User is in quiet hours");
        }

        // 3. 检查频率限制
        if (isRateLimited(userId, type)) {
            log.debug("User {} reached daily limit for {}", userId, type);
            return PushDecision.blockedByRateLimit(
                    "Daily limit reached for " + type + " (" + getDailyLimit(type) + ")");
        }

        // 4. 根据偏好决定渠道
        if (pref.isPresent()) {
            String channel = pref.get().getChannel().name();
            if ("IN_APP".equalsIgnoreCase(channel)) {
                return PushDecision.downgrade("User prefers in-app for " + type);
            }
        }

        // 允许推送并记录
        recordPush(userId, type);
        return PushDecision.allow();
    }

    /**
     * 检查用户是否处于静默时段。
     * 从 NotificationPreference 中读取 quietHoursStart/End。
     */
    public boolean isInQuietHours(String userId) {
        // 获取用户所有偏好中的静默时段设置
        List<NotificationPreference> prefs = prefRepo.findByUserId(userId);
        for (NotificationPreference pref : prefs) {
            String start = pref.getQuietHoursStart();
            String end = pref.getQuietHoursEnd();
            if (start != null && end != null) {
                LocalTime now = LocalTime.now();
                LocalTime quietStart = LocalTime.parse(start);
                LocalTime quietEnd = LocalTime.parse(end);

                if (quietStart.isBefore(quietEnd)) {
                    // 同一自然日，如 22:00-08:00
                    if (now.isAfter(quietStart) && now.isBefore(quietEnd)) {
                        return true;
                    }
                } else {
                    // 跨自然日，如 22:00-08:00
                    if (now.isAfter(quietStart) || now.isBefore(quietEnd)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查该用户该类型通知是否已达频率限制。
     */
    private boolean isRateLimited(String userId, NotificationType type) {
        int limit = getDailyLimit(type);
        Map<NotificationType, Integer> userCounters = dailyCounters.get(userId);
        if (userCounters == null) {
            return false;
        }
        int count = userCounters.getOrDefault(type, 0);
        return count >= limit;
    }

    /**
     * 记录一次推送。
     */
    private void recordPush(String userId, NotificationType type) {
        dailyCounters.computeIfAbsent(userId, k -> new ConcurrentHashMap<>())
                .merge(type, 1, Integer::sum);
    }

    /**
     * 获取某通知类型的每日推送上限。
     */
    public int getDailyLimit(NotificationType type) {
        return DAILY_LIMITS.getOrDefault(type, DEFAULT_DAILY_LIMIT);
    }

    /**
     * 获取用户当日各类型的推送计数。
     */
    public Map<NotificationType, Integer> getUserDailyStats(String userId) {
        return dailyCounters.getOrDefault(userId, Collections.emptyMap());
    }

    /**
     * 重置用户的每日推送计数（定时任务调用）。
     */
    public void resetDailyCounters(String userId) {
        dailyCounters.remove(userId);
        log.debug("Reset daily counters for user {}", userId);
    }

    /**
     * 获取用户过去N小时内某类型的推送次数。
     */
    public int getRecentPushCount(String userId, NotificationType type, int hours) {
        // 从仓储查询实际发送记录
        return (int) notificationRepo.findUnreadByUserId(userId).stream()
                .filter(n -> n.getType() == type)
                .count();
    }
}
