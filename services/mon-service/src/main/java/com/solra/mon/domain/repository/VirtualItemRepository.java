package com.solra.mon.domain.repository;

import com.solra.mon.domain.entity.VirtualItem;
import java.util.List;
import java.util.Optional;

/**
 * 虚拟物品仓储接口。
 */
public interface VirtualItemRepository {

    Optional<VirtualItem> findById(String itemId);

    List<VirtualItem> findByType(VirtualItem.VirtualItemType type, int offset, int limit);

    long countByType(VirtualItem.VirtualItemType type);

    VirtualItem save(VirtualItem item);
}
