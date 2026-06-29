package com.solra.mon.domain.repository;

import com.solra.mon.domain.entity.Subscription;
import java.util.List;
import java.util.Optional;

/**
 * 订阅仓储接口。
 */
public interface SubscriptionRepository {

    Optional<Subscription> findById(String subscriptionId);

    Optional<Subscription> findByUserId(String userId);

    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);

    List<Subscription> findExpiringBefore(long timestamp);

    Subscription save(Subscription subscription);

    void delete(Subscription subscription);
}
