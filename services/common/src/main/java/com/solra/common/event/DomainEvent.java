package com.solra.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * DomainEvent — 领域事件基类。
 * 所有跨服务 Kafka 事件的基础结构，包含通用元数据。
 * INF-004: 消息队列事件驱动架构底座。
 */
public abstract class DomainEvent {

    private String eventId;
    private String eventType;
    private String sourceService;
    private Instant timestamp;
    private String correlationId;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    protected DomainEvent(String sourceService) {
        this();
        this.sourceService = sourceService;
    }

    /** 获取事件类型标识（格式: namespace.event_name） */
    public abstract String getEventType();

    /** 获取事件分区的 key（通常为 userId 或 spaceId） */
    public abstract String getPartitionKey();

    // ---- getters/setters ----

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public void setEventType(String eventType) { this.eventType = eventType; }
}
