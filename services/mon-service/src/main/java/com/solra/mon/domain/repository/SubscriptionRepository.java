package com.solra.mon.domain.repository;

import com.solra.mon.domain.entity.Subscription;
import java.util.Optional;

/**
 * 订阅仓储接口。
 */
public interface SubscriptionRepository {

    Optional<Subscription> findByUserId(String userId);

    Subscription save(Subscription subscription);
}
