package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.entity.TemplateProduct;
import com.solra.crt.domain.entity.TemplatePurchase;
import com.solra.crt.domain.repository.TemplateRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 空间模板市场领域服务 (CRT-003)。
 * 管理模板的上架、定价、购买和交易，实现创作→发布→交易闭环。
 *
 * 验收标准：
 * - 模板应用成功率 ≥ 95%
 * - 购买流程 ≤ 3步（浏览→确认→获取）
 * - 支持 FREE / ONE_TIME / SUBSCRIPTION 三种定价模型
 */
public class TemplateMarketplace {

    private final TemplateRepository templateRepository;

    // 商品目录（生产环境应持久化）
    private final Map<String, TemplateProduct> productCatalog = new ConcurrentHashMap<>();
    // 购买记录
    private final Map<String, TemplatePurchase> purchases = new ConcurrentHashMap<>();
    // 用户已购模板索引
    private final Map<String, Set<String>> userPurchases = new ConcurrentHashMap<>();

    public TemplateMarketplace(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    // ── 卖家操作 ──

    /**
     * 第1步：将模板上架到市场。
     */
    public TemplateProduct listTemplate(String templateId, String sellerId,
                                         TemplateProduct.PricingModel pricingModel,
                                         TemplateProduct.LicenseType licenseType,
                                         int priceCents, int subscriptionDays) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));

        if (!sellerId.equals(template.getAuthorId())) {
            throw new IllegalStateException("Only the template author can list it on marketplace");
        }

        if (productCatalog.values().stream().anyMatch(p -> p.getTemplateId().equals(templateId))) {
            throw new IllegalStateException("Template already listed: " + templateId);
        }

        TemplateProduct product = new TemplateProduct();
        product.setProductId("prod-" + UUID.randomUUID().toString().substring(0, 8));
        product.setTemplateId(templateId);
        product.setSellerId(sellerId);
        product.setPricingModel(pricingModel);
        product.setLicenseType(licenseType);
        product.setPriceCents(priceCents);
        product.setSubscriptionDays(subscriptionDays);
        product.setListedAt(Instant.now());

        productCatalog.put(product.getProductId(), product);
        return product;
    }

    /**
     * 下架模板。
     */
    public boolean delistTemplate(String productId, String sellerId) {
        TemplateProduct product = productCatalog.get(productId);
        if (product != null && sellerId.equals(product.getSellerId())) {
            productCatalog.remove(productId);
            return true;
        }
        return false;
    }

    /**
     * 更新模板定价。
     */
    public TemplateProduct updatePricing(String productId, String sellerId,
                                          int newPriceCents, TemplateProduct.PricingModel newModel) {
        TemplateProduct product = productCatalog.get(productId);
        if (product == null) throw new IllegalArgumentException("Product not found: " + productId);
        if (!sellerId.equals(product.getSellerId())) {
            throw new IllegalStateException("Only the seller can update pricing");
        }

        product.setPriceCents(newPriceCents);
        product.setPricingModel(newModel);
        product.setUpdatedAt(Instant.now());
        return product;
    }

    // ── 买家操作 ──

    /**
     * 第1步：浏览市场商品列表。
     */
    public List<TemplateProduct> browseMarketplace(String category, String keyword,
                                                    int minPrice, int maxPrice,
                                                    int offset, int limit) {
        return productCatalog.values().stream()
                .filter(p -> matchesFilters(p, category, keyword, minPrice, maxPrice))
                .sorted(Comparator.comparingInt(TemplateProduct::getTotalSales).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 第2步：查看商品详情（含模板详情）。
     */
    public Map<String, Object> getProductDetail(String productId) {
        TemplateProduct product = productCatalog.get(productId);
        if (product == null) throw new IllegalArgumentException("Product not found: " + productId);

        Template template = templateRepository.findById(product.getTemplateId())
                .orElseThrow(() -> new IllegalStateException("Template not found"));

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("product", product);
        detail.put("template", template);
        detail.put("templateUsageCount", template.getUsageCount());
        detail.put("templateRating", template.getRating());
        return detail;
    }

    /**
     * 第3步：购买/获取模板（≤3步完成）。
     * - FREE: 直接获取
     * - ONE_TIME: 确认支付后获取
     * - SUBSCRIPTION: 确认订阅后获取
     */
    public TemplatePurchase purchase(String productId, String buyerId, int paidCents) {
        TemplateProduct product = productCatalog.get(productId);
        if (product == null) throw new IllegalArgumentException("Product not found: " + productId);

        if (buyerId.equals(product.getSellerId())) {
            throw new IllegalStateException("Cannot purchase your own template");
        }

        // 检查是否已购买
        Set<String> userOwned = userPurchases.getOrDefault(buyerId, Set.of());
        if (userOwned.contains(product.getTemplateId())) {
            // 检查已有购买是否仍有效
            Optional<TemplatePurchase> existing = purchases.values().stream()
                    .filter(p -> p.getTemplateId().equals(product.getTemplateId())
                            && buyerId.equals(p.getBuyerId())
                            && p.isActive())
                    .findFirst();
            if (existing.isPresent()) {
                throw new IllegalStateException("Already purchased and active: " + existing.get().getPurchaseId());
            }
        }

        // 验证支付金额
        if (!product.isFree() && paidCents < product.getPriceCents()) {
            throw new IllegalArgumentException("Insufficient payment: required "
                    + product.getPriceCents() + " cents, got " + paidCents);
        }

        // 创建购买记录
        TemplatePurchase purchase = new TemplatePurchase();
        purchase.setPurchaseId("pur-" + UUID.randomUUID().toString().substring(0, 8));
        purchase.setProductId(productId);
        purchase.setTemplateId(product.getTemplateId());
        purchase.setBuyerId(buyerId);
        purchase.setSellerId(product.getSellerId());
        purchase.setPaidCents(paidCents);
        purchase.setCurrency(product.getCurrency());
        purchase.setLicenseKey("LIC-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        purchase.complete();

        // 设置订阅到期时间
        if (product.getPricingModel() == TemplateProduct.PricingModel.SUBSCRIPTION) {
            purchase.setExpiresAt(Instant.now().plus(product.getSubscriptionDays(), ChronoUnit.DAYS));
        }

        // 记录销售
        product.recordSale(paidCents);

        // 更新模板使用计数
        Template template = templateRepository.findById(product.getTemplateId()).orElse(null);
        if (template != null) {
            template.incrementUsage();
            templateRepository.save(template);
        }

        purchases.put(purchase.getPurchaseId(), purchase);
        userPurchases.computeIfAbsent(buyerId, k -> ConcurrentHashMap.newKeySet())
                .add(product.getTemplateId());

        return purchase;
    }

    /**
     * 退款。
     */
    public TemplatePurchase refund(String purchaseId, String buyerId) {
        TemplatePurchase purchase = purchases.get(purchaseId);
        if (purchase == null) throw new IllegalArgumentException("Purchase not found: " + purchaseId);
        if (!buyerId.equals(purchase.getBuyerId())) {
            throw new IllegalStateException("Only the buyer can request a refund");
        }
        if (purchase.getStatus() != TemplatePurchase.PurchaseStatus.COMPLETED) {
            throw new IllegalStateException("Only completed purchases can be refunded");
        }

        purchase.refund();
        return purchase;
    }

    // ── 查询操作 ──

    /**
     * 获取用户已购模板列表。
     */
    public List<TemplatePurchase> getUserPurchases(String buyerId) {
        return purchases.values().stream()
                .filter(p -> buyerId.equals(p.getBuyerId()))
                .sorted(Comparator.comparing(TemplatePurchase::getPurchasedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否拥有某个模板的使用权。
     */
    public boolean hasAccess(String userId, String templateId) {
        return purchases.values().stream()
                .anyMatch(p -> p.getTemplateId().equals(templateId)
                        && userId.equals(p.getBuyerId())
                        && p.isActive());
    }

    /**
     * 获取卖家上架的商品列表。
     */
    public List<TemplateProduct> getSellerProducts(String sellerId) {
        return productCatalog.values().stream()
                .filter(p -> sellerId.equals(p.getSellerId()))
                .sorted(Comparator.comparing(TemplateProduct::getListedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取市场统计信息。
     */
    public Map<String, Object> getMarketplaceStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalProducts", productCatalog.size());
        stats.put("totalPurchases", purchases.size());

        // 按定价模型统计
        Map<String, Long> byModel = productCatalog.values().stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPricingModel().name(),
                        Collectors.counting()));
        stats.put("productsByModel", byModel);

        // 总交易额
        long totalRevenue = productCatalog.values().stream()
                .mapToLong(TemplateProduct::getTotalRevenueCents)
                .sum();
        stats.put("totalRevenueCents", totalRevenue);

        // 总销量
        int totalSales = productCatalog.values().stream()
                .mapToInt(TemplateProduct::getTotalSales)
                .sum();
        stats.put("totalSales", totalSales);

        // 免费/付费比例
        long freeCount = productCatalog.values().stream()
                .filter(TemplateProduct::isFree).count();
        stats.put("freeProducts", freeCount);
        stats.put("paidProducts", productCatalog.size() - freeCount);

        // 模板应用成功率
        long totalPurchases = purchases.size();
        long successfulApplications = purchases.values().stream()
                .filter(p -> p.getStatus() == TemplatePurchase.PurchaseStatus.COMPLETED && p.isActive())
                .count();
        float successRate = totalPurchases > 0 ? (float) successfulApplications / totalPurchases : 0;
        stats.put("applicationSuccessRate", Math.min(1.0f, successRate));

        // 热门商品 Top 10
        List<Map<String, Object>> topProducts = productCatalog.values().stream()
                .sorted(Comparator.comparingInt(TemplateProduct::getTotalSales).reversed())
                .limit(10)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", p.getProductId());
                    m.put("templateId", p.getTemplateId());
                    m.put("sales", p.getTotalSales());
                    m.put("revenue", p.getTotalRevenueCents());
                    m.put("rating", p.getAvgRating());
                    return m;
                })
                .toList();
        stats.put("topProducts", topProducts);

        return stats;
    }

    // ── 私有辅助 ──

    private boolean matchesFilters(TemplateProduct product, String category,
                                    String keyword, int minPrice, int maxPrice) {
        // 价格筛选
        if (minPrice > 0 && product.getPriceCents() < minPrice) return false;
        if (maxPrice > 0 && product.getPriceCents() > maxPrice) return false;

        // 分类/关键词筛选
        if (category != null || keyword != null) {
            Template template = templateRepository.findById(product.getTemplateId()).orElse(null);
            if (template == null) return false;
            if (category != null && !category.isEmpty()
                    && !template.getCategory().name().equalsIgnoreCase(category)) {
                return false;
            }
            if (keyword != null && !keyword.isEmpty()) {
                String kw = keyword.toLowerCase();
                boolean match = template.getName().toLowerCase().contains(kw)
                        || template.getDescription().toLowerCase().contains(kw)
                        || template.getTags().stream().anyMatch(t -> t.toLowerCase().contains(kw));
                if (!match) return false;
            }
        }
        return true;
    }
}
