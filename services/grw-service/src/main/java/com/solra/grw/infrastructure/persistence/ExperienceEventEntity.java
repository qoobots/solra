package com.solra.grw.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "experience_events")
public class ExperienceEventEntity {
    @Id @Column(name = "event_id", length = 64)
    private String eventId;
    @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "event_type", length = 50)
    private String eventType;
    @Column(name = "space_id", length = 64)
    private String spaceId;
    @Column(name = "value")
    private int value;
    @Column(name = "metadata", length = 512)
    private String metadata;
    @Column(name = "timestamp")
    private Instant timestamp;

    public ExperienceEventEntity() {}

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
