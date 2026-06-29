package com.solra.mon.application.dto;

import com.solra.mon.domain.entity.VirtualItem;
import java.util.List;

/**
 * 虚拟物品 DTO。
 */
public class VirtualItemDTO {

    private String itemId;
    private String name;
    private String description;
    private String type;
    private long price;
    private String currency;
    private boolean isLimited;
    private long availableUntil;
    private String previewUrl;
    private List<String> tags;

    public static VirtualItemDTO from(VirtualItem item) {
        VirtualItemDTO dto = new VirtualItemDTO();
        dto.itemId = item.getItemId();
        dto.name = item.getName();
        dto.description = item.getDescription();
        dto.type = item.getType().name();
        dto.price = item.getPrice();
        dto.currency = item.getCurrency().name();
        dto.isLimited = item.isLimited();
        dto.availableUntil = item.getAvailableUntil();
        dto.previewUrl = item.getPreviewUrl();
        dto.tags = item.getTags();
        return dto;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public long getPrice() { return price; }
    public String getCurrency() { return currency; }
    public boolean isLimited() { return isLimited; }
    public long getAvailableUntil() { return availableUntil; }
    public String getPreviewUrl() { return previewUrl; }
    public List<String> getTags() { return tags; }
}
