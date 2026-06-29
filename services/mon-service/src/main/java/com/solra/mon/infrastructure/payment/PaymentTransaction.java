package com.solra.mon.infrastructure.payment;

import java.util.UUID;

/**
 * 支付交易流水 — MON-004。
 * 记录每笔支付的生命周期，满足审计和 PCI-DSS 合规要求。
 */
public class PaymentTransaction {

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }

    private String transactionId;
    private String orderId;
    private String channelId;
    private long amount;                // 单位：分
    private TransactionStatus status;
    private String platformOrderId;     // 支付平台订单号
    private String platformTxnId;       // 支付平台交易号（验证后填充）
    private long createdAt;
    private long updatedAt;

    public PaymentTransaction(String orderId, String channelId, long amount,
                               TransactionStatus status) {
        this.transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8);
        this.orderId = orderId;
        this.channelId = channelId;
        this.amount = amount;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public void markCompleted(String platformTxnId) {
        this.status = TransactionStatus.COMPLETED;
        this.platformTxnId = platformTxnId;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markFailed(String reason) {
        this.status = TransactionStatus.FAILED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void markRefunded() {
        this.status = TransactionStatus.REFUNDED;
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public String getOrderId() { return orderId; }
    public String getChannelId() { return channelId; }
    public long getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public String getPlatformOrderId() { return platformOrderId; }
    public void setPlatformOrderId(String platformOrderId) { this.platformOrderId = platformOrderId; }
    public String getPlatformTxnId() { return platformTxnId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
