package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * SocialGesture — SOC-003 空间社交信号实体。
 *
 * 独立管理空间内用户的社交信号：举手/鼓掌/安静/点赞/跺脚/舞蹈等。
 * 与 AvatarSyncState.GestureState 不同，SocialGesture 是面向社交互动
 * 的高层信号，支持信号类型、强度、目标受众和持续时间等语义。
 */
public class SocialGesture {

    private String gestureId;
    private String sessionId;
    private String fromUserId;
    private GestureSignal signal;
    private SignalIntensity intensity;
    private String targetUserId;          // null = broadcast to all in session
    private String message;               // optional text accompanying gesture
    private Instant createdAt;
    private int durationMs;               // how long the gesture lasts (0 = instantaneous)
    private boolean acknowledged;         // whether at least one participant responded

    private SocialGesture() {}

    /**
     * Create a social gesture signal.
     */
    public static SocialGesture create(String sessionId, String fromUserId,
                                        GestureSignal signal, SignalIntensity intensity,
                                        String targetUserId, String message, int durationMs) {
        SocialGesture g = new SocialGesture();
        g.gestureId = UUID.randomUUID().toString();
        g.sessionId = sessionId;
        g.fromUserId = fromUserId;
        g.signal = signal;
        g.intensity = intensity != null ? intensity : SignalIntensity.NORMAL;
        g.targetUserId = targetUserId;
        g.message = message;
        g.createdAt = Instant.now();
        g.durationMs = Math.max(0, durationMs);
        g.acknowledged = false;
        return g;
    }

    /**
     * Factory: hand raise gesture.
     */
    public static SocialGesture raiseHand(String sessionId, String fromUserId) {
        return create(sessionId, fromUserId, GestureSignal.RAISE_HAND,
                SignalIntensity.NORMAL, null, null, 0);
    }

    /**
     * Factory: applause/clap gesture.
     */
    public static SocialGesture applaud(String sessionId, String fromUserId, SignalIntensity intensity) {
        return create(sessionId, fromUserId, GestureSignal.APPLAUD,
                intensity, null, null, 3000);
    }

    /**
     * Factory: quiet/shh gesture.
     */
    public static SocialGesture requestSilence(String sessionId, String fromUserId) {
        return create(sessionId, fromUserId, GestureSignal.REQUEST_SILENCE,
                SignalIntensity.NORMAL, null, "请大家安静一下", 0);
    }

    /**
     * Factory: thumbs up gesture.
     */
    public static SocialGesture thumbsUp(String sessionId, String fromUserId, String targetUserId) {
        return create(sessionId, fromUserId, GestureSignal.THUMBS_UP,
                SignalIntensity.NORMAL, targetUserId, null, 2000);
    }

    /**
     * Factory: stomp/attention gesture.
     */
    public static SocialGesture stomp(String sessionId, String fromUserId, SignalIntensity intensity) {
        return create(sessionId, fromUserId, GestureSignal.STOMP,
                intensity, null, null, 1500);
    }

    /**
     * Factory: dance gesture.
     */
    public static SocialGesture dance(String sessionId, String fromUserId) {
        return create(sessionId, fromUserId, GestureSignal.DANCE,
                SignalIntensity.HIGH, null, null, 5000);
    }

    /**
     * Factory: bow gesture.
     */
    public static SocialGesture bow(String sessionId, String fromUserId, String targetUserId) {
        return create(sessionId, fromUserId, GestureSignal.BOW,
                SignalIntensity.NORMAL, targetUserId, null, 2000);
    }

    /**
     * Factory: laugh gesture.
     */
    public static SocialGesture laugh(String sessionId, String fromUserId, SignalIntensity intensity) {
        return create(sessionId, fromUserId, GestureSignal.LAUGH,
                intensity, null, null, 3000);
    }

    /**
     * Mark gesture as acknowledged by at least one other participant.
     */
    public void acknowledge() {
        this.acknowledged = true;
    }

    /** Whether this gesture is targeted at a specific user. */
    public boolean isDirected() {
        return targetUserId != null && !targetUserId.isBlank();
    }

    /** Whether this gesture is instantaneous (no duration). */
    public boolean isInstantaneous() {
        return durationMs <= 0;
    }

    /** Whether this gesture is still active based on elapsed time. */
    public boolean isActive() {
        if (isInstantaneous()) return false;
        long elapsed = Instant.now().toEpochMilli() - createdAt.toEpochMilli();
        return elapsed < durationMs;
    }

    // -- Getters/Setters --

    public String getGestureId() { return gestureId; }
    public void setGestureId(String gestureId) { this.gestureId = gestureId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public GestureSignal getSignal() { return signal; }
    public void setSignal(GestureSignal signal) { this.signal = signal; }
    public SignalIntensity getIntensity() { return intensity; }
    public void setIntensity(SignalIntensity intensity) { this.intensity = intensity; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    // -- Enums --

    /** 8 social gesture signal types (≥5 required). */
    public enum GestureSignal {
        RAISE_HAND,         // 举手
        APPLAUD,            // 鼓掌
        REQUEST_SILENCE,    // 请求安静
        THUMBS_UP,          // 点赞
        STOMP,              // 跺脚（吸引注意）
        DANCE,              // 舞蹈
        BOW,                // 鞠躬（致意/感谢）
        LAUGH               // 大笑
    }

    /** Gesture intensity level. */
    public enum SignalIntensity {
        SUBTLE,     // 轻微
        NORMAL,     // 正常
        HIGH,       // 强烈
        OVERWHELMING // 极强
    }
}
