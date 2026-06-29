package com.solra.mon.domain.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户背包领域实体。
 * 管理用户拥有的虚拟物品清单。
 */
public class UserInventory {

    private String userId;
    private List<InventoryEntry> entries;

    public UserInventory(String userId) {
        this.userId = userId;
        this.entries = new ArrayList<>();
    }

    public void addItem(String itemId, int quantity, long expiresAt) {
        Optional<InventoryEntry> existing = entries.stream()
                .filter(e -> e.getItemId().equals(itemId))
                .findFirst();
        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
        } else {
            entries.add(new InventoryEntry(itemId, quantity, expiresAt));
        }
    }

    public boolean hasItem(String itemId) {
        return entries.stream()
                .anyMatch(e -> e.getItemId().equals(itemId) && e.getQuantity() > 0
                        && (e.getExpiresAt() == 0 || System.currentTimeMillis() < e.getExpiresAt()));
    }

    public int getItemQuantity(String itemId) {
        return entries.stream()
                .filter(e -> e.getItemId().equals(itemId))
                .mapToInt(InventoryEntry::getQuantity)
                .sum();
    }

    public String getUserId() { return userId; }
    public List<InventoryEntry> getEntries() { return entries; }

    public static class InventoryEntry {
        private String itemId;
        private int quantity;
        private long acquiredAt;
        private long expiresAt;     // 0=永久

        public InventoryEntry(String itemId, int quantity, long expiresAt) {
            this.itemId = itemId;
            this.quantity = quantity;
            this.acquiredAt = System.currentTimeMillis();
            this.expiresAt = expiresAt;
        }

        public void addQuantity(int qty) { this.quantity += qty; }

        public String getItemId() { return itemId; }
        public int getQuantity() { return quantity; }
        public long getAcquiredAt() { return acquiredAt; }
        public long getExpiresAt() { return expiresAt; }
    }
}
