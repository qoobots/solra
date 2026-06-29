package com.solra.mon.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VirtualWallet 实体单元测试。
 * 验证多币种钱包的充值/扣款/余额查询。
 */
@DisplayName("VirtualWallet — 虚拟钱包测试")
class VirtualWalletTest {

    private VirtualWallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new VirtualWallet("user-1");
    }

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("初始所有币种余额为0")
        void allBalancesAreZero() {
            assertEquals(0L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
            assertEquals(0L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));
            assertEquals(0L, wallet.getBalance(VirtualWallet.CurrencyType.FAITH_POINT));
        }

        @Test
        @DisplayName("userId 正确")
        void userIdCorrect() {
            assertEquals("user-1", wallet.getUserId());
        }
    }

    @Nested
    @DisplayName("addBalance() — 充值")
    class AddBalance {

        @Test
        @DisplayName("正常充值 GOLD")
        void addGold() {
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 100L, "bonus");
            assertEquals(100L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("正常充值 DIAMOND")
        void addDiamond() {
            wallet.addBalance(VirtualWallet.CurrencyType.DIAMOND, 50L, "purchase");
            assertEquals(50L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));
        }

        @Test
        @DisplayName("正常充值 FAITH_POINT")
        void addFaithPoint() {
            wallet.addBalance(VirtualWallet.CurrencyType.FAITH_POINT, 200L, "daily");
            assertEquals(200L, wallet.getBalance(VirtualWallet.CurrencyType.FAITH_POINT));
        }

        @Test
        @DisplayName("多次充值累加")
        void multipleAddsAccumulate() {
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 100L, "bonus");
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 50L, "bonus");
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 25L, "bonus");
            assertEquals(175L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("充值金额为0抛异常")
        void zeroAmountThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 0L, "zero"));
        }

        @Test
        @DisplayName("充值金额为负抛异常")
        void negativeAmountThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.addBalance(VirtualWallet.CurrencyType.GOLD, -100L, "negative"));
        }

        @Test
        @DisplayName("不同币种互不影响")
        void currenciesAreIndependent() {
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 100L, "bonus");
            wallet.addBalance(VirtualWallet.CurrencyType.DIAMOND, 200L, "bonus");

            assertEquals(100L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
            assertEquals(200L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));
            assertEquals(0L, wallet.getBalance(VirtualWallet.CurrencyType.FAITH_POINT));
        }
    }

    @Nested
    @DisplayName("deductBalance() — 扣款")
    class DeductBalance {

        @Test
        @DisplayName("正常扣款")
        void normalDeduct() {
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 200L, "bonus");
            wallet.deductBalance(VirtualWallet.CurrencyType.GOLD, 50L, "purchase");

            assertEquals(150L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
        }

        @Test
        @DisplayName("余额不足抛异常")
        void insufficientBalanceThrows() {
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 50L, "bonus");
            assertThrows(IllegalStateException.class,
                    () -> wallet.deductBalance(VirtualWallet.CurrencyType.GOLD, 100L, "purchase"));
        }

        @Test
        @DisplayName("扣款金额为0抛异常")
        void deductZeroThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.deductBalance(VirtualWallet.CurrencyType.GOLD, 0L, "zero"));
        }

        @Test
        @DisplayName("扣款金额为负抛异常")
        void deductNegativeThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> wallet.deductBalance(VirtualWallet.CurrencyType.GOLD, -50L, "negative"));
        }

        @Test
        @DisplayName("刚好扣完余额")
        void deductExactBalance() {
            wallet.addBalance(VirtualWallet.CurrencyType.GOLD, 100L, "bonus");
            wallet.deductBalance(VirtualWallet.CurrencyType.GOLD, 100L, "purchase");

            assertEquals(0L, wallet.getBalance(VirtualWallet.CurrencyType.GOLD));
        }
    }

    @Nested
    @DisplayName("完整交易流程")
    class FullFlow {

        @Test
        @DisplayName("充值 → 消费 → 再充值")
        void rechargeConsumeRecharge() {
            wallet.addBalance(VirtualWallet.CurrencyType.DIAMOND, 100L, "purchase");
            wallet.deductBalance(VirtualWallet.CurrencyType.DIAMOND, 30L, "buy item");
            assertEquals(70L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));

            wallet.addBalance(VirtualWallet.CurrencyType.DIAMOND, 50L, "bonus");
            assertEquals(120L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));

            wallet.deductBalance(VirtualWallet.CurrencyType.DIAMOND, 120L, "buy premium");
            assertEquals(0L, wallet.getBalance(VirtualWallet.CurrencyType.DIAMOND));
        }
    }
}
