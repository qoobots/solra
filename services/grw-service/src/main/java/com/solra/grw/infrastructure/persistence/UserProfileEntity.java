package com.solra.grw.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {
    @Id @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "presence_score")
    private double presenceScore;
    @Column(name = "faith_level", length = 20)
    private String faithLevel;
    @Column(name = "total_interactions")
    private int totalInteractions;
    @Column(name = "spaces_visited")
    private int spacesVisited;
    @Column(name = "conversations_had")
    private int conversationsHad;
    @Column(name = "friends_count")
    private int friendsCount;
    @Column(name = "created_at")
    private Instant createdAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Column(name = "last_active_at")
    private Instant lastActiveAt;
    @Column(name = "current_onboarding_step", length = 50)
    private String currentOnboardingStep;
    @Column(name = "onboarding_completed")
    private boolean onboardingCompleted;

    public UserProfileEntity() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public double getPresenceScore() { return presenceScore; }
    public void setPresenceScore(double presenceScore) { this.presenceScore = presenceScore; }
    public String getFaithLevel() { return faithLevel; }
    public void setFaithLevel(String faithLevel) { this.faithLevel = faithLevel; }
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
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
}
