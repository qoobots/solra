package com.solra.mon.infrastructure.payment;

import com.solra.mon.domain.entity.Order;
import com.solra.mon.domain.repository.OrderRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一支付网关 — MON-004。
 *
 * <p>职责：
 * <ul>
 *   <li>根据支付渠道路由到对应的 Provider</li>
 *   <li>创建支付 → 验证回执 → 完成订单</li>
 *   <li>PCI-DSS：使用支付令牌，不存储卡号</li>
 *   <li>支付流水记录</li>
 * </ul>
 */
public class PaymentGateway {

    private final Map<String, PaymentChannel> channels = new ConcurrentHashMap<>();
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;

    public PaymentGateway(OrderRepository orderRepository,
                           PaymentTransactionRepository transactionRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
    }

    /** 注册支付渠道。 */
    public void registerChannel(PaymentChannel channel) {
        channels.put(channel.getChannelId(), channel);
    }

    /** 获取可用渠道列表。 */
    public List<Map<String, String>> listAvailableChannels() {
        List<Map<String, String>> result = new ArrayList<>();
        for (PaymentChannel ch : channels.values()) {
            result.add(Map.of(
                    "channel_id", ch.getChannelId(),
                    "display_name", ch.getDisplayName()
            ));
        }
        return result;
    }

    /**
     * 创建支付（生成预支付凭证）。
     *
     * @param orderId 订单ID
     * @param channelId 支付渠道ID (apple/google/wechat/alipay)
     * @param metadata 渠道特定元数据
     * @return 预支付信息（客户端调起支付用）
     */
    public PaymentChannel.PaymentResult createPayment(String orderId, String channelId,
                                                       Map<String, String> metadata) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not in PENDING state: " + order.getStatus());
        }

        PaymentChannel channel = channels.get(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Unknown payment channel: " + channelId);
        }

        if (!channel.supportsCurrency(order)) {
            throw new IllegalArgumentException(
                    "Channel " + channelId + " does not support currency " + order.getCurrency());
        }

        PaymentChannel.PaymentResult result = channel.createPayment(order, metadata);

        if (result.isSuccess()) {
            // 记录支付交易流水
            PaymentTransaction txn = new PaymentTransaction(
                    orderId, channelId, order.getTotalAmount(),
                    PaymentTransaction.TransactionStatus.PENDING);
            txn.setPlatformOrderId(result.getPlatformOrderId());
            transactionRepository.save(txn);
        }

        return result;
    }

    /**
     * 验证支付回执并完成订单。
     *
     * @param orderId 订单ID
     * @param channelId 支付渠道ID
     * @param receipt 支付平台回执/凭证
     * @return 验证结果
     */
    public PaymentChannel.VerifyResult verifyAndComplete(String orderId, String channelId,
                                                          String receipt) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        PaymentChannel channel = channels.get(channelId);
        if (channel == null) {
            throw new IllegalArgumentException("Unknown payment channel: " + channelId);
        }

        PaymentChannel.VerifyResult result = channel.verifyPayment(orderId, receipt);

        if (result.isVerified()) {
            // 完成订单
            order.complete(channelId, result.getTransactionId());
            orderRepository.save(order);

            // 更新交易流水
            transactionRepository.findByOrderId(orderId).ifPresent(txn -> {
                txn.markCompleted(result.getTransactionId());
                transactionRepository.save(txn);
            });
        } else {
            // 标记交易失败
            transactionRepository.findByOrderId(orderId).ifPresent(txn -> {
                txn.markFailed("receipt_verification_failed");
                transactionRepository.save(txn);
            });
        }

        return result;
    }

    /**
     * 退款。
     * @param orderId 订单ID
     * @param amount 退款金额（分），0=全额退款
     * @param reason 退款原因
     */
    public PaymentChannel.RefundResult processRefund(String orderId, long amount, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getPaymentChannel() == null) {
            throw new IllegalStateException("Order has no payment channel");
        }

        PaymentChannel channel = channels.get(order.getPaymentChannel());
        if (channel == null) {
            throw new IllegalArgumentException("Unknown payment channel: " + order.getPaymentChannel());
        }

        long refundAmount = amount == 0 ? order.getTotalAmount() : amount;
        PaymentChannel.RefundResult result = channel.refund(orderId, refundAmount, reason);

        if (result.isSuccess()) {
            order.refund();
            orderRepository.save(order);
        }

        return result;
    }
}
