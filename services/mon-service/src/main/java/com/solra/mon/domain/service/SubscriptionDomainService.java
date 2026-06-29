package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.entity.SubscriptionPlan;
import com.solra.mon.domain.entity.VirtualWallet;
import com.solra.mon.domain.repository.SubscriptionRepository;

import java.util.*;

/**
 * 订阅领域服务。
 * 封装订阅计划的查询、升降级规则和续费逻辑。
 *
 * <p>业务规则：
 * <ul>
 *   <li>升级立即生效，按剩余天数折算差价</li>
 *   <li>降级在当前周期结束后生效</li>
 *   <li>续费在当前到期时间上叠加</li>
 *   <li>年付享受月付的 8 折</li>
 * </ul>
 */
public class SubscriptionDomainService {

    private final List<SubscriptionPlan> plans;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionDomainService(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.plans = initPlans();
    }

    /** 获取全部订阅计划。 */
    public List<SubscriptionPlan> listPlans() {
        return Collections.unmodifiableList(plans);
    }

    /** 按等级查找计划。 */
    public Optional<SubscriptionPlan> findPlan(Subscription.SubscriptionTier tier) {
        return plans.stream().filter(p -> p.getTier() == tier).findFirst();
    }

    /** 计算升级差价（分）。剩余天数 × (新价格-旧价格)/30，四舍五入。 */
    public long calculateUpgradePrice(Subscription current, Subscription.SubscriptionTier newTier,
                                       Subscription.BillingCycle newCycle) {
        SubscriptionPlan oldPlan = findPlan(current.getTier()).orElseThrow();
        SubscriptionPlan newPlan = findPlan(newTier).orElseThrow();

        long oldDailyPrice = oldPlan.priceFor(current.getBillingCycle()) / 30;
        long newDailyPrice = newPlan.priceFor(newCycle) / 30;

        long remainingDays = Math.max(1,
                (current.getExpireAt() - System.currentTimeMillis()) / (24 * 3600 * 1000));
        long diffPerDay = newDailyPrice - oldDailyPrice;
        return Math.max(0, diffPerDay * remainingDays);
    }

    /** 创建订阅（新用户或已过期用户）。 */
    public Subscription createSubscription(String userId, Subscription.SubscriptionTier tier,
                                            Subscription.BillingCycle cycle) {
        SubscriptionPlan plan = findPlan(tier)
                .orElseThrow(() -> new IllegalArgumentException("Unknown tier: " + tier));

        if (!plan.isYearlyAvailable() && cycle == Subscription.BillingCycle.YEARLY) {
            throw new IllegalArgumentException("Yearly billing not available for " + tier);
        }

        long durationMs = cycle == Subscription.BillingCycle.YEARLY
                ? 365L * 24 * 3600 * 1000
                : 30L * 24 * 3600 * 1000;
        long expireAt = System.currentTimeMillis() + durationMs;

        Subscription sub = new Subscription(userId, tier, cycle, expireAt, true);
        return subscriptionRepository.save(sub);
    }

    /** 升级订阅。 */
    public Subscription upgradeSubscription(Subscription current,
                                             Subscription.SubscriptionTier newTier,
                                             Subscription.BillingCycle newCycle) {
        if (!current.isActive()) {
            throw new IllegalStateException("Cannot upgrade inactive subscription");
        }
        if (!current.canUpgradeTo(newTier)) {
            throw new IllegalArgumentException(
                    "Cannot upgrade from " + current.getTier() + " to " + newTier);
        }

        SubscriptionPlan newPlan = findPlan(newTier).orElseThrow();
        long durationMs = newCycle == Subscription.BillingCycle.YEARLY
                ? 365L * 24 * 3600 * 1000
                : 30L * 24 * 3600 * 1000;
        long newExpireAt = System.currentTimeMillis() + durationMs;

        current.upgrade(newTier, newExpireAt);
        return subscriptionRepository.save(current);
    }

    /** 降级订阅（下一周期生效）。 */
    public Subscription downgradeSubscription(Subscription current,
                                               Subscription.SubscriptionTier newTier) {
        if (!current.isActive()) {
            throw new IllegalStateException("Cannot downgrade inactive subscription");
        }
        if (!current.canDowngradeTo(newTier)) {
            throw new IllegalArgumentException(
                    "Cannot downgrade from " + current.getTier() + " to " + newTier);
        }

        current.scheduleDowngrade(newTier);
        return subscriptionRepository.save(current);
    }

    /** 续费订阅。 */
    public Subscription renewSubscription(Subscription current,
                                           Subscription.BillingCycle cycle) {
        if (current.getStatus() == Subscription.SubscriptionStatus.CANCELLED) {
            // 已取消用户重新激活
        }

        long durationMs = cycle == Subscription.BillingCycle.YEARLY
                ? 365L * 24 * 3600 * 1000
                : 30L * 24 * 3600 * 1000;
        long newExpireAt = Math.max(current.getExpireAt(), System.currentTimeMillis()) + durationMs;

        current.renew(newExpireAt);
        return subscriptionRepository.save(current);
    }

    // ── 预置四档订阅计划 ──

    private List<SubscriptionPlan> initPlans() {
        return List.of(
                new SubscriptionPlan(
                        Subscription.SubscriptionTier.FREE,
                        "Free", "免费体验，基础功能",
                        0, 0,
                        VirtualWallet.CurrencyType.DIAMOND,
                        List.of("基础空间模板 ×5", "虚拟人基础外观", "社区基础功能"),
                        false
                ),
                new SubscriptionPlan(
                        Subscription.SubscriptionTier.PLUS,
                        "Plus", "高级会员，解锁更多创作工具",
                        1900, 18200,  // ¥19/月, ¥182/年
                        VirtualWallet.CurrencyType.DIAMOND,
                        List.of("全部空间模板", "高级虚拟人定制", "优先客服支持",
                                "无广告体验", "每月 100 钻石"),
                        true
                ),
                new SubscriptionPlan(
                        Subscription.SubscriptionTier.PRO,
                        "Pro", "专业版，创作者必备",
                        4900, 47000,  // ¥49/月, ¥470/年
                        VirtualWallet.CurrencyType.DIAMOND,
                        List.of("Plus 全部权益", "创作收益分成", "资产商店上架",
                                "专属创作者徽章", "每月 300 钻石",
                                "3D 模型导入", "API 接入（1000次/月）"),
                        true
                ),
                new SubscriptionPlan(
                        Subscription.SubscriptionTier.ULTRA,
                        "Ultra", "旗舰版，品牌空间 + 全功能",
                        9900, 95000,  // ¥99/月, ¥950/年
                        VirtualWallet.CurrencyType.DIAMOND,
                        List.of("Pro 全部权益", "品牌定制空间", "团队协作管理（最多10人）",
                                "API 接入（无限制）", "每月 1000 钻石",
                                "专属技术支持", "品牌数据分析"),
                        true
                )
        );
    }
}
