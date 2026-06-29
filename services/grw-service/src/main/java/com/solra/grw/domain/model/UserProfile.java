package com.solra.grw.domain.model;

import java.time.Instant;

/**
 * UserProfile 聚合根 — 用户画像。
 * 管理用户在成长体系中的核心状态，包括信誉等级、活跃度分数和引导进度。
 */
public class UserProfile {

    private String userId;
    private double presenceScore;
    private FaithLevel faithLevel;
    private int totalInteractions;
    private int spacesVisited;
    private int conversationsHad;
    private int friendsCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastActiveAt;
    private String currentOnboardingStep;
    private boolean onboardingCompleted;

    // ---- constructors ----
    public UserProfile() {}

    public UserProfile(String userId) {
        this.userId = userId;
        this.presenceScore = 0.0;
        this.faithLevel = FaithLevel.SEEKER;
        this.totalInteractions = 0;
        this.spacesVisited = 0;
        this.conversationsHad = 0;
        this.friendsCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.lastActiveAt = this.createdAt;
        this.currentOnboardingStep = null;
        this.onboardingCompleted = false;
    }

    // ---- business methods ----

    /**
     * 增加一次互动计数并更新活跃时间。
     */
    public void recordInteraction() {
        this.totalInteractions++;
        this.updatedAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    /**
     * 更新信誉等级。
     *
     * @param newLevel 新的信誉等级
     * @return 如果等级发生变化返回 true，否则返回 false
     */
    public boolean updateFaithLevel(FaithLevel newLevel) {
        if (newLevel != this.faithLevel) {
            this.faithLevel = newLevel;
            this.updatedAt = Instant.now();
            return true;
        }
        return false;
    }

    /**
     * 更新活跃度分数。
     *
     * @param delta 分数增量
     */
    public void adjustPresenceScore(double delta) {
        this.presenceScore = Math.max(0.0, this.presenceScore + delta);
        this.updatedAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    /**
     * 增加访问空间计数。
     */
    public void incrementSpacesVisited() {
        this.spacesVisited++;
        this.updatedAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    /**
     * 增加对话计数。
     */
    public void incrementConversations() {
        this.conversationsHad++;
        this.updatedAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    /**
     * 增加好友计数。
     */
    public void incrementFriends() {
        this.friendsCount++;
        this.updatedAt = Instant.now();
        this.lastActiveAt = Instant.now();
    }

    /**
     * 更新引导步骤。
     *
     * @param step 当前引导步骤标识
     */
    public void advanceOnboardingStep(String step) {
        this.currentOnboardingStep = step;
        this.updatedAt = Instant.now();
    }

    /**
     * 标记引导完成。
     */
    public void completeOnboarding() {
        this.onboardingCompleted = true;
        this.currentOnboardingStep = null;
        this.updatedAt = Instant.now();
    }

    /**
     * 判断用户是否已经过引导流程。
     */
    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }

    // ---- getters / setters ----

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getPresenceScore() { return presenceScore; }
    public void setPresenceScore(double presenceScore) { this.presenceScore = presenceScore; }

    public FaithLevel getFaithLevel() { return faithLevel; }
    public void setFaithLevel(FaithLevel faithLevel) { this.faithLevel = faithLevel; }

    public int getTotalInteractions() { return totalInteractions; }
    public void setTotalInteractions(int totalInteractions) { this.totalInteractions = totalInteractions; }

    public int getSpacesVisited() { return spacesVisited; }
    public void setSpacesVisited(int spacesVisited) { this.spacesVisited = spacesVisited; }

    public int getConversationsHad() { return conversationsHad; }
    public void setConversationsHad(int conversationsHad) { this.conversationsHad = conversationsHad; }

    public int getFriendsCount() { return friendsCount; }
    public void setFriendsCount(int friendsCount) { this.friendsCount = friendsCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Instant lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public String getCurrentOnboardingStep() { return currentOnboardingStep; }
    public void setCurrentOnboardingStep(String currentOnboardingStep) { this.currentOnboardingStep = currentOnboardingStep; }

    public boolean isOnboardingCompletedFlag() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
}
