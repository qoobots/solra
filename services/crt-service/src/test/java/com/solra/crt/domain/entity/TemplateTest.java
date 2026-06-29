package com.solra.crt.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Template 实体单元测试。
 * 验证模板的评分边界值和使用计数。
 */
@DisplayName("Template — 模板实体测试")
class TemplateTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("默认 usageCount 为 0")
        void defaultUsageCountZero() {
            Template tpl = createTemplate();
            assertEquals(0, tpl.getUsageCount());
        }

        @Test
        @DisplayName("默认 rating 为 0.0")
        void defaultRatingZero() {
            Template tpl = createTemplate();
            assertEquals(0.0f, tpl.getRating());
        }

        @Test
        @DisplayName("默认非官方模板")
        void defaultNotOfficial() {
            Template tpl = createTemplate();
            assertFalse(tpl.isOfficial());
        }

        @Test
        @DisplayName("默认标签列表为空")
        void defaultTagsEmpty() {
            Template tpl = createTemplate();
            assertTrue(tpl.getTags().isEmpty());
        }

        @Test
        @DisplayName("默认配置不为空")
        void defaultConfigNotNull() {
            Template tpl = createTemplate();
            assertNotNull(tpl.getDefaultConfig());
        }
    }

    @Nested
    @DisplayName("incrementUsage() — 增加使用计数")
    class IncrementUsage {

        @Test
        @DisplayName("单次递增")
        void singleIncrement() {
            Template tpl = createTemplate();
            tpl.incrementUsage();
            assertEquals(1, tpl.getUsageCount());
        }

        @Test
        @DisplayName("多次递增")
        void multipleIncrements() {
            Template tpl = createTemplate();
            tpl.incrementUsage();
            tpl.incrementUsage();
            tpl.incrementUsage();
            assertEquals(3, tpl.getUsageCount());
        }
    }

    @Nested
    @DisplayName("updateRating() — 更新评分")
    class UpdateRating {

        @Test
        @DisplayName("正常评分 4.5")
        void normalRating() {
            Template tpl = createTemplate();
            tpl.updateRating(4.5f);
            assertEquals(4.5f, tpl.getRating());
        }

        @Test
        @DisplayName("评分上限为 5.0")
        void ratingCappedAt5() {
            Template tpl = createTemplate();
            tpl.updateRating(5.5f);
            assertEquals(5.0f, tpl.getRating());
        }

        @Test
        @DisplayName("评分下限为 0.0")
        void ratingFloorAt0() {
            Template tpl = createTemplate();
            tpl.updateRating(3.0f);
            tpl.updateRating(-1.0f);
            assertEquals(0.0f, tpl.getRating());
        }

        @Test
        @DisplayName("边界值 0.0")
        void ratingBoundaryZero() {
            Template tpl = createTemplate();
            tpl.updateRating(0.0f);
            assertEquals(0.0f, tpl.getRating());
        }

        @Test
        @DisplayName("边界值 5.0")
        void ratingBoundaryFive() {
            Template tpl = createTemplate();
            tpl.updateRating(5.0f);
            assertEquals(5.0f, tpl.getRating());
        }

        @Test
        @DisplayName("评分 10.0 钳制到 5.0")
        void rating10ClampedTo5() {
            Template tpl = createTemplate();
            tpl.updateRating(10.0f);
            assertEquals(5.0f, tpl.getRating());
        }

        @Test
        @DisplayName("评分 -5.0 钳制到 0.0")
        void ratingNeg5ClampedTo0() {
            Template tpl = createTemplate();
            tpl.updateRating(2.0f);
            tpl.updateRating(-5.0f);
            assertEquals(0.0f, tpl.getRating());
        }
    }

    @Nested
    @DisplayName("模板分类")
    class Categories {

        @Test
        @DisplayName("SPACE 分类")
        void spaceCategory() {
            Template tpl = new Template("t1", "Space", "desc", "author",
                    Template.TemplateCategory.SPACE);
            assertEquals(Template.TemplateCategory.SPACE, tpl.getCategory());
        }

        @Test
        @DisplayName("GALLERY 分类")
        void galleryCategory() {
            Template tpl = new Template("t1", "Gallery", "desc", "author",
                    Template.TemplateCategory.GALLERY);
            assertEquals(Template.TemplateCategory.GALLERY, tpl.getCategory());
        }

        @Test
        @DisplayName("AVATAR 分类")
        void avatarCategory() {
            Template tpl = new Template("t1", "Avatar", "desc", "author",
                    Template.TemplateCategory.AVATAR);
            assertEquals(Template.TemplateCategory.AVATAR, tpl.getCategory());
        }

        @Test
        @DisplayName("GAME 分类")
        void gameCategory() {
            Template tpl = new Template("t1", "Game", "desc", "author",
                    Template.TemplateCategory.GAME);
            assertEquals(Template.TemplateCategory.GAME, tpl.getCategory());
        }
    }

    private Template createTemplate() {
        return new Template("tpl-1", "Basic Space", "A basic template",
                "author-1", Template.TemplateCategory.SPACE);
    }
}
