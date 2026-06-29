package com.solra.mon.domain.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟物品领域实体。
 * 可购买的虚拟商品（皮肤、模板、表情、礼物等）。
 *
 * <p>MON-002: 支持分类浏览、3D试穿/试摆放、7天未使用退款。
 */
public class VirtualItem {

    public enum VirtualItemType {
        AVATAR_SKIN, SPACE_TEMPLATE, EMOTE, GIFT, EFFECT, BADGE
    }

    /** 物品上架状态。 */
    public enum ItemStatus {
        DRAFT, PUBLISHED, SUSPENDED, DELETED
    }

    private String itemId;
    private String name;
    private String description;
    private VirtualItemType type;
    private ItemStatus status = ItemStatus.PUBLISHED;
    private long price;                 // 单位：分
    private VirtualWallet.CurrencyType currency;
    private boolean isLimited;
    private long availableUntil;        // 0=永久
    /** 3D预览模型URL，支持试穿/试摆放。 */
    private String previewUrl;
    /** 缩略图URL。 */
    private String thumbnailUrl;
    /** 是否支持7天未使用退款。 */
    private boolean refundable = true;
    /** 已售数量。 */
    private long soldCount;
    private List<String> tags;
    private String creatorId;           // 创作者ID（MON-003）

    public VirtualItem(String itemId, String name, VirtualItemType type,
                        long price, VirtualWallet.CurrencyType currency) {
        this.itemId = itemId;
        this.name = name;
        this.type = type;
        this.price = price;
        this.currency = currency;
        this.tags = new ArrayList<>();
    }

    public boolean isAvailable() {
        if (status != ItemStatus.PUBLISHED) return false;
        if (!isLimited) return true;
        return availableUntil == 0 || System.currentTimeMillis() < availableUntil;
    }

    /** 是否支持退款（7天内未使用）。 */
    public boolean isRefundable() {
        return refundable && isAvailable();
    }

    /** 增加销量。 */
    public void incrementSoldCount(int quantity) {
        this.soldCount += quantity;
    }

    /** 下架商品。 */
    public void suspend() {
        this.status = ItemStatus.SUSPENDED;
    }

    /** 重新上架。 */
    public void publish() {
        this.status = ItemStatus.PUBLISHED;
    }

    // Getters and setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public VirtualItemType getType() { return type; }
    public void setType(VirtualItemType type) { this.type = type; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }
    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }
    public VirtualWallet.CurrencyType getCurrency() { return currency; }
    public void setCurrency(VirtualWallet.CurrencyType currency) { this.currency = currency; }
    public boolean isLimited() { return isLimited; }
    public void setLimited(boolean limited) { isLimited = limited; }
    public long getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(long availableUntil) { this.availableUntil = availableUntil; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public boolean isRefundable() { return refundable; }
    public void setRefundable(boolean refundable) { this.refundable = refundable; }
    public long getSoldCount() { return soldCount; }
    public void setSoldCount(long soldCount) { this.soldCount = soldCount; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getCreatorId() { return creatorId; }
    public void setCreatorId(String creatorId) { this.creatorId = creatorId; }
}
