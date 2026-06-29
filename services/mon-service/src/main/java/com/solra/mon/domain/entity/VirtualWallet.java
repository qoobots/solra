package com.solra.mon.domain.entity;

import java.util.EnumMap;
import java.util.Map;

/**
 * 虚拟钱包领域实体。
 * 管理用户的多币种余额（金币、钻石、信仰积分）。
 */
public class VirtualWallet {

    public enum CurrencyType {
        GOLD, DIAMOND, FAITH_POINT
    }

    private String userId;
    private Map<CurrencyType, Long> balances;
    private long updatedAt;

    public VirtualWallet(String userId) {
        this.userId = userId;
        this.balances = new EnumMap<>(CurrencyType.class);
        for (CurrencyType type : CurrencyType.values()) {
            this.balances.put(type, 0L);
        }
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 增加余额。
     * @throws IllegalArgumentException 如果金额为负
     */
    public void addBalance(CurrencyType currency, long amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        balances.put(currency, balances.get(currency) + amount);
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * 扣除余额。
     * @throws IllegalStateException 如果余额不足
     */
    public void deductBalance(CurrencyType currency, long amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        long current = balances.get(currency);
        if (current < amount) {
            throw new IllegalStateException(
                    "Insufficient balance: " + currency + " has " + current + ", need " + amount);
        }
        balances.put(currency, current - amount);
        this.updatedAt = System.currentTimeMillis();
    }

    public long getBalance(CurrencyType currency) {
        return balances.getOrDefault(currency, 0L);
    }

    // Getters
    public String getUserId() { return userId; }
    public Map<CurrencyType, Long> getBalances() { return balances; }
    public long getUpdatedAt() { return updatedAt; }
}
