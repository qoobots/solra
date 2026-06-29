package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;

import java.util.Map;

/**
 * Apple In-App Purchase 支付渠道 — MON-004。
 *
 * <p>通过 App Store Server API 验证收据并完成交易。
 * 生产环境需集成 Apple 的 verifyReceipt 端点。
 */
public class AppleIAPProvider implements PaymentChannel {

    @Override
    public String getChannelId() { return "apple"; }

    @Override
    public String getDisplayName() { return "Apple In-App Purchase"; }

    @Override
    public boolean supportsCurrency(Order order) {
        // Apple IAP 支持所有虚拟币种
        return true;
    }

    @Override
    public PaymentResult createPayment(Order order, Map<String, String> metadata) {
        String productId = metadata.getOrDefault("product_id",
                order.getItems().isEmpty() ? "unknown" : order.getItems().get(0).getProductId());

        String platformOrderId = "APPLE-" + order.getOrderId().substring(0, 8);
        // 生产环境：调用 App Store Server API createTransaction
        // 此处为 Mock，返回模拟数据
        String prepayData = "{\"platform\":\"apple\",\"order_id\":\"" + platformOrderId
                + "\",\"product_id\":\"" + productId + "\"}";

        return PaymentResult.ok(platformOrderId, prepayData);
    }

    @Override
    public VerifyResult verifyPayment(String orderId, String receipt) {
        // 生产环境：POST https://buy.itunes.apple.com/verifyReceipt
        // 此处为 Mock 验证
        if (receipt == null || receipt.isEmpty()) {
            return VerifyResult.fail();
        }
        String txnId = "APPLE-TXN-" + orderId.substring(0, 8);
        return VerifyResult.ok(txnId, 0, "CNY");
    }

    @Override
    public RefundResult refund(String orderId, long amount, String reason) {
        // 生产环境：调用 App Store Server API refundLookup + refundTransaction
        String refundId = "APPLE-REF-" + orderId.substring(0, 8);
        return RefundResult.ok(refundId);
    }
}
