package com.solra.crt.domain.entity;

import java.time.Instant;

/**
 * 模板购买记录值对象 (CRT-003)。
 * 记录用户购买/获取模板的完整交易信息。
 */
public class TemplatePurchase {

    public enum PurchaseStatus {
        PENDING, COMPLETED, REFUNDED, EXPIRED
    }

    private String purchaseId;
    private String productId;
    private String templateId;
    private String buyerId;
    private String sellerId;
    private int paidCents;
    private String currency;
    private PurchaseStatus status;
    private String licenseKey;
    private Instant purchasedAt;
    private Instant expiresAt;    // 订阅到期时间（仅SUBSCRIPTION）

    public TemplatePurchase() {
        this.status = PurchaseStatus.PENDING;
        this.purchasedAt = Instant.now();
        this.currency = "CNY";
    }

    public void complete() {
        this.status = PurchaseStatus.COMPLETED;
    }

    public void refund() {
        this.status = PurchaseStatus.REFUNDED;
    }

    public boolean isActive() {
        if (status != PurchaseStatus.COMPLETED) return false;
        if (expiresAt != null) return expiresAt.isAfter(Instant.now());
        return true;
    }

    // ── Getters and Setters ──

    public String getPurchaseId() { return purchaseId; }
    public void setPurchaseId(String purchaseId) { this.purchaseId = purchaseId; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public int getPaidCents() { return paidCents; }
    public void setPaidCents(int paidCents) { this.paidCents = paidCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PurchaseStatus getStatus() { return status; }
    public void setStatus(PurchaseStatus status) { this.status = status; }
    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Instant purchasedAt) { this.purchasedAt = purchasedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
