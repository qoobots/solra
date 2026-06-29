package com.solra.spc.domain.model;

import java.time.Instant;

/**
 * UserAction 实体 — 用户对空间的行为事件（推荐算法输入）。
 */
public class UserAction {
    private String actionId;
    private String userId;
    private String spaceId;
    private UserActionType actionType;
    private long dwellDurationMs;
    private Instant actionTime;

    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public UserActionType getActionType() { return actionType; }
    public void setActionType(UserActionType actionType) { this.actionType = actionType; }
    public long getDwellDurationMs() { return dwellDurationMs; }
    public void setDwellDurationMs(long dwellDurationMs) { this.dwellDurationMs = dwellDurationMs; }
    public Instant getActionTime() { return actionTime; }
    public void setActionTime(Instant actionTime) { this.actionTime = actionTime; }
}
