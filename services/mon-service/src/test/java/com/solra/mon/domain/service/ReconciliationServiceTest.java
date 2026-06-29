package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.Order;
import com.solra.mon.domain.entity.VirtualWallet;
import com.solra.mon.domain.repository.OrderRepository;
import com.solra.mon.domain.repository.VirtualItemRepository;
import com.solra.mon.infrastructure.payment.*;
import com.solra.mon.infrastructure.persistence.InMemoryOrderRepository;
import com.solra.mon.infrastructure.persistence.InMemoryVirtualItemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReconciliationService 单元测试 — MON-007。
 * 验证日对账、月结算、差错率。
 */
@DisplayName("ReconciliationService — 对账结算测试")
class ReconciliationServiceTest {

    private ReconciliationService service;
    private OrderRepository orderRepo;
    private PaymentTransactionRepository txnRepo;
    private CreatorMarketplaceService creatorMarketplace;
    private VirtualItemRepository itemRepo;

    @BeforeEach
    void setUp() {
        orderRepo = new InMemoryOrderRepository();
        txnRepo = new InMemoryPaymentTransactionRepository();
        itemRepo = new InMemoryVirtualItemRepository();
        creatorMarketplace = new CreatorMarketplaceService(itemRepo);
        service = new ReconciliationService(orderRepo, txnRepo, creatorMarketplace);
    }

    @Nested
    @DisplayName("日对账 — reconcileDaily")
    class DailyReconciliation {

        @Test
        @DisplayName("无订单时对账通过，差错率0%")
        void emptyOrders() {
            ReconciliationService.DailyReconciliationReport report =
                    service.reconcileDaily("2026-06-30");

            assertEquals(0, report.getTotalOrders());
            assertEquals(0, report.getDiscrepancies());
            assertEquals(0.0, report.getErrorRate(), 0.001);
            assertTrue(report.isWithinTolerance());
        }

        @Test
        @DisplayName("已完成订单有对应支付流水，对账通过")
        void matchedOrders() {
            Order order = createCompletedOrder("order-1", 1000L);
            PaymentTransaction txn = new PaymentTransaction(
                    "order-1", "apple", 1000L, PaymentTransaction.TransactionStatus.COMPLETED);
            txnRepo.save(txn);

            ReconciliationService.DailyReconciliationReport report =
                    service.reconcileDaily("2026-06-30");

            assertEquals(1, report.getMatchedOrders());
            assertEquals(0, report.getDiscrepancies());
        }

        @Test
        @DisplayName("金额不一致产生差异")
        void amountMismatch() {
            Order order = createCompletedOrder("order-2", 1000L);
            PaymentTransaction txn = new PaymentTransaction(
                    "order-2", "apple", 900L, PaymentTransaction.TransactionStatus.COMPLETED);
            txnRepo.save(txn);

            ReconciliationService.DailyReconciliationReport report =
                    service.reconcileDaily("2026-06-30");

            assertEquals(1, report.getDiscrepancies());
            assertEquals(ReconciliationService.ReconciliationDiscrepancy.DiscrepancyType.AMOUNT_MISMATCH,
                    report.getDetails().get(0).getType());
            assertEquals(100L, report.getDetails().get(0).getDiff());
        }

        @Test
        @DisplayName("差错率在容忍范围内")
        void errorRateWithinTolerance() {
            // 100个订单中1个差异 = 1% > 0.01% 容忍度
            for (int i = 0; i < 100; i++) {
                Order order = createCompletedOrder("order-" + i, 1000L);
                PaymentTransaction txn = new PaymentTransaction(
                        "order-" + i, "apple", 1000L, PaymentTransaction.TransactionStatus.COMPLETED);
                txnRepo.save(txn);
            }
            // 制造1个差异
            Order badOrder = createCompletedOrder("order-bad", 2000L);
            PaymentTransaction badTxn = new PaymentTransaction(
                    "order-bad", "apple", 1000L, PaymentTransaction.TransactionStatus.COMPLETED);
            txnRepo.save(badTxn);

            ReconciliationService.DailyReconciliationReport report =
                    service.reconcileDaily("2026-06-30");

            // 1/101 ≈ 0.99% > 0.01%，不在容忍范围内
            assertFalse(report.isWithinTolerance());
            assertTrue(report.getErrorRate() > 0.01);
        }
    }

    @Nested
    @DisplayName("月结算 — settleMonthly")
    class MonthlySettlement {

        @Test
        @DisplayName("月结汇总正确")
        void monthlySettlement() {
            creatorMarketplace.recordEarning("c1", "i1", "o1", 10000L);
            creatorMarketplace.recordEarning("c1", "i2", "o2", 5000L);
            creatorMarketplace.recordEarning("c2", "i3", "o3", 20000L);

            ReconciliationService.MonthlySettlementReport report =
                    service.settleMonthly("2026-06");

            assertEquals(3, report.getSettledCount());
            assertEquals(35000L, report.getTotalSales());
            assertEquals(10500L, report.getTotalCommission());  // 30% of 35000
            assertEquals(24500L, report.getTotalCreatorShare()); // 70% of 35000

            Map<String, Long> byCreator = report.getByCreator();
            assertEquals(2, byCreator.size());
        }
    }

    @Nested
    @DisplayName("品牌结算 — getBrandSettlement（MON-005预留）")
    class BrandSettlement {

        @Test
        @DisplayName("品牌结算预留接口正常返回")
        void brandSettlementReserved() {
            ReconciliationService.BrandSettlementSummary summary =
                    service.getBrandSettlement("brand-001", "2026-06");

            assertEquals("brand-001", summary.getBrandId());
            assertEquals("2026-06", summary.getPeriod());
        }
    }

    private Order createCompletedOrder(String orderId, long amount) {
        Order order = new Order("user-1", VirtualWallet.CurrencyType.DIAMOND) {
            @Override
            public String getOrderId() { return orderId; }
        };
        order.addItem("item-1", "Test", 1, amount);
        order.complete("apple", "txn-" + orderId);
        orderRepo.save(order);
        return order;
    }
}
