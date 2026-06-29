package com.solra.mon.infrastructure.persistence;

import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.repository.SubscriptionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅仓储内存实现。
 */
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<String, Subscription> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Subscription> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public Subscription save(Subscription subscription) {
        store.put(subscription.getUserId(), subscription);
        return subscription;
    }
}
