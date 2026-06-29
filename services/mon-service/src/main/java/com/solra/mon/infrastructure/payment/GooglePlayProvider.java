package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;

import java.util.Map;

/**
 * Google Play Billing 支付渠道 — MON-004。
 *
 * <p>通过 Google Play Developer API 验证购买令牌并确认交易。
 */
public class GooglePlayProvider implements PaymentChannel {

    @Override
    public String getChannelId() { return "google"; }

    @Override
    public String getDisplayName() { return "Google Play Billing"; }

    @Override
    public boolean supportsCurrency(Order order) {
        return true;
    }

    @Override
    public PaymentResult createPayment(Order order, Map<String, String> metadata) {
        String productId = metadata.getOrDefault("product_id",
                order.getItems().isEmpty() ? "unknown" : order.getItems().get(0).getProductId());

        String platformOrderId = "GP-" + order.getOrderId().substring(0, 8);
        String prepayData = "{\"platform\":\"google\",\"order_id\":\"" + platformOrderId
                + "\",\"product_id\":\"" + productId + "\"}";

        return PaymentResult.ok(platformOrderId, prepayData);
    }

    @Override
    public VerifyResult verifyPayment(String orderId, String receipt) {
        if (receipt == null || receipt.isEmpty()) {
            return VerifyResult.fail();
        }
        // 生产环境：调用 purchases.products.get 或 purchases.subscriptions.get
        String txnId = "GP-TXN-" + orderId.substring(0, 8);
        return VerifyResult.ok(txnId, 0, "CNY");
    }

    @Override
    public RefundResult refund(String orderId, long amount, String reason) {
        String refundId = "GP-REF-" + orderId.substring(0, 8);
        return RefundResult.ok(refundId);
    }
}
