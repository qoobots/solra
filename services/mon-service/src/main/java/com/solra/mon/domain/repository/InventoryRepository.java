package com.solra.mon.domain.repository;

import com.solra.mon.domain.entity.UserInventory;
import java.util.Optional;

/**
 * 用户背包仓储接口。
 */
public interface InventoryRepository {

    Optional<UserInventory> findByUserId(String userId);

    UserInventory save(UserInventory inventory);
}
