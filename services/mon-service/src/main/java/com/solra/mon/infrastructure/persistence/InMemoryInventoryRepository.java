package com.solra.mon.infrastructure.persistence;

import com.solra.mon.domain.entity.UserInventory;
import com.solra.mon.domain.repository.InventoryRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 背包仓储内存实现。
 */
public class InMemoryInventoryRepository implements InventoryRepository {

    private final Map<String, UserInventory> store = new ConcurrentHashMap<>();

    @Override
    public Optional<UserInventory> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public UserInventory save(UserInventory inventory) {
        store.put(inventory.getUserId(), inventory);
        return inventory;
    }
}
