package com.solra.mon.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CreatorEarning 实体单元测试 — MON-003。
 */
@DisplayName("CreatorEarning — 创作者收益测试")
class CreatorEarningTest {

    @Nested
    @DisplayName("构造函数 — 平台抽成30%")
    class Commission {

        @Test
        @DisplayName("¥100售价：平台30，创作者70")
        void sale100() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 10000L);
            assertEquals(3000L, earning.getCommission());
            assertEquals(7000L, earning.getCreatorShare());
        }

        @Test
        @DisplayName("¥200售价：平台60，创作者140")
        void sale200() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 20000L);
            assertEquals(6000L, earning.getCommission());
            assertEquals(14000L, earning.getCreatorShare());
        }

        @Test
        @DisplayName("初始状态为 PENDING")
        void initialStatusPending() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 10000L);
            assertEquals(CreatorEarning.EarningStatus.PENDING, earning.getStatus());
        }
    }

    @Nested
    @DisplayName("生命周期 — PENDING → SETTLED → WITHDRAWN")
    class Lifecycle {

        @Test
        @DisplayName("完整生命周期")
        void fullLifecycle() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 20000L);

            // settle
            earning.settle("2026-06");
            assertEquals(CreatorEarning.EarningStatus.SETTLED, earning.getStatus());
            assertEquals("2026-06", earning.getSettlementPeriod());
            assertTrue(earning.getSettledAt() > 0);

            // withdraw
            earning.withdraw();
            assertEquals(CreatorEarning.EarningStatus.WITHDRAWN, earning.getStatus());
            assertTrue(earning.getWithdrawnAt() > 0);
        }

        @Test
        @DisplayName("不能重复结算")
        void cannotSettleTwice() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 10000L);
            earning.settle("2026-06");
            assertThrows(IllegalStateException.class, () -> earning.settle("2026-07"));
        }

        @Test
        @DisplayName("不能跳过结算直接提现")
        void cannotWithdrawWithoutSettle() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 20000L);
            assertThrows(IllegalStateException.class, earning::withdraw);
        }
    }

    @Nested
    @DisplayName("取消")
    class Cancel {

        @Test
        @DisplayName("PENDING 状态可以取消")
        void pendingCanCancel() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 10000L);
            earning.cancel();
            assertEquals(CreatorEarning.EarningStatus.CANCELLED, earning.getStatus());
        }

        @Test
        @DisplayName("SETTLED 状态不能取消")
        void settledCannotCancel() {
            CreatorEarning earning = new CreatorEarning("c1", "i1", "o1", 10000L);
            earning.settle("2026-06");
            assertThrows(IllegalStateException.class, earning::cancel);
        }
    }

    @Nested
    @DisplayName("常量")
    class Constants {

        @Test
        @DisplayName("平台抽成率为30%")
        void commissionRate() {
            assertEquals(0.30, CreatorEarning.PLATFORM_COMMISSION_RATE, 0.001);
        }

        @Test
        @DisplayName("最低提现金额为¥100（10000分）")
        void minWithdrawal() {
            assertEquals(10000L, CreatorEarning.MIN_WITHDRAWAL_AMOUNT);
        }
    }
}
