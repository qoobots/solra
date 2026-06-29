package com.solra.mon.domain.repository;

import com.solra.mon.domain.entity.Order;
import java.util.List;
import java.util.Optional;

/**
 * 订单仓储接口。
 */
public interface OrderRepository {

    Optional<Order> findById(String orderId);

    List<Order> findByUserId(String userId);

    Order save(Order order);
}
