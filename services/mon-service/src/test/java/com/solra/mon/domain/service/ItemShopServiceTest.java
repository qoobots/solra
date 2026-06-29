package com.solra.mon.domain.service;

import com.solra.mon.domain.entity.*;
import com.solra.mon.domain.repository.*;
import com.solra.mon.infrastructure.persistence.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ItemShopService 单元测试 — MON-002。
 * 验证商品浏览、搜索、热门推荐、退款校验。
 */
@DisplayName("ItemShopService — 虚拟物品商店测试")
class ItemShopServiceTest {

    private ItemShopService service;
    private VirtualItemRepository itemRepo;
    private InventoryRepository inventoryRepo;
    private OrderRepository orderRepo;

    @BeforeEach
    void setUp() {
        itemRepo = new InMemoryVirtualItemRepository();
        inventoryRepo = new InMemoryInventoryRepository();
        orderRepo = new InMemoryOrderRepository();
        service = new ItemShopService(itemRepo, inventoryRepo, orderRepo);

        // 预置测试商品
        seedItems();
    }

    private void seedItems() {
        VirtualItem skin1 = createItem("skin-001", "龙年限定皮肤", VirtualItem.VirtualItemType.AVATAR_SKIN, 9900);
        skin1.setTags(List.of("限定", "龙年", "传说"));
        skin1.setSoldCount(1500);
        itemRepo.save(skin1);

        VirtualItem skin2 = createItem("skin-002", "夏日泳装", VirtualItem.VirtualItemType.AVATAR_SKIN, 2900);
        skin2.setTags(List.of("夏日", "泳装"));
        skin2.setSoldCount(800);
        itemRepo.save(skin2);

        VirtualItem template1 = createItem("tmpl-001", "赛博朋克空间", VirtualItem.VirtualItemType.SPACE_TEMPLATE, 4900);
        template1.setTags(List.of("赛博朋克", "科幻"));
        template1.setSoldCount(2000);
        itemRepo.save(template1);

        VirtualItem emote1 = createItem("emote-001", "爱心表情", VirtualItem.VirtualItemType.EMOTE, 500);
        emote1.setTags(List.of("表情", "爱心"));
        emote1.setSoldCount(5000);
        itemRepo.save(emote1);

        VirtualItem badge1 = createItem("badge-001", "创始者徽章", VirtualItem.VirtualItemType.BADGE, 0);
        badge1.setLimited(true);
        badge1.setRefundable(false);
        badge1.setSoldCount(100);
        itemRepo.save(badge1);
    }

    private VirtualItem createItem(String id, String name, VirtualItem.VirtualItemType type, long price) {
        return new VirtualItem(id, name, type, price, VirtualWallet.CurrencyType.DIAMOND);
    }

    @Nested
    @DisplayName("browseItems — 分类浏览")
    class BrowseItems {

        @Test
        @DisplayName("按类型筛选 AVATAR_SKIN 返回2个")
        void filterByType() {
            List<VirtualItem> items = service.browseItems(
                    VirtualItem.VirtualItemType.AVATAR_SKIN, "newest", 0, 10);
            assertEquals(2, items.size());
            assertTrue(items.stream().allMatch(i -> i.getType() == VirtualItem.VirtualItemType.AVATAR_SKIN));
        }

        @Test
        @DisplayName("按销量排序（popular）")
        void sortByPopular() {
            List<VirtualItem> items = service.browseItems(null, "popular", 0, 10);
            assertTrue(items.get(0).getSoldCount() >= items.get(1).getSoldCount());
        }

        @Test
        @DisplayName("按价格升序")
        void sortByPriceAsc() {
            List<VirtualItem> items = service.browseItems(null, "price_asc", 0, 10);
            assertTrue(items.get(0).getPrice() <= items.get(1).getPrice());
        }

        @Test
        @DisplayName("分页偏移")
        void paginationOffset() {
            List<VirtualItem> page1 = service.browseItems(null, "newest", 0, 2);
            List<VirtualItem> page2 = service.browseItems(null, "newest", 2, 2);
            assertEquals(2, page1.size());
            assertFalse(page2.isEmpty());
        }
    }

    @Nested
    @DisplayName("searchItems — 关键词搜索")
    class SearchItems {

        @Test
        @DisplayName("按名称搜索")
        void searchByName() {
            List<VirtualItem> results = service.searchItems("夏日", 0, 10);
            assertEquals(1, results.size());
            assertEquals("夏日泳装", results.get(0).getName());
        }

        @Test
        @DisplayName("按标签搜索")
        void searchByTag() {
            List<VirtualItem> results = service.searchItems("赛博朋克", 0, 10);
            assertEquals(1, results.size());
            assertEquals("tmpl-001", results.get(0).getItemId());
        }

        @Test
        @DisplayName("无结果")
        void noResults() {
            List<VirtualItem> results = service.searchItems("不存在的商品", 0, 10);
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("getHotItems — 热门推荐")
    class HotItems {

        @Test
        @DisplayName("返回Top N按销量降序")
        void topNBySoldCount() {
            List<VirtualItem> hot = service.getHotItems(3);
            assertEquals(3, hot.size());
            assertTrue(hot.get(0).getSoldCount() >= hot.get(1).getSoldCount());
            assertTrue(hot.get(1).getSoldCount() >= hot.get(2).getSoldCount());
        }
    }

    @Nested
    @DisplayName("退款校验 — canRefund / refundItem")
    class Refund {

        @Test
        @DisplayName("刚购买可退款")
        void recentlyPurchasedCanRefund() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("skin-001", 1, 0);
            inventoryRepo.save(inv);

            assertTrue(service.canRefund("user-1", "skin-001"));
        }

        @Test
        @DisplayName("未拥有的物品不能退款")
        void notOwnedCannotRefund() {
            assertFalse(service.canRefund("user-1", "skin-001"));
        }

        @Test
        @DisplayName("退款后背包移除物品")
        void refundRemovesFromInventory() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("skin-001", 1, 0);
            inventoryRepo.save(inv);

            service.refundItem("user-1", "skin-001");

            UserInventory updated = inventoryRepo.findByUserId("user-1").orElseThrow();
            assertEquals(0, updated.getItemQuantity("skin-001"));
        }
    }

    @Nested
    @DisplayName("商品管理 — publishItem / suspendItem")
    class ItemManagement {

        @Test
        @DisplayName("上架商品状态为 PUBLISHED")
        void publishSetsPublished() {
            VirtualItem item = createItem("new-001", "新品", VirtualItem.VirtualItemType.GIFT, 1000);
            item = service.publishItem(item);
            assertEquals(VirtualItem.ItemStatus.PUBLISHED, item.getStatus());
        }

        @Test
        @DisplayName("下架商品不可购买")
        void suspendedNotAvailable() {
            VirtualItem item = createItem("new-002", "待下架", VirtualItem.VirtualItemType.GIFT, 1000);
            item = service.publishItem(item);
            service.suspendItem("new-002");

            VirtualItem suspended = itemRepo.findById("new-002").orElseThrow();
            assertFalse(suspended.isAvailable());
        }
    }
}
