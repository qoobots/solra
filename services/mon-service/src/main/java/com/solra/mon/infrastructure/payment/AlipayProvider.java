package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;

import java.util.Map;

/**
 * 支付宝支付渠道 — MON-004。
 *
 * <p>通过支付宝开放平台 API 创建订单、查询支付结果、退款。
 * 生产环境需配置 AppID、私钥、支付宝公钥。
 */
public class AlipayProvider implements PaymentChannel {

    @Override
    public String getChannelId() { return "alipay"; }

    @Override
    public String getDisplayName() { return "Alipay"; }

    @Override
    public boolean supportsCurrency(Order order) {
        return true;
    }

    @Override
    public PaymentResult createPayment(Order order, Map<String, String> metadata) {
        String subject = metadata.getOrDefault("subject", "Solra Purchase");
        String platformOrderId = "ALI-" + order.getOrderId().substring(0, 8);

        // 生产环境：调用 alipay.trade.app.pay 或 alipay.trade.create
        String prepayData = "{\"platform\":\"alipay\",\"trade_no\":\""
                + platformOrderId + "\"}";

        return PaymentResult.ok(platformOrderId, prepayData);
    }

    @Override
    public VerifyResult verifyPayment(String orderId, String receipt) {
        if (receipt == null || receipt.isEmpty()) {
            return VerifyResult.fail();
        }
        // 生产环境：调用 alipay.trade.query
        String txnId = "ALI-TXN-" + orderId.substring(0, 8);
        return VerifyResult.ok(txnId, 0, "CNY");
    }

    @Override
    public RefundResult refund(String orderId, long amount, String reason) {
        // 生产环境：调用 alipay.trade.refund
        String refundId = "ALI-REF-" + orderId.substring(0, 8);
        return RefundResult.ok(refundId);
    }
}
