package com.solra.crt.domain.entity;

import java.time.Instant;

/**
 * 模板商品值对象 (CRT-003)。
 * 将模板包装为可交易商品，包含定价、许可和交易信息。
 */
public class TemplateProduct {

    public enum PricingModel {
        FREE, ONE_TIME, SUBSCRIPTION
    }

    public enum LicenseType {
        PERSONAL, COMMERCIAL, ENTERPRISE
    }

    private String productId;
    private String templateId;
    private String sellerId;
    private PricingModel pricingModel;
    private LicenseType licenseType;
    private int priceCents;         // 价格（分）
    private String currency;        // 货币代码
    private int subscriptionDays;   // 订阅天数（仅SUBSCRIPTION）
    private boolean allowDerivative;
    private boolean requireAttribution;
    private int totalSales;
    private int totalRevenueCents;
    private float avgRating;
    private int reviewCount;
    private Instant listedAt;
    private Instant updatedAt;

    public TemplateProduct() {
        this.pricingModel = PricingModel.FREE;
        this.licenseType = LicenseType.PERSONAL;
        this.currency = "CNY";
        this.allowDerivative = true;
        this.requireAttribution = true;
        this.totalSales = 0;
        this.totalRevenueCents = 0;
        this.avgRating = 0.0f;
        this.reviewCount = 0;
        this.listedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * 记录一次销售。
     */
    public void recordSale(int paidCents) {
        this.totalSales++;
        this.totalRevenueCents += paidCents;
    }

    /**
     * 更新平均评分。
     */
    public void addReview(float rating) {
        float totalScore = this.avgRating * this.reviewCount + rating;
        this.reviewCount++;
        this.avgRating = Math.round(totalScore / this.reviewCount * 10f) / 10f;
    }

    public boolean isFree() {
        return pricingModel == PricingModel.FREE || priceCents == 0;
    }

    // ── Getters and Setters ──

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public PricingModel getPricingModel() { return pricingModel; }
    public void setPricingModel(PricingModel pricingModel) { this.pricingModel = pricingModel; }
    public LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(LicenseType licenseType) { this.licenseType = licenseType; }
    public int getPriceCents() { return priceCents; }
    public void setPriceCents(int priceCents) { this.priceCents = priceCents; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getSubscriptionDays() { return subscriptionDays; }
    public void setSubscriptionDays(int subscriptionDays) { this.subscriptionDays = subscriptionDays; }
    public boolean isAllowDerivative() { return allowDerivative; }
    public void setAllowDerivative(boolean allowDerivative) { this.allowDerivative = allowDerivative; }
    public boolean isRequireAttribution() { return requireAttribution; }
    public void setRequireAttribution(boolean requireAttribution) { this.requireAttribution = requireAttribution; }
    public int getTotalSales() { return totalSales; }
    public int getTotalRevenueCents() { return totalRevenueCents; }
    public float getAvgRating() { return avgRating; }
    public int getReviewCount() { return reviewCount; }
    public Instant getListedAt() { return listedAt; }
    public void setListedAt(Instant listedAt) { this.listedAt = listedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
