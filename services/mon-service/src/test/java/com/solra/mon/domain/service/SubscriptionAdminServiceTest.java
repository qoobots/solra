package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.repository.SubscriptionRepository;
import com.solra.mon.infrastructure.persistence.InMemorySubscriptionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SubscriptionAdminService 单元测试 — MON-006。
 * 验证订阅管理后台的统计、筛选、管理员操作。
 */
@DisplayName("SubscriptionAdminService — 订阅管理后台测试")
class SubscriptionAdminServiceTest {

    private SubscriptionAdminService adminService;
    private SubscriptionRepository repository;
    private SubscriptionDomainService domainService;

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        domainService = new SubscriptionDomainService(repository);
        adminService = new SubscriptionAdminService(repository, domainService);

        // 预置测试数据
        seedSubscriptions();
    }

    private void seedSubscriptions() {
        // 活跃 PLUS
        domainService.createSubscription("user-1", Subscription.SubscriptionTier.PLUS,
                Subscription.BillingCycle.MONTHLY);
        domainService.createSubscription("user-2", Subscription.SubscriptionTier.PLUS,
                Subscription.BillingCycle.MONTHLY);

        // 活跃 PRO
        domainService.createSubscription("user-3", Subscription.SubscriptionTier.PRO,
                Subscription.BillingCycle.MONTHLY);

        // 活跃 ULTRA
        domainService.createSubscription("user-4", Subscription.SubscriptionTier.ULTRA,
                Subscription.BillingCycle.YEARLY);

        // 已取消
        Subscription cancelled = domainService.createSubscription("user-5",
                Subscription.SubscriptionTier.PLUS, Subscription.BillingCycle.MONTHLY);
        cancelled.cancel("no longer needed");
        repository.save(cancelled);

        // 已过期
        Subscription expired = domainService.createSubscription("user-6",
                Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY);
        expired.expire();
        repository.save(expired);
    }

    @Nested
    @DisplayName("getStats — 统计概览")
    class GetStats {

        @Test
        @DisplayName("总订阅数正确")
        void totalCount() {
            SubscriptionAdminService.SubscriptionStats stats = adminService.getStats();
            assertEquals(6, stats.getTotal());
            assertEquals(4, stats.getActive());
            assertEquals(1, stats.getCancelled());
            assertEquals(1, stats.getExpired());
        }

        @Test
        @DisplayName("各等级分布")
        void tierDistribution() {
            SubscriptionAdminService.SubscriptionStats stats = adminService.getStats();
            var byTier = stats.getByTier();

            assertEquals(3, byTier.getOrDefault(Subscription.SubscriptionTier.PLUS, 0L));
            assertEquals(2, byTier.getOrDefault(Subscription.SubscriptionTier.PRO, 0L));
            assertEquals(1, byTier.getOrDefault(Subscription.SubscriptionTier.ULTRA, 0L));
        }

        @Test
        @DisplayName("续费率和流失率之和为100%")
        void renewalAndChurnSum() {
            SubscriptionAdminService.SubscriptionStats stats = adminService.getStats();
            assertEquals(100.0, stats.getRenewalRate() + stats.getChurnRate(), 0.01);
        }
    }

    @Nested
    @DisplayName("getExpiringSoon — 到期预警")
    class ExpiringSoon {

        @Test
        @DisplayName("30天内即将到期列表")
        void within30Days() {
            List<Subscription> expiring = adminService.getExpiringSoon(30);
            // 所有活跃订阅都应该在30天内到期
            assertEquals(4, expiring.size());
            assertTrue(expiring.stream().allMatch(Subscription::isActive));
        }

        @Test
        @DisplayName("按到期时间升序排列")
        void sortedByExpire() {
            List<Subscription> expiring = adminService.getExpiringSoon(30);
            for (int i = 1; i < expiring.size(); i++) {
                assertTrue(expiring.get(i - 1).getExpireAt() <= expiring.get(i).getExpireAt());
            }
        }
    }

    @Nested
    @DisplayName("listByTier / listByStatus — 筛选")
    class ListByFilter {

        @Test
        @DisplayName("按等级 PLUS 筛选返回3个")
        void byTierPlus() {
            List<Subscription> subs = adminService.listByTier(Subscription.SubscriptionTier.PLUS);
            assertEquals(3, subs.size());
        }

        @Test
        @DisplayName("按状态 ACTIVE 筛选")
        void byStatusActive() {
            List<Subscription> subs = adminService.listByStatus(Subscription.SubscriptionStatus.ACTIVE);
            assertEquals(4, subs.size());
        }
    }

    @Nested
    @DisplayName("管理员操作 — adminUpgrade / adminDowngrade / adminExtend")
    class AdminOperations {

        @Test
        @DisplayName("管理员升级 PLUS→PRO")
        void adminUpgradePlusToPro() {
            Subscription upgraded = adminService.adminUpgrade("user-1",
                    Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY);

            assertEquals(Subscription.SubscriptionTier.PRO, upgraded.getTier());
        }

        @Test
        @DisplayName("管理员降级 PRO→PLUS")
        void adminDowngradeProToPlus() {
            Subscription downgraded = adminService.adminDowngrade("user-3",
                    Subscription.SubscriptionTier.PLUS);

            assertEquals(Subscription.SubscriptionTier.PLUS, downgraded.getTier());
        }

        @Test
        @DisplayName("管理员延长订阅天数")
        void adminExtendDays() {
            Subscription sub = repository.findByUserId("user-1").orElseThrow();
            long oldExpire = sub.getExpireAt();

            Subscription extended = adminService.adminExtend("user-1", 15);
            long expectedNewExpire = oldExpire + 15L * 24 * 3600 * 1000;

            assertEquals(expectedNewExpire, extended.getExpireAt());
        }

        @Test
        @DisplayName("不存在的用户抛出异常")
        void nonExistentUserThrows() {
            assertThrows(IllegalArgumentException.class, () ->
                    adminService.adminUpgrade("user-999",
                            Subscription.SubscriptionTier.PRO, Subscription.BillingCycle.MONTHLY));
        }
    }
}
