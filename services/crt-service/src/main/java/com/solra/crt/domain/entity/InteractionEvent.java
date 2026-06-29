package com.solra.crt.domain.entity;

import java.time.Instant;

/**
 * 交互事件值对象 (CRT-004)。
 * 记录用户在空间内的交互行为，用于热力图生成。
 */
public class InteractionEvent {

    public enum EventType {
        CLICK, HOVER, DRAG, ZOOM, ROTATE, ASSET_INTERACT,
        NAVIGATE, CHAT, EMOTE, PURCHASE
    }

    private String eventId;
    private String visitId;
    private String spaceId;
    private String projectId;
    private String visitorId;
    private EventType eventType;
    private String targetNodeId;   // 交互目标节点
    private float positionX;
    private float positionY;
    private float positionZ;
    private Instant timestamp;
    private String metadata;       // JSON 扩展数据

    public InteractionEvent() {
        this.timestamp = Instant.now();
    }

    // ── Getters and Setters ──

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getVisitId() { return visitId; }
    public void setVisitId(String visitId) { this.visitId = visitId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public String getTargetNodeId() { return targetNodeId; }
    public void setTargetNodeId(String targetNodeId) { this.targetNodeId = targetNodeId; }
    public float getPositionX() { return positionX; }
    public void setPositionX(float positionX) { this.positionX = positionX; }
    public float getPositionY() { return positionY; }
    public void setPositionY(float positionY) { this.positionY = positionY; }
    public float getPositionZ() { return positionZ; }
    public void setPositionZ(float positionZ) { this.positionZ = positionZ; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
