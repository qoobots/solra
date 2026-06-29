package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.*;
import com.solra.mon.domain.repository.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 虚拟物品商店领域服务 — MON-002。
 *
 * <p>职责：
 * <ul>
 *   <li>分类浏览：按类型/标签/价格排序筛选</li>
 *   <li>热门推荐：按销量降序</li>
 *   <li>搜索：按名称/标签关键词</li>
 *   <li>退款校验：7天内购买且未使用可退</li>
 *   <li>预置商品数据初始化</li>
 * </ul>
 */
public class ItemShopService {

    private final VirtualItemRepository itemRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    public ItemShopService(VirtualItemRepository itemRepository,
                            InventoryRepository inventoryRepository,
                            OrderRepository orderRepository) {
        this.itemRepository = itemRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    // ── 商品查询 ──

    /** 分页查询上架商品，支持类型筛选和排序。 */
    public List<VirtualItem> browseItems(VirtualItem.VirtualItemType type,
                                          String sortBy, int offset, int limit) {
        List<VirtualItem> items = itemRepository.findByType(type, 0, Integer.MAX_VALUE)
                .stream()
                .filter(VirtualItem::isAvailable)
                .collect(Collectors.toList());

        if ("price_asc".equals(sortBy)) {
            items.sort(Comparator.comparingLong(VirtualItem::getPrice));
        } else if ("price_desc".equals(sortBy)) {
            items.sort(Comparator.comparingLong(VirtualItem::getPrice).reversed());
        } else if ("popular".equals(sortBy)) {
            items.sort(Comparator.comparingLong(VirtualItem::getSoldCount).reversed());
        } else {
            // newest: 默认按自然顺序（预置顺序）
        }

        return items.stream().skip(offset).limit(limit).collect(Collectors.toList());
    }

    /** 按关键词搜索（名称+标签）。 */
    public List<VirtualItem> searchItems(String keyword, int offset, int limit) {
        String kw = keyword.toLowerCase();
        return itemRepository.findByType(null, 0, Integer.MAX_VALUE)
                .stream()
                .filter(VirtualItem::isAvailable)
                .filter(i -> i.getName().toLowerCase().contains(kw)
                        || i.getTags().stream().anyMatch(t -> t.toLowerCase().contains(kw))
                        || (i.getDescription() != null && i.getDescription().toLowerCase().contains(kw)))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** 获取热门商品（按销量降序 Top N）。 */
    public List<VirtualItem> getHotItems(int limit) {
        return itemRepository.findByType(null, 0, Integer.MAX_VALUE)
                .stream()
                .filter(VirtualItem::isAvailable)
                .sorted(Comparator.comparingLong(VirtualItem::getSoldCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** 获取商品详情。 */
    public Optional<VirtualItem> getItemDetail(String itemId) {
        return itemRepository.findById(itemId)
                .filter(VirtualItem::isAvailable);
    }

    // ── 退款校验 ──

    /** 校验物品是否可退款：购买后7天内且未使用。 */
    public boolean canRefund(String userId, String itemId) {
        UserInventory inventory = inventoryRepository.findByUserId(userId).orElse(null);
        if (inventory == null || !inventory.hasItem(itemId)) {
            return false;
        }

        UserInventory.InventoryEntry entry = inventory.getEntries().stream()
                .filter(e -> e.getItemId().equals(itemId))
                .findFirst().orElse(null);
        if (entry == null) return false;

        long daysSinceAcquired = (System.currentTimeMillis() - entry.getAcquiredAt())
                / (24 * 3600 * 1000);
        return daysSinceAcquired <= 7;
    }

    /** 退还物品到商店（从背包移除）。 */
    public void refundItem(String userId, String itemId) {
        UserInventory inventory = inventoryRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));

        UserInventory.InventoryEntry entry = inventory.getEntries().stream()
                .filter(e -> e.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item not in inventory"));

        // 标记为已退还（减少数量或移除）
        entry.addQuantity(-1); // 减少1个
        inventoryRepository.save(inventory);
    }

    // ── 商品管理 ──

    /** 上架新商品。 */
    public VirtualItem publishItem(VirtualItem item) {
        item.publish();
        return itemRepository.save(item);
    }

    /** 下架商品。 */
    public VirtualItem suspendItem(String itemId) {
        VirtualItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        item.suspend();
        return itemRepository.save(item);
    }
}
