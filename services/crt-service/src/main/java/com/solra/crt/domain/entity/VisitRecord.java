package com.solra.crt.domain.entity;

import java.time.Instant;

/**
 * 空间访问记录值对象 (CRT-004)。
 * 单条访问记录，用于数据分析聚合。
 */
public class VisitRecord {

    private String visitId;
    private String spaceId;
    private String projectId;
    private String visitorId;      // 匿名或登录用户ID
    private String sessionId;      // 会话ID
    private Instant enteredAt;
    private Instant leftAt;
    private long durationMs;       // 停留时长（毫秒）
    private String entrySource;    // 进入来源：direct/search/share/recommend
    private String deviceType;     // 设备类型：desktop/mobile/tablet/vr
    private String region;         // 地区（如 CN/US/JP）
    private boolean bounced;       // 是否跳出（停留<5秒）

    public VisitRecord() {
        this.enteredAt = Instant.now();
        this.bounced = false;
    }

    public void markExit(Instant leftAt) {
        this.leftAt = leftAt;
        this.durationMs = leftAt.toEpochMilli() - enteredAt.toEpochMilli();
        this.bounced = durationMs < 5000;
    }

    // ── Getters and Setters ──

    public String getVisitId() { return visitId; }
    public void setVisitId(String visitId) { this.visitId = visitId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Instant getEnteredAt() { return enteredAt; }
    public void setEnteredAt(Instant enteredAt) { this.enteredAt = enteredAt; }
    public Instant getLeftAt() { return leftAt; }
    public void setLeftAt(Instant leftAt) { this.leftAt = leftAt; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getEntrySource() { return entrySource; }
    public void setEntrySource(String entrySource) { this.entrySource = entrySource; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public boolean isBounced() { return bounced; }
    public void setBounced(boolean bounced) { this.bounced = bounced; }
}
