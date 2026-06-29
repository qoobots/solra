package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;

import java.util.Map;

/**
 * 微信支付渠道 — MON-004。
 *
 * <p>通过微信支付 API V3 统一下单、查询订单、申请退款。
 * 生产环境需配置商户号、API V3密钥、证书。
 */
public class WeChatPayProvider implements PaymentChannel {

    @Override
    public String getChannelId() { return "wechat"; }

    @Override
    public String getDisplayName() { return "WeChat Pay"; }

    @Override
    public boolean supportsCurrency(Order order) {
        return true;
    }

    @Override
    public PaymentResult createPayment(Order order, Map<String, String metadata) {
        String description = metadata.getOrDefault("description", "Solra Purchase");
        String platformOrderId = "WX-" + order.getOrderId().substring(0, 8);

        // 生产环境：POST https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi
        // 或 native/app 等支付方式
        String prepayData = "{\"platform\":\"wechat\",\"prepay_id\":\"wx"
                + order.getOrderId().substring(0, 12) + "\"}";

        return PaymentResult.ok(platformOrderId, prepayData);
    }

    @Override
    public VerifyResult verifyPayment(String orderId, String receipt) {
        if (receipt == null || receipt.isEmpty()) {
            return VerifyResult.fail();
        }
        // 生产环境：GET /v3/pay/transactions/out-trade-no/{out_trade_no}
        String txnId = "WX-TXN-" + orderId.substring(0, 8);
        return VerifyResult.ok(txnId, 0, "CNY");
    }

    @Override
    public RefundResult refund(String orderId, long amount, String reason) {
        // 生产环境：POST /v3/refund/domestic/refunds
        String refundId = "WX-REF-" + orderId.substring(0, 8);
        return RefundResult.ok(refundId);
    }
}
