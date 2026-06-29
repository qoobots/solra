package com.solra.mon.application.dto;

import java.util.List;

/**
 * 订阅计划 DTO。
 */
public class SubscriptionPlanDTO {

    private String tier;
    private String name;
    private String description;
    private long pricePerMonth;
    private long pricePerYear;
    private String currency;
    private List<String> benefits;
    private boolean yearlyAvailable;

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getPricePerMonth() { return pricePerMonth; }
    public void setPricePerMonth(long pricePerMonth) { this.pricePerMonth = pricePerMonth; }
    public long getPricePerYear() { return pricePerYear; }
    public void setPricePerYear(long pricePerYear) { this.pricePerYear = pricePerYear; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits) { this.benefits = benefits; }
    public boolean isYearlyAvailable() { return yearlyAvailable; }
    public void setYearlyAvailable(boolean yearlyAvailable) { this.yearlyAvailable = yearlyAvailable; }
}
