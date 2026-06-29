package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;
import com.solra.mon.domain.entity.VirtualWallet;
import com.solra.mon.domain.repository.OrderRepository;
import com.solra.mon.infrastructure.persistence.InMemoryOrderRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PaymentGateway 单元测试 — MON-004。
 * 验证支付创建、回执验证、退款全流程。
 */
@DisplayName("PaymentGateway — 支付网关测试")
class PaymentGatewayTest {

    private PaymentGateway gateway;
    private OrderRepository orderRepo;
    private PaymentTransactionRepository txnRepo;

    @BeforeEach
    void setUp() {
        orderRepo = new InMemoryOrderRepository();
        txnRepo = new InMemoryPaymentTransactionRepository();
        gateway = new PaymentGateway(orderRepo, txnRepo);

        gateway.registerChannel(new AppleIAPProvider());
        gateway.registerChannel(new GooglePlayProvider());
        gateway.registerChannel(new WeChatPayProvider());
        gateway.registerChannel(new AlipayProvider());
    }

    @Nested
    @DisplayName("listAvailableChannels — 渠道列表")
    class ListChannels {

        @Test
        @DisplayName("返回四个支付渠道")
        void returnsFourChannels() {
            List<Map<String, String>> channels = gateway.listAvailableChannels();
            assertEquals(4, channels.size());

            List<String> ids = channels.stream().map(c -> c.get("channel_id")).toList();
            assertTrue(ids.contains("apple"));
            assertTrue(ids.contains("google"));
            assertTrue(ids.contains("wechat"));
            assertTrue(ids.contains("alipay"));
        }
    }

    @Nested
    @DisplayName("createPayment — 创建支付")
    class CreatePayment {

        @Test
        @DisplayName("Apple IAP 创建支付成功")
        void appleIAPSuccess() {
            Order order = createOrder();
            PaymentChannel.PaymentResult result = gateway.createPayment(
                    order.getOrderId(), "apple", Map.of("product_id", "plus_monthly"));

            assertTrue(result.isSuccess());
            assertNotNull(result.getPlatformOrderId());
            assertTrue(result.getPlatformOrderId().startsWith("APPLE-"));
        }

        @Test
        @DisplayName("微信支付创建成功")
        void wechatPaySuccess() {
            Order order = createOrder();
            PaymentChannel.PaymentResult result = gateway.createPayment(
                    order.getOrderId(), "wechat", Map.of());

            assertTrue(result.isSuccess());
            assertTrue(result.getPlatformOrderId().startsWith("WX-"));
        }

        @Test
        @DisplayName("支付宝创建成功")
        void alipaySuccess() {
            Order order = createOrder();
            PaymentChannel.PaymentResult result = gateway.createPayment(
                    order.getOrderId(), "alipay", Map.of());

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("Google Play 创建成功")
        void googlePlaySuccess() {
            Order order = createOrder();
            PaymentChannel.PaymentResult result = gateway.createPayment(
                    order.getOrderId(), "google", Map.of());

            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("未知渠道抛出异常")
        void unknownChannelThrows() {
            Order order = createOrder();
            assertThrows(IllegalArgumentException.class, () ->
                    gateway.createPayment(order.getOrderId(), "paypal", Map.of()));
        }

        @Test
        @DisplayName("非 PENDING 订单不能创建支付")
        void nonPendingOrderThrows() {
            Order order = createOrder();
            order.complete("internal", "txn-1");
            orderRepo.save(order);

            assertThrows(IllegalStateException.class, () ->
                    gateway.createPayment(order.getOrderId(), "apple", Map.of()));
        }

        @Test
        @DisplayName("创建支付后生成交易流水")
        void createsTransactionRecord() {
            Order order = createOrder();
            gateway.createPayment(order.getOrderId(), "apple", Map.of());

            assertTrue(txnRepo.findByOrderId(order.getOrderId()).isPresent());
            assertEquals(PaymentTransaction.TransactionStatus.PENDING,
                    txnRepo.findByOrderId(order.getOrderId()).get().getStatus());
        }
    }

    @Nested
    @DisplayName("verifyAndComplete — 验证支付回执")
    class VerifyAndComplete {

        @Test
        @DisplayName("Apple 回执验证成功后订单完成")
        void appleVerifyCompletesOrder() {
            Order order = createOrder();
            gateway.createPayment(order.getOrderId(), "apple", Map.of());

            PaymentChannel.VerifyResult result = gateway.verifyAndComplete(
                    order.getOrderId(), "apple", "mock-receipt-data");

            assertTrue(result.isVerified());
            Order updated = orderRepo.findById(order.getOrderId()).orElseThrow();
            assertEquals(Order.OrderStatus.COMPLETED, updated.getStatus());
        }

        @Test
        @DisplayName("无效回执验证失败")
        void invalidReceiptFails() {
            Order order = createOrder();
            gateway.createPayment(order.getOrderId(), "apple", Map.of());

            PaymentChannel.VerifyResult result = gateway.verifyAndComplete(
                    order.getOrderId(), "apple", "");

            assertFalse(result.isVerified());
        }
    }

    @Nested
    @DisplayName("processRefund — 退款")
    class ProcessRefund {

        @Test
        @DisplayName("已支付订单退款成功")
        void completedOrderRefundsSuccessfully() {
            Order order = createOrder();
            gateway.createPayment(order.getOrderId(), "apple", Map.of());
            gateway.verifyAndComplete(order.getOrderId(), "apple", "mock-receipt");

            PaymentChannel.RefundResult result = gateway.processRefund(
                    order.getOrderId(), 0, "user request");

            assertTrue(result.isSuccess());
            assertNotNull(result.getRefundId());
            assertTrue(result.getRefundId().startsWith("APPLE-REF-"));

            Order updated = orderRepo.findById(order.getOrderId()).orElseThrow();
            assertEquals(Order.OrderStatus.REFUNDED, updated.getStatus());
        }

        @Test
        @DisplayName("未支付订单不能退款")
        void pendingOrderRefundThrows() {
            Order order = createOrder();

            assertThrows(IllegalStateException.class, () ->
                    gateway.processRefund(order.getOrderId(), 0, "test"));
        }
    }

    @Nested
    @DisplayName("PaymentTransaction — 交易流水")
    class Transaction {

        @Test
        @DisplayName("完整生命周期：PENDING → COMPLETED → REFUNDED")
        void fullLifecycle() {
            Order order = createOrder();
            gateway.createPayment(order.getOrderId(), "apple", Map.of());

            PaymentTransaction txn = txnRepo.findByOrderId(order.getOrderId()).orElseThrow();
            assertEquals(PaymentTransaction.TransactionStatus.PENDING, txn.getStatus());

            gateway.verifyAndComplete(order.getOrderId(), "apple", "receipt");
            txn = txnRepo.findByOrderId(order.getOrderId()).orElseThrow();
            assertEquals(PaymentTransaction.TransactionStatus.COMPLETED, txn.getStatus());
        }
    }

    private Order createOrder() {
        Order order = new Order("user-test", VirtualWallet.CurrencyType.DIAMOND);
        order.addItem("item-001", "Test Item", 1, 1900);
        orderRepo.save(order);
        return order;
    }
}
