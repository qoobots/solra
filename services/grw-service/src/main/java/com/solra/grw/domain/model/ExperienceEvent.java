package com.solra.grw.domain.model;

import java.time.Instant;

/**
 * ExperienceEvent — 经验事件实体。
 * 记录用户在平台上的每一次有意义的操作及其经验值变化。
 */
public class ExperienceEvent {

    private String eventId;
    private String userId;
    private String eventType;        // CONVERSATION, SPACE_EXPLORE, FRIEND_ADD, SHARE_CREATE, etc.
    private String spaceId;
    private int value;               // 经验值增量
    private String metadata;
    private Instant timestamp;

    public ExperienceEvent() {}

    public ExperienceEvent(String eventId, String userId, String eventType, int value) {
        this.eventId = eventId;
        this.userId = userId;
        this.eventType = eventType;
        this.value = value;
        this.timestamp = Instant.now();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
