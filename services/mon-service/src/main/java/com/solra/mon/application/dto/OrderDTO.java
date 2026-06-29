package com.solra.mon.application.dto;

import com.solra.mon.domain.entity.Order;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单 DTO。
 */
public class OrderDTO {

    private String orderId;
    private String userId;
    private List<OrderItemDTO> items;
    private long totalAmount;
    private String currency;
    private String status;
    private String paymentChannel;
    private String transactionId;
    private long createdAt;
    private long updatedAt;

    public static OrderDTO from(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.orderId = order.getOrderId();
        dto.userId = order.getUserId();
        dto.items = order.getItems().stream().map(i -> {
            OrderItemDTO idto = new OrderItemDTO();
            idto.productId = i.getProductId();
            idto.productName = i.getProductName();
            idto.quantity = i.getQuantity();
            idto.unitPrice = i.getUnitPrice();
            return idto;
        }).collect(Collectors.toList());
        dto.totalAmount = order.getTotalAmount();
        dto.currency = order.getCurrency().name();
        dto.status = order.getStatus().name();
        dto.paymentChannel = order.getPaymentChannel();
        dto.transactionId = order.getTransactionId();
        dto.createdAt = order.getCreatedAt();
        dto.updatedAt = order.getUpdatedAt();
        return dto;
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public List<OrderItemDTO> getItems() { return items; }
    public long getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getPaymentChannel() { return paymentChannel; }
    public String getTransactionId() { return transactionId; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    public static class OrderItemDTO {
        public String productId;
        public String productName;
        public int quantity;
        public long unitPrice;

        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public long getUnitPrice() { return unitPrice; }
    }
}
