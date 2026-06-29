package com.solra.mon.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserInventory 实体单元测试。
 * 验证背包物品管理：添加/合并/查询/过期判断。
 */
@DisplayName("UserInventory — 背包实体测试")
class UserInventoryTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("创建后 entries 为空")
        void initialEntriesEmpty() {
            UserInventory inv = new UserInventory("user-1");
            assertTrue(inv.getEntries().isEmpty());
        }

        @Test
        @DisplayName("userId 正确")
        void userIdCorrect() {
            UserInventory inv = new UserInventory("user-1");
            assertEquals("user-1", inv.getUserId());
        }
    }

    @Nested
    @DisplayName("addItem() — 添加物品")
    class AddItem {

        @Test
        @DisplayName("添加新物品")
        void addNewItem() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 1, 0);

            assertEquals(1, inv.getEntries().size());
            assertTrue(inv.hasItem("item-1"));
        }

        @Test
        @DisplayName("添加已存在物品合并数量")
        void addExistingMergesQuantity() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 1, 0);
            inv.addItem("item-1", 2, 0);

            assertEquals(1, inv.getEntries().size());
            assertEquals(3, inv.getItemQuantity("item-1"));
        }

        @Test
        @DisplayName("添加多个不同物品")
        void addMultipleDifferentItems() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 1, 0);
            inv.addItem("item-2", 3, 0);
            inv.addItem("item-3", 5, 0);

            assertEquals(3, inv.getEntries().size());
            assertTrue(inv.hasItem("item-1"));
            assertTrue(inv.hasItem("item-2"));
            assertTrue(inv.hasItem("item-3"));
        }
    }

    @Nested
    @DisplayName("hasItem() — 检查是否拥有")
    class HasItem {

        @Test
        @DisplayName("拥有且数量>0返回 true")
        void hasItemWithQuantity() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 5, 0);
            assertTrue(inv.hasItem("item-1"));
        }

        @Test
        @DisplayName("不存在的物品返回 false")
        void notOwnedReturnsFalse() {
            UserInventory inv = new UserInventory("user-1");
            assertFalse(inv.hasItem("non-existent"));
        }

        @Test
        @DisplayName("永久物品(expiresAt=0)返回 true")
        void permanentItemReturnsTrue() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 1, 0);
            assertTrue(inv.hasItem("item-1"));
        }

        @Test
        @DisplayName("未过期的限时物品返回 true")
        void notExpiredItemReturnsTrue() {
            UserInventory inv = new UserInventory("user-1");
            long futureExpiry = System.currentTimeMillis() + 86400000L; // 1天后
            inv.addItem("item-1", 1, futureExpiry);
            assertTrue(inv.hasItem("item-1"));
        }

        @Test
        @DisplayName("已过期的限时物品返回 false")
        void expiredItemReturnsFalse() {
            UserInventory inv = new UserInventory("user-1");
            long pastExpiry = System.currentTimeMillis() - 86400000L; // 1天前
            inv.addItem("item-1", 1, pastExpiry);
            assertFalse(inv.hasItem("item-1"));
        }

        @Test
        @DisplayName("数量为0的物品返回 false")
        void zeroQuantityReturnsFalse() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 0, 0);
            assertFalse(inv.hasItem("item-1"));
        }
    }

    @Nested
    @DisplayName("getItemQuantity() — 获取数量")
    class GetItemQuantity {

        @Test
        @DisplayName("单个物品数量")
        void singleItemQuantity() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 10, 0);
            assertEquals(10, inv.getItemQuantity("item-1"));
        }

        @Test
        @DisplayName("多次添加合并后数量")
        void mergedQuantity() {
            UserInventory inv = new UserInventory("user-1");
            inv.addItem("item-1", 5, 0);
            inv.addItem("item-1", 3, 0);
            inv.addItem("item-1", 2, 0);
            assertEquals(10, inv.getItemQuantity("item-1"));
        }

        @Test
        @DisplayName("不存在的物品返回 0")
        void nonExistentReturnsZero() {
            UserInventory inv = new UserInventory("user-1");
            assertEquals(0, inv.getItemQuantity("non-existent"));
        }
    }

    @Nested
    @DisplayName("InventoryEntry")
    class InventoryEntryTest {

        @Test
        @DisplayName("创建 InventoryEntry")
        void createEntry() {
            UserInventory.InventoryEntry entry = new UserInventory.InventoryEntry(
                    "item-1", 3, 0);

            assertEquals("item-1", entry.getItemId());
            assertEquals(3, entry.getQuantity());
            assertEquals(0L, entry.getExpiresAt());
        }

        @Test
        @DisplayName("addQuantity 增加数量")
        void addQuantity() {
            UserInventory.InventoryEntry entry = new UserInventory.InventoryEntry(
                    "item-1", 3, 0);
            entry.addQuantity(5);
            assertEquals(8, entry.getQuantity());
        }
    }
}
