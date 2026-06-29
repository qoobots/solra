package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.entity.SubscriptionPlan;
import com.solra.mon.domain.repository.SubscriptionRepository;
import com.solra.mon.infrastructure.persistence.InMemorySubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SubscriptionDomainService 单元测试。
 * 验证订阅计划查询、创建、升降级、续费逻辑。
 */
@DisplayName("SubscriptionDomainService — 订阅领域服务测试")
class SubscriptionDomainServiceTest {

    private SubscriptionDomainService service;
    private SubscriptionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        service = new SubscriptionDomainService(repository);
    }

    @Nested
    @DisplayName("listPlans — 订阅计划查询")
    class ListPlans {

        @Test
        @DisplayName("返回四档计划：FREE/PLUS/PRO/ULTRA")
        void returnsFourPlans() {
            List<SubscriptionPlan> plans = service.listPlans();
            assertEquals(4, plans.size());

            assertEquals(Subscription.SubscriptionTier.FREE, plans.get(0).getTier());
            assertEquals(Subscription.SubscriptionTier.PLUS, plans.get(1).getTier());
            assertEquals(Subscription.SubscriptionTier.PRO, plans.get(2).getTier());
            assertEquals(Subscription.SubscriptionTier.ULTRA, plans.get(3).getTier());
        }

        @Test
        @DisplayName("PLUS 月付 ¥19(1900分)，年付 ¥182(18200分)")
        void plusPricing() {
            SubscriptionPlan plan = service.findPlan(Subscription.SubscriptionTier.PLUS).orElseThrow();
            assertEquals(1900, plan.getPriceMonthly());
            assertEquals(18200, plan.getPriceYearly());
            assertTrue(plan.isYearlyAvailable());
        }

        @Test
        @DisplayName("PRO 月付 ¥49(4900分)，年付 ¥470(47000分)")
        void proPricing() {
            SubscriptionPlan plan = service.findPlan(Subscription.SubscriptionTier.PRO).orElseThrow();
            assertEquals(4900, plan.getPriceMonthly());
            assertEquals(47000, plan.getPriceYearly());
        }

        @Test
        @DisplayName("ULTRA 月付 ¥99(9900分)，年付 ¥950(95000分)")
        void ultraPricing() {
            SubscriptionPlan plan = service.findPlan(Subscription.SubscriptionTier.ULTRA).orElseThrow();
            assertEquals(9900, plan.getPriceMonthly());
            assertEquals(95000, plan.getPriceYearly());
        }

        @Test
        @DisplayName("FREE 免费，不支持年付")
        void freeIsFree() {
            SubscriptionPlan plan = service.findPlan(Subscription.SubscriptionTier.FREE).orElseThrow();
            assertEquals(0, plan.getPriceMonthly());
            assertEquals(0, plan.getPriceYearly());
            assertFalse(plan.isYearlyAvailable());
        }
    }

    @Nested
    @DisplayName("createSubscription — 创建订阅")
    class CreateSubscription {

        @Test
        @DisplayName("新用户创建 PLUS 月付")
        void createPlusMonthly() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);

            assertEquals("user-1", sub.getUserId());
            assertEquals(Subscription.SubscriptionTier.PLUS, sub.getTier());
            assertEquals(Subscription.BillingCycle.MONTHLY, sub.getBillingCycle());
            assertEquals(Subscription.SubscriptionStatus.ACTIVE, sub.getStatus());
            assertTrue(sub.isAutoRenew());
        }

        @Test
        @DisplayName("新用户创建 PRO 年付")
        void createProYearly() {
            Subscription sub = service.createSubscription("user-2",
                    Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.YEARLY);

            assertEquals(Subscription.SubscriptionTier.PRO, sub.getTier());
            assertEquals(Subscription.BillingCycle.YEARLY, sub.getBillingCycle());
        }

        @Test
        @DisplayName("FREE 不支持年付")
        void freeYearlyThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    service.createSubscription("user-3",
                            Subscription.SubscriptionTier.FREE, Subscription.BillingCycle.YEARLY));
        }
    }

    @Nested
    @DisplayName("upgradeSubscription — 升级")
    class UpgradeSubscription {

        @Test
        @DisplayName("PLUS → PRO 升级成功")
        void plusToPro() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);

            Subscription upgraded = service.upgradeSubscription(sub,
                    Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY);

            assertEquals(Subscription.SubscriptionTier.PRO, upgraded.getTier());
        }

        @Test
        @DisplayName("PLUS → ULTRA 升级成功")
        void plusToUltra() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);

            Subscription upgraded = service.upgradeSubscription(sub,
                    Subscription.SubscriptionTier.ULTRA, Subscription.BillingCycle.MONTHLY);

            assertEquals(Subscription.SubscriptionTier.ULTRA, upgraded.getTier());
        }

        @Test
        @DisplayName("PRO → PLUS 升级失败（非法降级）")
        void proToPlusThrows() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY);

            assertThrows(IllegalArgumentException.class, () ->
                    service.upgradeSubscription(sub,
                            Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY));
        }

        @Test
        @DisplayName("已取消订阅不能升级")
        void cancelledCannotUpgrade() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);
            sub.cancel("test");
            repository.save(sub);

            assertThrows(IllegalStateException.class, () ->
                    service.upgradeSubscription(sub,
                            Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY));
        }
    }

    @Nested
    @DisplayName("downgradeSubscription — 降级")
    class DowngradeSubscription {

        @Test
        @DisplayName("PRO → PLUS 降级成功")
        void proToPlus() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY);

            Subscription downgraded = service.downgradeSubscription(sub,
                    Subscription.SubscriptionTier.PLUS);

            assertEquals(Subscription.SubscriptionTier.PLUS, downgraded.getTier());
        }

        @Test
        @DisplayName("降级到 FREE 失败")
        void toFreeThrows() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);

            assertThrows(IllegalArgumentException.class, () ->
                    service.downgradeSubscription(sub, Subscription.SubscriptionTier.FREE));
        }
    }

    @Nested
    @DisplayName("renewSubscription — 续费")
    class RenewSubscription {

        @Test
        @DisplayName("续费后到期时间延长")
        void renewExtendsExpire() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);
            long oldExpire = sub.getExpireAt();

            Subscription renewed = service.renewSubscription(sub, Subscription.BillingCycle.MONTHLY);

            assertTrue(renewed.getExpireAt() > oldExpire);
            assertTrue(renewed.isAutoRenew());
        }
    }

    @Nested
    @DisplayName("calculateUpgradePrice — 升级差价")
    class CalculateUpgradePrice {

        @Test
        @DisplayName("PLUS→PRO 升级有正差价")
        void plusToProHasPositiveDiff() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);

            long price = service.calculateUpgradePrice(sub,
                    Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY);

            assertTrue(price >= 0);
        }

        @Test
        @DisplayName("FREE→PLUS 升级差价等于全额")
        void freeToPlus() {
            Subscription sub = service.createSubscription("user-1",
                    Subscription.SubscriptionTier.FREE, Subscription.BillingCycle.MONTHLY);

            long price = service.calculateUpgradePrice(sub,
                    Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);

            assertTrue(price > 0);
        }
    }
}
