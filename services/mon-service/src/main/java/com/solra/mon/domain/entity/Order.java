package com.solra.mon.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 订单领域实体。
 * 管理购买交易的生命周期：创建→支付→完成→退款。
 */
public class Order {

    public enum OrderStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }

    private String orderId;
    private String userId;
    private List<OrderItem> items;
    private long totalAmount;       // 单位：分
    private VirtualWallet.CurrencyType currency;
    private OrderStatus status;
    private String paymentChannel;
    private String transactionId;
    private long createdAt;
    private long updatedAt;

    public Order(String userId, VirtualWallet.CurrencyType currency) {
        this.orderId = UUID.randomUUID().toString();
        this.userId = userId;
        this.items = new ArrayList<>();
        this.currency = currency;
        this.status = OrderStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public void addItem(String productId, String productName, int quantity, long unitPrice) {
        OrderItem item = new OrderItem(productId, productName, quantity, unitPrice);
        this.items.add(item);
        recalculateTotal();
    }

    public void complete(String paymentChannel, String transactionId) {
        this.status = OrderStatus.COMPLETED;
        this.paymentChannel = paymentChannel;
        this.transactionId = transactionId;
        this.updatedAt = System.currentTimeMillis();
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
        this.updatedAt = System.currentTimeMillis();
    }

    public void refund() {
        if (this.status != OrderStatus.COMPLETED) {
            throw new IllegalStateException("Only completed orders can be refunded");
        }
        this.status = OrderStatus.REFUNDED;
        this.updatedAt = System.currentTimeMillis();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
                .mapToLong(i -> i.getUnitPrice() * i.getQuantity())
                .sum();
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<OrderItem> getItems() { return items; }
    public long getTotalAmount() { return totalAmount; }
    public VirtualWallet.CurrencyType getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentChannel() { return paymentChannel; }
    public String getTransactionId() { return transactionId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public static class OrderItem {
        private String productId;
        private String productName;
        private int quantity;
        private long unitPrice;

        public OrderItem(String productId, String productName, int quantity, long unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public long getUnitPrice() { return unitPrice; }
    }
}
