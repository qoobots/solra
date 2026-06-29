package com.solra.mon.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SubscriptionPlan 值对象单元测试。
 * 验证定价计算和权益配置。
 */
@DisplayName("SubscriptionPlan — 订阅计划测试")
class SubscriptionPlanTest {

    @Nested
    @DisplayName("priceFor — 按计费周期获取价格")
    class PriceFor {

        private final SubscriptionPlan plan = new SubscriptionPlan(
                Subscription.SubscriptionTier.PLUS, "Plus", "测试计划",
                1900, 18200, VirtualWallet.CurrencyType.DIAMOND,
                List.of("权益A", "权益B"), true);

        @Test
        @DisplayName("月付返回月价")
        void monthlyReturnsMonthlyPrice() {
            assertEquals(1900, plan.priceFor(Subscription.BillingCycle.MONTHLY));
        }

        @Test
        @DisplayName("年付返回年价")
        void yearlyReturnsYearlyPrice() {
            assertEquals(18200, plan.priceFor(Subscription.BillingCycle.YEARLY));
        }
    }

    @Nested
    @DisplayName("Getters — 属性访问")
    class Getters {

        @Test
        @DisplayName("所有属性正确返回")
        void allAttributes() {
            List<String> benefits = List.of("权益A", "权益B", "权益C");
            SubscriptionPlan plan = new SubscriptionPlan(
                    Subscription.SubscriptionTier.ULTRA, "Ultra", "旗舰计划",
                    9900, 95000, VirtualWallet.CurrencyType.DIAMOND,
                    benefits, true);

            assertEquals(Subscription.SubscriptionTier.ULTRA, plan.getTier());
            assertEquals("Ultra", plan.getName());
            assertEquals("旗舰计划", plan.getDescription());
            assertEquals(9900, plan.getPriceMonthly());
            assertEquals(95000, plan.getPriceYearly());
            assertEquals(VirtualWallet.CurrencyType.DIAMOND, plan.getCurrency());
            assertEquals(3, plan.getBenefits().size());
            assertTrue(plan.isYearlyAvailable());
        }
    }
}
