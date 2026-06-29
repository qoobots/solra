package com.solra.mon.infrastructure.persistence;

import com.solra.mon.domain.entity.Subscription;
import com.solra.mon.domain.repository.SubscriptionRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 订阅仓储内存实现。
 */
public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<String, Subscription> byId = new ConcurrentHashMap<>();
    private final Map<String, Subscription> byUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<Subscription> findById(String subscriptionId) {
        return Optional.ofNullable(byId.get(subscriptionId));
    }

    @Override
    public Optional<Subscription> findByUserId(String userId) {
        return Optional.ofNullable(byUserId.get(userId));
    }

    @Override
    public List<Subscription> findByStatus(Subscription.SubscriptionStatus status) {
        return byId.values().stream()
                .filter(s -> s.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findExpiringBefore(long timestamp) {
        return byId.values().stream()
                .filter(s -> s.getExpireAt() < timestamp && s.isActive())
                .collect(Collectors.toList());
    }

    @Override
    public Subscription save(Subscription subscription) {
        byId.put(subscription.getSubscriptionId(), subscription);
        byUserId.put(subscription.getUserId(), subscription);
        return subscription;
    }

    @Override
    public void delete(Subscription subscription) {
        byId.remove(subscription.getSubscriptionId());
        byUserId.remove(subscription.getUserId());
    }
}
