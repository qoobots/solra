package com.solra.mon.domain.entity;

import java.util.List;

/**
 * 订阅计划值对象。
 * 定义每档订阅的定价、权益和升级规则。
 *
 * <p>Solra Plus 分档定价：
 * <ul>
 *   <li>Free  — ¥0/月</li>
 *   <li>Plus  — ¥19/月，年付 ¥182（8折）</li>
 *   <li>Pro   — ¥49/月，年付 ¥470（8折）</li>
 *   <li>Ultra — ¥99/月，年付 ¥950（8折）</li>
 * </ul>
 */
public class SubscriptionPlan {

    private final Subscription.SubscriptionTier tier;
    private final String name;
    private final String description;
    /** 月付价格（单位：分）。 */
    private final long priceMonthly;
    /** 年付价格（单位：分），约为月付 × 12 × 0.8。 */
    private final long priceYearly;
    private final VirtualWallet.CurrencyType currency;
    private final List<String> benefits;
    /** 该档是否允许年付。 */
    private final boolean yearlyAvailable;

    public SubscriptionPlan(Subscription.SubscriptionTier tier, String name, String description,
                             long priceMonthly, long priceYearly,
                             VirtualWallet.CurrencyType currency,
                             List<String> benefits, boolean yearlyAvailable) {
        this.tier = tier;
        this.name = name;
        this.description = description;
        this.priceMonthly = priceMonthly;
        this.priceYearly = priceYearly;
        this.currency = currency;
        this.benefits = List.copyOf(benefits);
        this.yearlyAvailable = yearlyAvailable;
    }

    /** 获取指定计费周期的价格（分）。 */
    public long priceFor(Subscription.BillingCycle cycle) {
        return cycle == Subscription.BillingCycle.YEARLY ? priceYearly : priceMonthly;
    }

    // Getters
    public Subscription.SubscriptionTier getTier() { return tier; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getPriceMonthly() { return priceMonthly; }
    public long getPriceYearly() { return priceYearly; }
    public VirtualWallet.CurrencyType getCurrency() { return currency; }
    public List<String> getBenefits() { return benefits; }
    public boolean isYearlyAvailable() { return yearlyAvailable; }
}
