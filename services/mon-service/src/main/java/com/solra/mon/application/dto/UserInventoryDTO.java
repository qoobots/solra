package com.solra.mon.application.dto;

import com.solra.mon.domain.entity.UserInventory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户背包 DTO。
 */
public class UserInventoryDTO {

    private String userId;
    private List<InventoryEntryDTO> entries;

    public static UserInventoryDTO from(UserInventory inventory) {
        UserInventoryDTO dto = new UserInventoryDTO();
        dto.userId = inventory.getUserId();
        dto.entries = inventory.getEntries().stream().map(e -> {
            InventoryEntryDTO edto = new InventoryEntryDTO();
            edto.itemId = e.getItemId();
            edto.quantity = e.getQuantity();
            edto.acquiredAt = e.getAcquiredAt();
            edto.expiresAt = e.getExpiresAt();
            return edto;
        }).collect(Collectors.toList());
        return dto;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<InventoryEntryDTO> getEntries() { return entries; }
    public void setEntries(List<InventoryEntryDTO> entries) { this.entries = entries; }

    public static class InventoryEntryDTO {
        public String itemId;
        public int quantity;
        public long acquiredAt;
        public long expiresAt;

        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public long getAcquiredAt() { return acquiredAt; }
        public long getExpiresAt() { return expiresAt; }
    }
}
