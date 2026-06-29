package com.solra.grw.application.dto;

/** 记录经验命令 */
public class RecordExperienceCommand {
    private String userId;
    private String eventType;
    private int value;
    private String spaceId;
    private String metadata;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

/** 检测决定性时刻命令 */
public record DetectMomentsCommand(String userId, java.util.List<String> recentActions,
                                    java.util.Map<String, Object> currentState) {}
