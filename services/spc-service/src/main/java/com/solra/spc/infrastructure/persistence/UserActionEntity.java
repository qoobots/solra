package com.solra.spc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "spc_user_actions")
public class UserActionEntity {
    @Id @Column(name = "action_id")
    private String actionId;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(name = "space_id", nullable = false)
    private String spaceId;
    @Column(name = "action_type")
    private String actionType;
    @Column(name = "dwell_duration_ms")
    private long dwellDurationMs;
    @Column(name = "action_time")
    private Instant actionTime;

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public long getDwellDurationMs() { return dwellDurationMs; }
    public void setDwellDurationMs(long dwellDurationMs) { this.dwellDurationMs = dwellDurationMs; }
    public Instant getActionTime() { return actionTime; }
    public void setActionTime(Instant actionTime) { this.actionTime = actionTime; }
}
