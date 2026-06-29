package com.solra.mon.infrastructure.payment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 支付交易流水仓储内存实现。
 */
public class InMemoryPaymentTransactionRepository implements PaymentTransactionRepository {

    private final Map<String, PaymentTransaction> byId = new ConcurrentHashMap<>();
    private final Map<String, PaymentTransaction> byOrderId = new ConcurrentHashMap<>();

    @Override
    public Optional<PaymentTransaction> findById(String transactionId) {
        return Optional.ofNullable(byId.get(transactionId));
    }

    @Override
    public Optional<PaymentTransaction> findByOrderId(String orderId) {
        return Optional.ofNullable(byOrderId.get(orderId));
    }

    @Override
    public List<PaymentTransaction> findByChannelId(String channelId) {
        return byId.values().stream()
                .filter(t -> channelId.equals(t.getChannelId()))
                .collect(Collectors.toList());
    }

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        byId.put(transaction.getTransactionId(), transaction);
        byOrderId.put(transaction.getOrderId(), transaction);
        return transaction;
    }
}
