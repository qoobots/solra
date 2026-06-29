package com.solra.mon.application.dto;

import com.solra.mon.domain.entity.Subscription;

/**
 * 订阅 DTO。
 */
public class SubscriptionDTO {

    private String subscriptionId;
    private String userId;
    private String tier;
    private String status;
    private String billingCycle;
    private long startAt;
    private long expireAt;
    private boolean autoRenew;
    private String originalOrderId;
    private long createdAt;
    private long updatedAt;

    public static SubscriptionDTO from(Subscription sub) {
        SubscriptionDTO dto = new SubscriptionDTO();
        dto.subscriptionId = sub.getSubscriptionId();
        dto.userId = sub.getUserId();
        dto.tier = sub.getTier().name();
        dto.status = sub.getStatus().name();
        dto.billingCycle = sub.getBillingCycle() != null ? sub.getBillingCycle().name() : null;
        dto.startAt = sub.getStartAt();
        dto.expireAt = sub.getExpireAt();
        dto.autoRenew = sub.isAutoRenew();
        dto.originalOrderId = sub.getOriginalOrderId();
        dto.createdAt = sub.getCreatedAt();
        dto.updatedAt = sub.getUpdatedAt();
        return dto;
    }

    public String getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(String subscriptionId) { this.subscriptionId = subscriptionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }
    public long getStartAt() { return startAt; }
    public long getExpireAt() { return expireAt; }
    public boolean isAutoRenew() { return autoRenew; }
    public String getOriginalOrderId() { return originalOrderId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
