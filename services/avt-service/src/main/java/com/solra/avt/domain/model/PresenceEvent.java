package com.solra.avt.domain.model;

import java.time.Instant;

/**
 * PresenceEvent 值对象 — 用户在空间中的在场事件。
 * AVT-002: 当用户进入空间时，虚拟人感知到用户在场并触发主动互动。
 */
public class PresenceEvent {

    private String userId;
    private String spaceId;
    private String avatarId;
    private PresenceEventType eventType;
    private Instant detectedAt;
    private UserContext userContext;

    public PresenceEvent() {
        this.detectedAt = Instant.now();
    }

    public PresenceEvent(String userId, String spaceId, String avatarId, PresenceEventType eventType) {
        this.userId = userId;
        this.spaceId = spaceId;
        this.avatarId = avatarId;
        this.eventType = eventType;
        this.detectedAt = Instant.now();
        this.userContext = new UserContext();
    }

    /** 事件类型 */
    public enum PresenceEventType {
        /** 用户进入空间 */
        USER_ENTERED,
        /** 用户靠近虚拟人（距离<3米） */
        USER_APPROACHED,
        /** 用户注视虚拟人（视线方向对准） */
        USER_GAZING,
        /** 用户在空间中停留超过阈值时间 */
        USER_LINGERING,
        /** 用户离开空间 */
        USER_LEFT
    }

    /**
     * UserContext — 用户在场时的上下文快照。
     */
    public static class UserContext {
        private String displayName;
        private boolean isNewUser;           // 是否首次进入空间
        private long timeSinceLastVisit;     // 距上次访问的秒数（-1=首次）
        private int totalVisits;             // 总访问次数
        private int friendsInSpace;          // 在空间中的好友数
        private boolean previousInteraction; // 是否与当前虚拟人互动过

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public boolean isNewUser() { return isNewUser; }
        public void setNewUser(boolean newUser) { isNewUser = newUser; }
        public long getTimeSinceLastVisit() { return timeSinceLastVisit; }
        public void setTimeSinceLastVisit(long timeSinceLastVisit) { this.timeSinceLastVisit = timeSinceLastVisit; }
        public int getTotalVisits() { return totalVisits; }
        public void setTotalVisits(int totalVisits) { this.totalVisits = totalVisits; }
        public int getFriendsInSpace() { return friendsInSpace; }
        public void setFriendsInSpace(int friendsInSpace) { this.friendsInSpace = friendsInSpace; }
        public boolean isPreviousInteraction() { return previousInteraction; }
        public void setPreviousInteraction(boolean previousInteraction) { this.previousInteraction = previousInteraction; }
    }

    /** 是否需要主动互动 */
    public boolean requiresProactiveAction() {
        return switch (eventType) {
            case USER_ENTERED -> shouldGreetOnEnter();
            case USER_APPROACHED -> true;
            case USER_GAZING -> shouldRespondToGaze();
            case USER_LINGERING -> true;
            case USER_LEFT -> false;
        };
    }

    private boolean shouldGreetOnEnter() {
        // 新用户或长时间未见的用户才触发主动招呼
        return userContext != null && (userContext.isNewUser || userContext.timeSinceLastVisit > 300);
    }

    private boolean shouldRespondToGaze() {
        // 注视超过2秒触发回应
        return detectedAt != null && Instant.now().isAfter(detectedAt.plusSeconds(2));
    }

    // ---- getters / setters ----
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getAvatarId() { return avatarId; }
    public void setAvatarId(String avatarId) { this.avatarId = avatarId; }
    public PresenceEventType getEventType() { return eventType; }
    public void setEventType(PresenceEventType eventType) { this.eventType = eventType; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public UserContext getUserContext() { return userContext; }
    public void setUserContext(UserContext userContext) { this.userContext = userContext; }
}
