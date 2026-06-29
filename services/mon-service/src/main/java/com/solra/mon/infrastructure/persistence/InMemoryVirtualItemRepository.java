package com.solra.mon.infrastructure.persistence;

import com.solra.mon.domain.entity.VirtualItem;
import com.solra.mon.domain.repository.VirtualItemRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 虚拟物品仓储内存实现。
 */
public class InMemoryVirtualItemRepository implements VirtualItemRepository {

    private final Map<String, VirtualItem> store = new ConcurrentHashMap<>();

    @Override
    public Optional<VirtualItem> findById(String itemId) {
        return Optional.ofNullable(store.get(itemId));
    }

    @Override
    public List<VirtualItem> findByType(VirtualItem.VirtualItemType type, int offset, int limit) {
        return store.values().stream()
                .filter(i -> type == null || i.getType() == type)
                .sorted(Comparator.comparingLong(VirtualItem::getPrice))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long countByType(VirtualItem.VirtualItemType type) {
        return store.values().stream()
                .filter(i -> type == null || i.getType() == type)
                .count();
    }

    @Override
    public VirtualItem save(VirtualItem item) {
        store.put(item.getItemId(), item);
        return item;
    }
}
