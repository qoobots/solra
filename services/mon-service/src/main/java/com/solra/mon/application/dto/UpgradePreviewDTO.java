package com.solra.mon.application.dto;

/**
 * 升级预览 DTO。
 * 用于前端展示升级差价。
 */
public class UpgradePreviewDTO {

    private String currentTier;
    private String targetTier;
    private long upgradePrice;   // 单位：分
    private String currency;

    public String getCurrentTier() { return currentTier; }
    public void setCurrentTier(String currentTier) { this.currentTier = currentTier; }
    public String getTargetTier() { return targetTier; }
    public void setTargetTier(String targetTier) { this.targetTier = targetTier; }
    public long getUpgradePrice() { return upgradePrice; }
    public void setUpgradePrice(long upgradePrice) { this.upgradePrice = upgradePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
