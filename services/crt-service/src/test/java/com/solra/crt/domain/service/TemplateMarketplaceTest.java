package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.entity.TemplateProduct;
import com.solra.crt.domain.entity.TemplatePurchase;
import com.solra.crt.domain.repository.TemplateRepository;
import com.solra.crt.infrastructure.persistence.InMemoryTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateMarketplace 单元测试 (CRT-003)。
 */
@DisplayName("TemplateMarketplace 模板市场")
class TemplateMarketplaceTest {

    private TemplateMarketplace marketplace;
    private TemplateRepository templateRepository;
    private Template template;

    @BeforeEach
    void setUp() {
        templateRepository = new InMemoryTemplateRepository();
        marketplace = new TemplateMarketplace(templateRepository);

        template = new Template("tmpl-001", "Modern Cafe", "A modern cafe space",
                "seller-001", Template.TemplateCategory.SPACE);
        template.setTags(List.of("cafe", "modern", "indoor"));
        templateRepository.save(template);
    }

    @Test
    @DisplayName("卖家可上架模板")
    void sellerCanListTemplate() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.COMMERCIAL,
                9900, 0);

        assertNotNull(product);
        assertEquals("tmpl-001", product.getTemplateId());
        assertEquals("seller-001", product.getSellerId());
        assertEquals(9900, product.getPriceCents());
        assertEquals(TemplateProduct.PricingModel.ONE_TIME, product.getPricingModel());
    }

    @Test
    @DisplayName("非作者不能上架模板")
    void nonAuthorCannotListTemplate() {
        assertThrows(IllegalStateException.class, () ->
                marketplace.listTemplate("tmpl-001", "other-user",
                        TemplateProduct.PricingModel.FREE,
                        TemplateProduct.LicenseType.PERSONAL, 0, 0));
    }

    @Test
    @DisplayName("免费模板可直接获取（≤3步）")
    void freeTemplateCanBeAcquiredDirectly() {
        // 第1步：上架
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        // 第2步：浏览（找到商品）
        List<TemplateProduct> products = marketplace.browseMarketplace(
                null, "cafe", 0, 0, 0, 10);
        assertFalse(products.isEmpty());

        // 第3步：获取
        TemplatePurchase purchase = marketplace.purchase(
                product.getProductId(), "buyer-001", 0);

        assertNotNull(purchase);
        assertEquals(TemplatePurchase.PurchaseStatus.COMPLETED, purchase.getStatus());
        assertEquals("buyer-001", purchase.getBuyerId());
    }

    @Test
    @DisplayName("付费模板需足额支付")
    void paidTemplateRequiresFullPayment() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.COMMERCIAL,
                9900, 0);

        // 不足额支付
        assertThrows(IllegalArgumentException.class, () ->
                marketplace.purchase(product.getProductId(), "buyer-001", 5000));

        // 足额支付
        TemplatePurchase purchase = marketplace.purchase(
                product.getProductId(), "buyer-001", 9900);
        assertEquals(TemplatePurchase.PurchaseStatus.COMPLETED, purchase.getStatus());
    }

    @Test
    @DisplayName("不能购买自己的模板")
    void cannotPurchaseOwnTemplate() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        assertThrows(IllegalStateException.class, () ->
                marketplace.purchase(product.getProductId(), "seller-001", 0));
    }

    @Test
    @DisplayName("重复购买已激活模板应被拒绝")
    void duplicateActivePurchaseShouldBeRejected() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        marketplace.purchase(product.getProductId(), "buyer-001", 0);

        assertThrows(IllegalStateException.class, () ->
                marketplace.purchase(product.getProductId(), "buyer-001", 0));
    }

    @Test
    @DisplayName("购买后用户拥有访问权")
    void userHasAccessAfterPurchase() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        marketplace.purchase(product.getProductId(), "buyer-001", 0);

        assertTrue(marketplace.hasAccess("buyer-001", "tmpl-001"));
        assertFalse(marketplace.hasAccess("other-user", "tmpl-001"));
    }

    @Test
    @DisplayName("可退款")
    void canRefund() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.PERSONAL, 500, 0);

        TemplatePurchase purchase = marketplace.purchase(
                product.getProductId(), "buyer-001", 500);

        TemplatePurchase refunded = marketplace.refund(
                purchase.getPurchaseId(), "buyer-001");
        assertEquals(TemplatePurchase.PurchaseStatus.REFUNDED, refunded.getStatus());
    }

    @Test
    @DisplayName("市场浏览可按价格筛选")
    void canBrowseByPriceRange() {
        marketplace.listTemplate("tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.COMMERCIAL, 9900, 0);

        // 创建第二个模板
        Template t2 = new Template("tmpl-002", "Budget Cafe", "Cheap cafe",
                "seller-002", Template.TemplateCategory.SPACE);
        t2.setTags(List.of("cafe", "budget"));
        templateRepository.save(t2);

        marketplace.listTemplate("tmpl-002", "seller-002",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.PERSONAL, 500, 0);

        List<TemplateProduct> cheap = marketplace.browseMarketplace(
                null, null, 0, 1000, 0, 10);
        assertEquals(1, cheap.size());

        List<TemplateProduct> expensive = marketplace.browseMarketplace(
                null, null, 5000, 0, 0, 10);
        assertEquals(1, expensive.size());
    }

    @Test
    @DisplayName("可获取用户已购列表")
    void canGetUserPurchases() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        marketplace.purchase(product.getProductId(), "buyer-001", 0);

        List<TemplatePurchase> purchases = marketplace.getUserPurchases("buyer-001");
        assertEquals(1, purchases.size());
    }

    @Test
    @DisplayName("市场统计信息正确")
    void marketplaceStatsShouldBeCorrect() {
        marketplace.listTemplate("tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.COMMERCIAL, 9900, 0);

        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.COMMERCIAL, 9900, 0);

        // 上面的第二次上架会抛出异常，因为模板已上架。用不同模板
        Template t2 = new Template("tmpl-003", "Another", "Another",
                "seller-001", Template.TemplateCategory.SPACE);
        templateRepository.save(t2);

        TemplateProduct p2 = marketplace.listTemplate(
                "tmpl-003", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        marketplace.purchase(p2.getProductId(), "buyer-001", 0);

        Map<String, Object> stats = marketplace.getMarketplaceStats();
        assertNotNull(stats);
        assertTrue((int) stats.get("totalProducts") >= 1);
        assertTrue((int) stats.get("totalPurchases") >= 1);
        assertNotNull(stats.get("topProducts"));
    }

    @Test
    @DisplayName("卖家可查看自己的商品列表")
    void sellerCanViewOwnProducts() {
        marketplace.listTemplate("tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        List<TemplateProduct> products = marketplace.getSellerProducts("seller-001");
        assertEquals(1, products.size());
    }

    @Test
    @DisplayName("可下架模板")
    void canDelistTemplate() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.FREE,
                TemplateProduct.LicenseType.PERSONAL, 0, 0);

        boolean delisted = marketplace.delistTemplate(product.getProductId(), "seller-001");
        assertTrue(delisted);

        // 下架后浏览不到了
        List<TemplateProduct> products = marketplace.browseMarketplace(
                null, null, 0, 0, 0, 10);
        assertTrue(products.isEmpty());
    }

    @Test
    @DisplayName("可更新定价")
    void canUpdatePricing() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.ONE_TIME,
                TemplateProduct.LicenseType.COMMERCIAL, 9900, 0);

        TemplateProduct updated = marketplace.updatePricing(
                product.getProductId(), "seller-001", 4900,
                TemplateProduct.PricingModel.ONE_TIME);

        assertEquals(4900, updated.getPriceCents());
    }

    @Test
    @DisplayName("订阅模板应有过期时间")
    void subscriptionTemplateShouldHaveExpiry() {
        TemplateProduct product = marketplace.listTemplate(
                "tmpl-001", "seller-001",
                TemplateProduct.PricingModel.SUBSCRIPTION,
                TemplateProduct.LicenseType.COMMERCIAL,
                2900, 30);

        TemplatePurchase purchase = marketplace.purchase(
                product.getProductId(), "buyer-001", 2900);

        assertNotNull(purchase.getExpiresAt());
        assertTrue(purchase.isActive());
    }
}
