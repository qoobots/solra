package com.solra.soc.application.dto;

import com.solra.soc.domain.model.SocialGesture;
import com.solra.soc.domain.service.SocialGestureService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * SOC-003 社交信号相关 DTO。
 */

/** 社交信号发送命令。 */
public class SendGestureCommand {
    private String sessionId;
    private String fromUserId;
    private String signal;          // GestureSignal name
    private String intensity;       // SignalIntensity name (optional)
    private String targetUserId;    // optional
    private String message;         // optional
    private int durationMs;

    public SendGestureCommand() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getSignal() { return signal; }
    public void setSignal(String signal) { this.signal = signal; }
    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getDurationMs() { return durationMs; }
    public void setDurationMs(int durationMs) { this.durationMs = durationMs; }
}

/** 社交信号 DTO。 */
public record GestureDTO(
        String gestureId,
        String sessionId,
        String fromUserId,
        String signal,
        String intensity,
        String targetUserId,
        String message,
        Instant createdAt,
        int durationMs,
        boolean acknowledged,
        boolean isDirected
) {
    public static GestureDTO from(SocialGesture g) {
        return new GestureDTO(
                g.getGestureId(),
                g.getSessionId(),
                g.getFromUserId(),
                g.getSignal().name(),
                g.getIntensity().name(),
                g.getTargetUserId(),
                g.getMessage(),
                g.getCreatedAt(),
                g.getDurationMs(),
                g.isAcknowledged(),
                g.isDirected()
        );
    }
}

/** 社交信号统计 DTO。 */
public record GestureStatsDTO(
        int totalGestures,
        Map<String, Long> signalCounts,
        Map<String, Long> userCounts,
        long directedCount,
        long acknowledgedCount,
        String topUser,
        String topSignal
) {
    public static GestureStatsDTO from(SocialGestureService.GestureStats stats) {
        Map<String, Long> signalCounts = stats.signalCounts().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue,
                        (a, b) -> a,
                        java.util.LinkedHashMap::new));
        return new GestureStatsDTO(
                stats.totalGestures(),
                signalCounts,
                stats.userCounts(),
                stats.directedCount(),
                stats.acknowledgedCount(),
                stats.topUser(),
                stats.topSignal()
        );
    }
}
