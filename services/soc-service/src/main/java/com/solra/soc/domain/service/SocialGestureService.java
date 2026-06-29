package com.solra.soc.domain.service;

import com.solra.soc.domain.model.SocialGesture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SocialGestureService — SOC-003 空间社交信号领域服务。
 *
 * 管理会话内的社交信号生命周期：发送/接收/历史查询/统计。
 * 支持 8 种社交信号：举手、鼓掌、请求安静、点赞、跺脚、舞蹈、鞠躬、大笑。
 * 信号通过 SessionManager 广播给会话内的所有参与者。
 */
public class SocialGestureService {

    private static final Logger log = LoggerFactory.getLogger(SocialGestureService.class);

    /** 会话信号存储：sessionId -> List<SocialGesture> */
    private final Map<String, List<SocialGesture>> sessionGestures = new ConcurrentHashMap<>();

    /** 最大信号历史保留数 */
    private static final int MAX_GESTURES_PER_SESSION = 300;

    /** 冷却时间（ms）：防止同一用户短时间内刷屏 */
    private static final long COOLDOWN_MS = 500;

    /** 用户最近发送信号时间：userId -> lastSendTimeMs */
    private final Map<String, Long> userCooldowns = new ConcurrentHashMap<>();

    public SocialGestureService() {}

    /**
     * Send a social gesture to a session.
     * Enforces cooldown to prevent spam.
     */
    public SocialGesture sendGesture(SocialGesture gesture) {
        // Enforce cooldown
        long now = System.currentTimeMillis();
        Long lastSend = userCooldowns.get(gesture.getFromUserId());
        if (lastSend != null && (now - lastSend) < COOLDOWN_MS) {
            throw new IllegalStateException("Gesture cooldown active, please wait "
                    + (COOLDOWN_MS - (now - lastSend)) + "ms");
        }

        userCooldowns.put(gesture.getFromUserId(), now);
        storeGesture(gesture);

        log.info("SOC-003 gesture sent: session={} from={} signal={} intensity={} target={}",
                gesture.getSessionId(), gesture.getFromUserId(), gesture.getSignal(),
                gesture.getIntensity(),
                gesture.getTargetUserId() != null ? gesture.getTargetUserId() : "broadcast");
        return gesture;
    }

    /**
     * Send a raise-hand gesture.
     */
    public SocialGesture raiseHand(String sessionId, String fromUserId) {
        return sendGesture(SocialGesture.raiseHand(sessionId, fromUserId));
    }

    /**
     * Send an applause gesture.
     */
    public SocialGesture applaud(String sessionId, String fromUserId, SocialGesture.SignalIntensity intensity) {
        return sendGesture(SocialGesture.applaud(sessionId, fromUserId, intensity));
    }

    /**
     * Send a request-silence gesture.
     */
    public SocialGesture requestSilence(String sessionId, String fromUserId) {
        return sendGesture(SocialGesture.requestSilence(sessionId, fromUserId));
    }

    /**
     * Send a thumbs-up gesture (directed at a specific user).
     */
    public SocialGesture thumbsUp(String sessionId, String fromUserId, String targetUserId) {
        return sendGesture(SocialGesture.thumbsUp(sessionId, fromUserId, targetUserId));
    }

    /**
     * Send a stomp gesture for attention.
     */
    public SocialGesture stomp(String sessionId, String fromUserId, SocialGesture.SignalIntensity intensity) {
        return sendGesture(SocialGesture.stomp(sessionId, fromUserId, intensity));
    }

    /**
     * Send a dance gesture.
     */
    public SocialGesture dance(String sessionId, String fromUserId) {
        return sendGesture(SocialGesture.dance(sessionId, fromUserId));
    }

    /**
     * Send a bow gesture.
     */
    public SocialGesture bow(String sessionId, String fromUserId, String targetUserId) {
        return sendGesture(SocialGesture.bow(sessionId, fromUserId, targetUserId));
    }

    /**
     * Send a laugh gesture.
     */
    public SocialGesture laugh(String sessionId, String fromUserId, SocialGesture.SignalIntensity intensity) {
        return sendGesture(SocialGesture.laugh(sessionId, fromUserId, intensity));
    }

