package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.repository.SubscriptionRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 订阅管理后台领域服务 — MON-006。
 *
 * <p>职责：
 * <ul>
 *   <li>订阅统计：续费率、流失率、各等级分布</li>
 *   <li>到期预警：即将到期的订阅列表</li>
 *   <li>批量操作：权益配置变更</li>
 *   <li>升降级操作：管理员手动干预</li>
 * </ul>
 */
public class SubscriptionAdminService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionDomainService subscriptionDomainService;

    public SubscriptionAdminService(SubscriptionRepository subscriptionRepository,
                                     SubscriptionDomainService subscriptionDomainService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionDomainService = subscriptionDomainService;
    }

    /** 订阅总览统计。 */
    public SubscriptionStats getStats() {
        List<Subscription> all = subscriptionRepository.findByStatus(null);

        long total = all.size();
        long active = all.stream().filter(Subscription::isActive).count();
        long cancelled = all.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.CANCELLED).count();
        long expired = all.stream()
                .filter(s -> s.getStatus() == Subscription.SubscriptionStatus.EXPIRED).count();

        Map<Subscription.SubscriptionTier, Long> byTier = all.stream()
                .collect(Collectors.groupingBy(Subscription::getTier, Collectors.counting()));

        // 续费率 = 自动续费活跃用户 / 总活跃用户
        long autoRenewActive = all.stream()
                .filter(s -> s.isActive() && s.isAutoRenew()).count();
        double renewalRate = active > 0 ? (double) autoRenewActive / active * 100 : 0;

        return new SubscriptionStats(total, active, cancelled, expired, byTier, renewalRate);
    }

    /** 即将到期的订阅（7天内）。 */
    public List<Subscription> getExpiringSoon(int withinDays) {
        long threshold = System.currentTimeMillis() + withinDays * 24L * 3600 * 1000;
        return subscriptionRepository.findByStatus(null).stream()
                .filter(Subscription::isActive)
                .filter(s -> s.getExpireAt() <= threshold)
                .sorted(Comparator.comparingLong(Subscription::getExpireAt))
                .collect(Collectors.toList());
    }

    /** 按等级筛选订阅列表。 */
    public List<Subscription> listByTier(Subscription.SubscriptionTier tier) {
        return subscriptionRepository.findByStatus(null).stream()
                .filter(s -> s.getTier() == tier)
                .sorted(Comparator.comparingLong(Subscription::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** 按状态筛选。 */
    public List<Subscription> listByStatus(Subscription.SubscriptionStatus status) {
        return subscriptionRepository.findByStatus(status);
    }

    /** 管理员手动升级用户订阅。 */
    public Subscription adminUpgrade(String userId, Subscription.SubscriptionTier newTier,
                                      Subscription.BillingCycle newCycle) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));
        return subscriptionDomainService.upgradeSubscription(current, newTier, newCycle);
    }

    /** 管理员手动降级用户订阅。 */
    public Subscription adminDowngrade(String userId, Subscription.SubscriptionTier newTier) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));
        return subscriptionDomainService.downgradeSubscription(current, newTier);
    }

    /** 管理员手动延长订阅（补偿等场景）。 */
    public Subscription adminExtend(String userId, int extraDays) {
        Subscription current = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for user: " + userId));
        long newExpire = current.getExpireAt() + extraDays * 24L * 3600 * 1000;
        current.renew(newExpire);
        return subscriptionRepository.save(current);
    }

    // ── 统计值对象 ──

    public static class SubscriptionStats {
        private final long total;
        private final long active;
        private final long cancelled;
        private final long expired;
        private final Map<Subscription.SubscriptionTier, Long> byTier;
        private final double renewalRate;

        public SubscriptionStats(long total, long active, long cancelled, long expired,
                                  Map<Subscription.SubscriptionTier, Long> byTier, double renewalRate) {
            this.total = total;
            this.active = active;
            this.cancelled = cancelled;
            this.expired = expired;
            this.byTier = Map.copyOf(byTier);
            this.renewalRate = renewalRate;
        }

        public long getTotal() { return total; }
        public long getActive() { return active; }
        public long getCancelled() { return cancelled; }
        public long getExpired() { return expired; }
        public Map<Subscription.SubscriptionTier, Long> getByTier() { return byTier; }
        public double getRenewalRate() { return renewalRate; }

        /** 流失率 = 1 - 续费率。 */
        public double getChurnRate() { return 100.0 - renewalRate; }
    }
}
