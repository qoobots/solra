package com.solra.mon.infrastructure.persistence;

import com.solra.mon.domain.entity.Order;
import com.solra.mon.domain.repository.OrderRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 订单仓储内存实现。
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }

    @Override
    public List<Order> findByUserId(String userId) {
        return store.values().stream()
                .filter(o -> userId.equals(o.getUserId()))
                .sorted(Comparator.comparingLong(Order::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Order save(Order order) {
        store.put(order.getOrderId(), order);
        return order;
    }
}
