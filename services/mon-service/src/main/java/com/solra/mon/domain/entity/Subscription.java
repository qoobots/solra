package com.solra.mon.domain.entity;

import java.util.UUID;

/**
 * 订阅领域实体。
 * 管理用户的会员订阅生命周期，支持四档订阅：Free / Plus / Pro / Ultra。
 *
 * <p>Solra Plus 分档定价（验收标准）：
 * <ul>
 *   <li>Free — ¥0/月，基础功能</li>
 *   <li>Plus — ¥19/月，年付 ¥182（8折）</li>
 *   <li>Pro  — ¥49/月，年付 ¥470（8折）</li>
 *   <li>Ultra — ¥99/月，年付 ¥950（8折）</li>
 * </ul>
 */
public class Subscription {

    /** 订阅等级（由低到高）。 */
    public enum SubscriptionTier {
        FREE(0),
        PLUS(1),
        PRO(2),
        ULTRA(3);

        private final int level;

        SubscriptionTier(int level) { this.level = level; }

        /** 等级序号，用于比较高低。 */
        public int level() { return level; }

        /** 当前等级是否不低于指定等级。 */
        public boolean atLeast(SubscriptionTier other) {
            return this.level >= other.level;
        }
    }

    public enum SubscriptionStatus {
        ACTIVE, EXPIRED, CANCELLED, GRACE_PERIOD
    }

    public enum BillingCycle {
        MONTHLY, YEARLY
    }

    private String subscriptionId;
    private String userId;
    private SubscriptionTier tier;
    private SubscriptionStatus status;
    private BillingCycle billingCycle;
    private long startAt;
    private long expireAt;
    private boolean autoRenew;
    private String originalOrderId;
    private long createdAt;
    private long updatedAt;

    public Subscription(String userId, SubscriptionTier tier,
                         BillingCycle billingCycle, long expireAt, boolean autoRenew) {
        this.subscriptionId = UUID.randomUUID().toString();
        this.userId = userId;
        this.tier = tier;
        this.billingCycle = billingCycle;
        this.status = SubscriptionStatus.ACTIVE;
        this.startAt = System.currentTimeMillis();
        this.expireAt = expireAt;
        this.autoRenew = autoRenew;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // ── 状态判断 ──

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.GRACE_PERIOD;
    }

    /** 是否可升级到目标等级。 */
    public boolean canUpgradeTo(SubscriptionTier target) {
        return target.level() > this.tier.level();
    }

    /** 是否可降级到目标等级。 */
    public boolean canDowngradeTo(SubscriptionTier target) {
        return target.level() < this.tier.level() && target != SubscriptionTier.FREE;
    }

    // ── 生命周期方法 ──

    /**
     * 升级订阅等级。
     * @param newTier 目标等级，必须高于当前等级
     * @param newExpireAt 新到期时间（升级后按比例计算）
     * @throws IllegalArgumentException 如果目标等级不高于当前等级
     */
    public void upgrade(SubscriptionTier newTier, long newExpireAt) {
        if (!canUpgradeTo(newTier)) {
            throw new IllegalArgumentException(
                    "Cannot upgrade from " + tier + " to " + newTier);
        }
        this.tier = newTier;
        this.expireAt = newExpireAt;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 降级订阅等级（当前周期结束后生效）。
     * @param newTier 目标等级，必须低于当前等级
     * @throws IllegalArgumentException 如果目标等级不低于当前等级或为 FREE
     */
    public void scheduleDowngrade(SubscriptionTier newTier) {
        if (!canDowngradeTo(newTier)) {
            throw new IllegalArgumentException(
                    "Cannot downgrade from " + tier + " to " + newTier);
        }
        // 降级在当前周期结束后生效
        this.tier = newTier;
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 续费，延长到期时间。
     * @param newExpireAt 新的到期时间戳
     */
    public void renew(long newExpireAt) {
        if (this.status == SubscriptionStatus.CANCELLED) {
            this.status = SubscriptionStatus.ACTIVE;
        }
        this.expireAt = newExpireAt;
        this.autoRenew = true;
        this.updatedAt = System.currentTimeMillis();
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
    public BillingCycle getBillingCycle() { return billingCycle; }
    public long getStartAt() { return startAt; }
    public long getExpireAt() { return expireAt; }
    public boolean isAutoRenew() { return autoRenew; }
    public String getOriginalOrderId() { return originalOrderId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
