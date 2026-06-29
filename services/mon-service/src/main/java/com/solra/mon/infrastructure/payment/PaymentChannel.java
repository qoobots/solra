package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;

import java.util.Map;

/**
 * 支付渠道接口（策略模式）— MON-004。
 *
 * <p>各支付渠道需实现此接口：
 * <ul>
 *   <li>Apple IAP（iOS 内购）</li>
 *   <li>Google Play Billing（Android）</li>
 *   <li>微信支付（WeChat Pay）</li>
 *   <li>支付宝（Alipay）</li>
 * </ul>
 *
 * <p>PCI-DSS 合规：不在服务端存储明文卡号，使用支付令牌（tokenization）。
 */
public interface PaymentChannel {

    /** 渠道标识。 */
    String getChannelId();

    /** 渠道显示名称。 */
    String getDisplayName();

    /** 支持的货币类型。 */
    boolean supportsCurrency(Order order);

    /**
     * 创建支付订单。
     * @return 支付平台返回的预支付信息（如 prepay_id、order_id 等）
     */
    PaymentResult createPayment(Order order, Map<String, String> metadata);

    /**
     * 验证支付回执。
     * @param receipt 支付平台返回的回执/凭证
     * @return 验证结果
     */
    VerifyResult verifyPayment(String orderId, String receipt);

    /**
     * 处理退款。
     * @param orderId 原始订单ID
     * @param amount 退款金额（分），0=全额退款
     * @param reason 退款原因
     * @return 退款结果
     */
    RefundResult refund(String orderId, long amount, String reason);

    // ── 内嵌类型 ──

    class PaymentResult {
        private boolean success;
        private String platformOrderId;   // 支付平台订单号
        private String prepayData;        // 预支付凭证（客户端调起支付用）
        private String errorCode;
        private String errorMessage;

        public static PaymentResult ok(String platformOrderId, String prepayData) {
            PaymentResult r = new PaymentResult();
            r.success = true;
            r.platformOrderId = platformOrderId;
            r.prepayData = prepayData;
            return r;
        }

        public static PaymentResult fail(String errorCode, String errorMessage) {
            PaymentResult r = new PaymentResult();
            r.success = false;
            r.errorCode = errorCode;
            r.errorMessage = errorMessage;
            return r;
        }

        public boolean isSuccess() { return success; }
        public String getPlatformOrderId() { return platformOrderId; }
        public String getPrepayData() { return prepayData; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
    }

    class VerifyResult {
        private boolean verified;
        private String transactionId;
        private long amountPaid;         // 实际支付金额（分）
        private String currency;

        public static VerifyResult ok(String transactionId, long amountPaid, String currency) {
            VerifyResult r = new VerifyResult();
            r.verified = true;
            r.transactionId = transactionId;
            r.amountPaid = amountPaid;
            r.currency = currency;
            return r;
        }

        public static VerifyResult fail() {
            VerifyResult r = new VerifyResult();
            r.verified = false;
            return r;
        }

        public boolean isVerified() { return verified; }
        public String getTransactionId() { return transactionId; }
        public long getAmountPaid() { return amountPaid; }
        public String getCurrency() { return currency; }
    }

    class RefundResult {
        private boolean success;
        private String refundId;
        private String errorMessage;

        public static RefundResult ok(String refundId) {
            RefundResult r = new RefundResult();
            r.success = true;
            r.refundId = refundId;
            return r;
        }

        public static RefundResult fail(String errorMessage) {
            RefundResult r = new RefundResult();
            r.success = false;
            r.errorMessage = errorMessage;
            return r;
        }

        public boolean isSuccess() { return success; }
        public String getRefundId() { return refundId; }
        public String getErrorMessage() { return errorMessage; }
    }
}
