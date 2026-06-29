package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.CreatorEarning;
import com.solra.mon.domain.entity.VirtualItem;
import com.solra.mon.domain.entity.VirtualWallet;
import com.solra.mon.domain.repository.VirtualItemRepository;
import com.solra.mon.infrastructure.persistence.InMemoryVirtualItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CreatorMarketplaceService 单元测试 — MON-003。
 * 验证创作者商品管理、收益记录、月结、提现。
 */
@DisplayName("CreatorMarketplaceService — 创作者交易市场测试")
class CreatorMarketplaceServiceTest {

    private CreatorMarketplaceService service;
    private VirtualItemRepository itemRepo;

    @BeforeEach
    void setUp() {
        itemRepo = new InMemoryVirtualItemRepository();
        service = new CreatorMarketplaceService(itemRepo);
    }

    @Nested
    @DisplayName("商品管理")
    class ItemManagement {

        @Test
        @DisplayName("创作者上架商品")
        void publishItem() {
            VirtualItem item = new VirtualItem("cr-item-1", "自定义皮肤",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 5000,
                    VirtualWallet.CurrencyType.DIAMOND);

            VirtualItem published = service.publishCreatorItem("creator-1", item);

            assertEquals("creator-1", published.getCreatorId());
            assertTrue(published.isAvailable());
        }

        @Test
        @DisplayName("创作者下架自己的商品")
        void suspendOwnItem() {
            VirtualItem item = new VirtualItem("cr-item-2", "模板",
                    VirtualItem.VirtualItemType.SPACE_TEMPLATE, 3000,
                    VirtualWallet.CurrencyType.DIAMOND);
            service.publishCreatorItem("creator-1", item);

            VirtualItem suspended = service.suspendCreatorItem("creator-1", "cr-item-2");
            assertFalse(suspended.isAvailable());
        }

        @Test
        @DisplayName("不能下架他人的商品")
        void cannotSuspendOthersItem() {
            VirtualItem item = new VirtualItem("cr-item-3", "他人商品",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 1000,
                    VirtualWallet.CurrencyType.DIAMOND);
            service.publishCreatorItem("creator-1", item);

            assertThrows(IllegalStateException.class, () ->
                    service.suspendCreatorItem("creator-2", "cr-item-3"));
        }

        @Test
        @DisplayName("获取创作者所有商品")
        void getCreatorItems() {
            VirtualItem item1 = new VirtualItem("cr-a-1", "A",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 1000,
                    VirtualWallet.CurrencyType.DIAMOND);
            VirtualItem item2 = new VirtualItem("cr-a-2", "B",
                    VirtualItem.VirtualItemType.GIFT, 500,
                    VirtualWallet.CurrencyType.DIAMOND);
            service.publishCreatorItem("creator-1", item1);
            service.publishCreatorItem("creator-1", item2);

            List<VirtualItem> items = service.getCreatorItems("creator-1");
            assertEquals(2, items.size());
        }
    }

    @Nested
    @DisplayName("收益记录")
    class Earnings {

        @Test
        @DisplayName("平台抽成30%，创作者得70%")
        void commissionSplit() {
            CreatorEarning earning = service.recordEarning(
                    "creator-1", "item-1", "order-1", 10000L); // ¥100

            assertEquals(10000L, earning.getSaleAmount());
            assertEquals(3000L, earning.getCommission());     // 30%
            assertEquals(7000L, earning.getCreatorShare());   // 70%
        }

        @Test
        @DisplayName("多笔收益汇总")
        void multipleEarnings() {
            service.recordEarning("creator-1", "item-1", "order-1", 5000L);
            service.recordEarning("creator-1", "item-2", "order-2", 10000L);

            long balance = service.getPendingBalance("creator-1");
            // 5000*0.7 + 10000*0.7 = 3500 + 7000 = 10500
            assertEquals(10500L, balance);
        }
    }

    @Nested
    @DisplayName("月结")
    class MonthlySettlement {

        @Test
        @DisplayName("月结后状态变为 SETTLED")
        void settleChangesStatus() {
            CreatorEarning earning = service.recordEarning(
                    "creator-1", "item-1", "order-1", 10000L);
            assertEquals(CreatorEarning.EarningStatus.PENDING, earning.getStatus());

            List<CreatorEarning> settled = service.settleMonthly("2026-06");
            assertEquals(1, settled.size());
            assertEquals(CreatorEarning.EarningStatus.SETTLED, settled.get(0).getStatus());
            assertEquals("2026-06", settled.get(0).getSettlementPeriod());
        }

        @Test
        @DisplayName("只结算 PENDING 状态收益")
        void onlySettlePending() {
            CreatorEarning e1 = service.recordEarning("c1", "i1", "o1", 10000L);
            e1.settle("2026-05"); // 手动标记已结算

            CreatorEarning e2 = service.recordEarning("c1", "i2", "o2", 5000L);

            List<CreatorEarning> settled = service.settleMonthly("2026-06");
            assertEquals(1, settled.size()); // 只有 e2 被结算
        }
    }

    @Nested
    @DisplayName("提现")
    class Withdrawal {

        @Test
        @DisplayName("满足最低 ¥100 可提现")
        void canWithdrawAboveMinimum() {
            CreatorEarning earning = service.recordEarning(
                    "creator-1", "item-1", "order-1", 15000L); // 售价¥150, 分得¥105
            earning.settle("2026-06");

            CreatorEarning withdrawn = service.withdraw(earning.getEarningId());
            assertEquals(CreatorEarning.EarningStatus.WITHDRAWN, withdrawn.getStatus());
        }

        @Test
        @DisplayName("低于最低 ¥100 不能提现")
        void cannotWithdrawBelowMinimum() {
            CreatorEarning earning = service.recordEarning(
                    "creator-1", "item-1", "order-1", 1000L); // 售价¥10, 分得¥7
            earning.settle("2026-06");

            assertThrows(IllegalStateException.class, () ->
                    service.withdraw(earning.getEarningId()));
        }

        @Test
        @DisplayName("未结算的收益不能提现")
        void cannotWithdrawPending() {
            CreatorEarning earning = service.recordEarning(
                    "creator-1", "item-1", "order-1", 20000L);

            assertThrows(IllegalStateException.class, () ->
                    service.withdraw(earning.getEarningId()));
        }
    }

    @Nested
    @DisplayName("创作者余额汇总")
    class BalanceSummary {

        @Test
        @DisplayName("多创作者余额独立统计")
        void separateBalances() {
            service.recordEarning("creator-1", "i1", "o1", 10000L);
            service.recordEarning("creator-2", "i2", "o2", 20000L);

            assertEquals(7000L, service.getPendingBalance("creator-1"));
            assertEquals(14000L, service.getPendingBalance("creator-2"));
        }

        @Test
        @DisplayName("getAllCreatorBalances 返回所有创作者")
        void allCreatorBalances() {
            service.recordEarning("c1", "i1", "o1", 10000L);
            service.recordEarning("c2", "i2", "o2", 5000L);

            Map<String, CreatorMarketplaceService.CreatorBalanceSummary> balances =
                    service.getAllCreatorBalances();
            assertEquals(2, balances.size());
        }
    }
}
