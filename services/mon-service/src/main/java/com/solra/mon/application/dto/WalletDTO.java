package com.solra.mon.application.dto;

import com.solra.mon.domain.entity.VirtualWallet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 钱包 DTO。
 */
public class WalletDTO {

    private String userId;
    private Map<String, Long> balances;
    private long updatedAt;

    public static WalletDTO from(VirtualWallet wallet) {
        WalletDTO dto = new WalletDTO();
        dto.userId = wallet.getUserId();
        dto.balances = wallet.getBalances().entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));
        dto.updatedAt = wallet.getUpdatedAt();
        return dto;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Map<String, Long> getBalances() { return balances; }
    public void setBalances(Map<String, Long> balances) { this.balances = balances; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
