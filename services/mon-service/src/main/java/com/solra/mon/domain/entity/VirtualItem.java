package com.solra.mon.domain.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 虚拟物品领域实体。
 * 可购买的虚拟商品（皮肤、模板、表情、礼物等）。
 */
public class VirtualItem {

    public enum VirtualItemType {
        AVATAR_SKIN, SPACE_TEMPLATE, EMOTE, GIFT, EFFECT, BADGE
    }

    private String itemId;
    private String name;
    private String description;
    private VirtualItemType type;
    private long price;                 // 单位：分
    private VirtualWallet.CurrencyType currency;
    private boolean isLimited;
    private long availableUntil;        // 0=永久
    private String previewUrl;
    private List<String> tags;

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
        if (!isLimited) return true;
        return availableUntil == 0 || System.currentTimeMillis() < availableUntil;
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
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
