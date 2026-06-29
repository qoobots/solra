package com.solra.mon.domain.entity;

import java.util.UUID;

/**
 * 订阅领域实体。
 * 管理用户的会员订阅生命周期。
 */
public class Subscription {

    public enum SubscriptionTier {
        FREE, PREMIUM, CREATOR, ENTERPRISE
    }

    public enum SubscriptionStatus {
        ACTIVE, EXPIRED, CANCELLED, GRACE_PERIOD
    }

    private String subscriptionId;
    private String userId;
    private SubscriptionTier tier;
    private SubscriptionStatus status;
    private long startAt;
    private long expireAt;
    private boolean autoRenew;
    private String originalOrderId;
    private long createdAt;
    private long updatedAt;

    public Subscription(String userId, SubscriptionTier tier, long expireAt, boolean autoRenew) {
        this.subscriptionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.tier = tier;
        this.status = SubscriptionStatus.ACTIVE;
        this.startAt = System.currentTimeMillis();
        this.expireAt = expireAt;
        this.autoRenew = autoRenew;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.GRACE_PERIOD;
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.autoRenew = false;
        this.updatedAt = System.currentTimeMillis();
    }

    public void expire() {
        if (status == SubscriptionStatus.ACTIVE) {
            this.status = SubscriptionStatus.EXPIRED;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void enterGracePeriod() {
        if (status == SubscriptionStatus.ACTIVE) {
            this.status = SubscriptionStatus.GRACE_PERIOD;
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void setOriginalOrderId(String orderId) {
        this.originalOrderId = orderId;
    }

    // Getters
    public String getSubscriptionId() { return subscriptionId; }
    public String getUserId() { return userId; }
    public SubscriptionTier getTier() { return tier; }
    public SubscriptionStatus getStatus() { return status; }
    public long getStartAt() { return startAt; }
    public long getExpireAt() { return expireAt; }
    public boolean isAutoRenew() { return autoRenew; }
    public String getOriginalOrderId() { return originalOrderId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