    /**
     * Acknowledge a gesture (mark as having received a response).
     */
    public void acknowledgeGesture(String sessionId, String gestureId) {
        List<SocialGesture> gestures = sessionGestures.get(sessionId);
        if (gestures != null) {
            gestures.stream()
                    .filter(g -> g.getGestureId().equals(gestureId))
                    .findFirst()
                    .ifPresent(g -> {
                        g.acknowledge();
                        log.info("SOC-003 gesture acknowledged: session={} gesture={}", sessionId, gestureId);
                    });
        }
    }

    /**
     * Get recent gestures for a session.
     */
    public List<SocialGesture> getRecentGestures(String sessionId, int limit) {
        List<SocialGesture> gestures = sessionGestures.getOrDefault(sessionId, List.of());
        int count = Math.min(limit, gestures.size());
        int start = Math.max(0, gestures.size() - count);
        return new ArrayList<>(gestures.subList(start, gestures.size()));
    }

    /**
     * Get gestures since a given gesture ID.
     */
    public List<SocialGesture> getGesturesSince(String sessionId, String sinceGestureId) {
        List<SocialGesture> gestures = sessionGestures.getOrDefault(sessionId, List.of());
        List<SocialGesture> result = new ArrayList<>();
        boolean found = (sinceGestureId == null);
        for (SocialGesture g : gestures) {
            if (found) {
                result.add(g);
            }
            if (g.getGestureId().equals(sinceGestureId)) {
                found = true;
            }
        }
        return result;
    }

    /**
     * Get gestures by a specific user in a session.
     */
    public List<SocialGesture> getGesturesByUser(String sessionId, String userId) {
        List<SocialGesture> gestures = sessionGestures.getOrDefault(sessionId, List.of());
        return gestures.stream()
                .filter(g -> g.getFromUserId().equals(userId))
                .toList();
    }

    /**
     * Get currently active gestures (with remaining duration > 0).
     */
    public List<SocialGesture> getActiveGestures(String sessionId) {
        List<SocialGesture> gestures = sessionGestures.getOrDefault(sessionId, List.of());
        return gestures.stream()
                .filter(SocialGesture::isActive)
                .toList();
    }

    /**
     * Get gesture statistics for a session.
     */
    public GestureStats getStats(String sessionId) {
        List<SocialGesture> gestures = sessionGestures.getOrDefault(sessionId, List.of());

        Map<SocialGesture.GestureSignal, Long> signalCounts = new LinkedHashMap<>();
        Map<String, Long> userCounts = new LinkedHashMap<>();
        long directedCount = 0;
        long acknowledgedCount = 0;

        for (SocialGesture g : gestures) {
            signalCounts.merge(g.getSignal(), 1L, Long::sum);
            userCounts.merge(g.getFromUserId(), 1L, Long::sum);
            if (g.isDirected()) directedCount++;
            if (g.isAcknowledged()) acknowledgedCount++;
        }

        String topUser = userCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String topSignal = signalCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().name())
                .orElse(null);

        return new GestureStats(gestures.size(), signalCounts, userCounts,
                directedCount, acknowledgedCount, topUser, topSignal);
    }

    /**
     * Clean up gestures for an ended session.
     */
    public void cleanup(String sessionId) {
        sessionGestures.remove(sessionId);
        log.info("SOC-003 gesture history cleaned for session: {}", sessionId);
    }

    private void storeGesture(SocialGesture gesture) {
        List<SocialGesture> gestures = sessionGestures.computeIfAbsent(
                gesture.getSessionId(), k -> new CopyOnWriteArrayList<>());
        gestures.add(gesture);

        // Trim old gestures if exceeding limit
        while (gestures.size() > MAX_GESTURES_PER_SESSION) {
            gestures.remove(0);
        }
    }

    // -- Inner types --

    public record GestureStats(
            int totalGestures,
            Map<SocialGesture.GestureSignal, Long> signalCounts,
            Map<String, Long> userCounts,
            long directedCount,
            long acknowledgedCount,
            String topUser,
            String topSignal) {}
}
