package com.solra.mon.infrastructure.payment;

import java.util.List;
import java.util.Optional;

/**
 * 支付交易流水仓储接口。
 */
public interface PaymentTransactionRepository {

    Optional<PaymentTransaction> findById(String transactionId);

    Optional<PaymentTransaction> findByOrderId(String orderId);

    List<PaymentTransaction> findByChannelId(String channelId);

    PaymentTransaction save(PaymentTransaction transaction);
}
