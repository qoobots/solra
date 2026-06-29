package com.solra.mon.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VirtualItem 实体单元测试。
 * 验证虚拟物品的可用性判断。
 */
@DisplayName("VirtualItem — 虚拟物品测试")
class VirtualItemTest {

    @Nested
    @DisplayName("isAvailable() — 可用性判断")
    class IsAvailable {

        @Test
        @DisplayName("非限量物品始终可用")
        void nonLimitedAlwaysAvailable() {
            VirtualItem item = new VirtualItem("item-1", "Skin",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.GOLD);
            assertTrue(item.isAvailable());
        }

        @Test
        @DisplayName("限量物品未过期可用")
        void limitedNotExpiredAvailable() {
            VirtualItem item = new VirtualItem("item-1", "Limited Skin",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.GOLD);
            item.setLimited(true);
            item.setAvailableUntil(System.currentTimeMillis() + 86400000L);
            assertTrue(item.isAvailable());
        }

        @Test
        @DisplayName("限量物品永久有效(availableUntil=0)")
        void limitedPermanentAvailable() {
            VirtualItem item = new VirtualItem("item-1", "Permanent Limited",
                    VirtualItem.VirtualItemType.BADGE, 50L,
                    VirtualWallet.CurrencyType.FAITH_POINT);
            item.setLimited(true);
            item.setAvailableUntil(0L);
            assertTrue(item.isAvailable());
        }

        @Test
        @DisplayName("限量物品已过期不可用")
        void limitedExpiredNotAvailable() {
            VirtualItem item = new VirtualItem("item-1", "Expired",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.GOLD);
            item.setLimited(true);
            item.setAvailableUntil(System.currentTimeMillis() - 86400000L);
            assertFalse(item.isAvailable());
        }
    }

    @Nested
    @DisplayName("物品类型")
    class ItemTypes {

        @Test
        @DisplayName("AVATAR_SKIN 类型")
        void avatarSkin() {
            VirtualItem item = new VirtualItem("i1", "Skin",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.GOLD);
            assertEquals(VirtualItem.VirtualItemType.AVATAR_SKIN, item.getType());
        }

        @Test
        @DisplayName("SPACE_TEMPLATE 类型")
        void spaceTemplate() {
            VirtualItem item = new VirtualItem("i2", "Template",
                    VirtualItem.VirtualItemType.SPACE_TEMPLATE, 200L,
                    VirtualWallet.CurrencyType.DIAMOND);
            assertEquals(VirtualItem.VirtualItemType.SPACE_TEMPLATE, item.getType());
        }

        @Test
        @DisplayName("EMOTE 类型")
        void emote() {
            VirtualItem item = new VirtualItem("i3", "Dance",
                    VirtualItem.VirtualItemType.EMOTE, 50L,
                    VirtualWallet.CurrencyType.GOLD);
            assertEquals(VirtualItem.VirtualItemType.EMOTE, item.getType());
        }

        @Test
        @DisplayName("GIFT 类型")
        void gift() {
            VirtualItem item = new VirtualItem("i4", "Rose",
                    VirtualItem.VirtualItemType.GIFT, 10L,
                    VirtualWallet.CurrencyType.DIAMOND);
            assertEquals(VirtualItem.VirtualItemType.GIFT, item.getType());
        }

        @Test
        @DisplayName("EFFECT 类型")
        void effect() {
            VirtualItem item = new VirtualItem("i5", "Glow",
                    VirtualItem.VirtualItemType.EFFECT, 30L,
                    VirtualWallet.CurrencyType.FAITH_POINT);
            assertEquals(VirtualItem.VirtualItemType.EFFECT, item.getType());
        }

        @Test
        @DisplayName("BADGE 类型")
        void badge() {
            VirtualItem item = new VirtualItem("i6", "VIP",
                    VirtualItem.VirtualItemType.BADGE, 0L,
                    VirtualWallet.CurrencyType.FAITH_POINT);
            assertEquals(VirtualItem.VirtualItemType.BADGE, item.getType());
        }
    }

    @Nested
    @DisplayName("价格和货币")
    class PriceAndCurrency {

        @Test
        @DisplayName("价格和货币正确")
        void priceAndCurrency() {
            VirtualItem item = new VirtualItem("i1", "Sword",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 150L,
                    VirtualWallet.CurrencyType.DIAMOND);

            assertEquals(150L, item.getPrice());
            assertEquals(VirtualWallet.CurrencyType.DIAMOND, item.getCurrency());
        }

        @Test
        @DisplayName("免费物品")
        void freeItem() {
            VirtualItem item = new VirtualItem("i1", "Free Badge",
                    VirtualItem.VirtualItemType.BADGE, 0L,
                    VirtualWallet.CurrencyType.GOLD);

            assertEquals(0L, item.getPrice());
            assertTrue(item.isAvailable());
        }
    }

    @Nested
    @DisplayName("商品状态 — ItemStatus")
    class ItemStatus {

        @Test
        @DisplayName("新建商品默认为 PUBLISHED")
        void defaultStatusIsPublished() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            assertEquals(VirtualItem.ItemStatus.PUBLISHED, item.getStatus());
            assertTrue(item.isAvailable());
        }

        @Test
        @DisplayName("下架后不可用")
        void suspendedNotAvailable() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            item.suspend();
            assertEquals(VirtualItem.ItemStatus.SUSPENDED, item.getStatus());
            assertFalse(item.isAvailable());
        }

        @Test
        @DisplayName("重新上架后可用")
        void republishAvailable() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            item.suspend();
            item.publish();
            assertTrue(item.isAvailable());
        }
    }

    @Nested
    @DisplayName("退款和销量")
    class RefundAndSales {

        @Test
        @DisplayName("默认可退款")
        void defaultRefundable() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            assertTrue(item.isRefundable());
        }

        @Test
        @DisplayName("可设置为不可退款")
        void canSetNonRefundable() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            item.setRefundable(false);
            assertFalse(item.isRefundable());
        }

        @Test
        @DisplayName("增加销量")
        void incrementSoldCount() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            item.incrementSoldCount(5);
            assertEquals(5, item.getSoldCount());
            item.incrementSoldCount(3);
            assertEquals(8, item.getSoldCount());
        }

        @Test
        @DisplayName("缩略图和预览URL")
        void thumbnailAndPreview() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            item.setPreviewUrl("https://cdn.solra.com/3d/item1.glb");
            item.setThumbnailUrl("https://cdn.solra.com/thumb/item1.png");
            assertEquals("https://cdn.solra.com/3d/item1.glb", item.getPreviewUrl());
            assertEquals("https://cdn.solra.com/thumb/item1.png", item.getThumbnailUrl());
        }

        @Test
        @DisplayName("创作者ID")
        void creatorId() {
            VirtualItem item = new VirtualItem("i1", "Test",
                    VirtualItem.VirtualItemType.AVATAR_SKIN, 100L,
                    VirtualWallet.CurrencyType.DIAMOND);
            item.setCreatorId("creator-001");
            assertEquals("creator-001", item.getCreatorId());
        }
    }
}
