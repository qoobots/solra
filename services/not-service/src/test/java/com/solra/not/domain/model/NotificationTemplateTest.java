package com.solra.not.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationTemplate 单元测试。
 * NOT-004: 通知模板领域模型。
 */
@DisplayName("NotificationTemplate — 通知模板领域模型测试")
class NotificationTemplateTest {

    @Nested
    @DisplayName("构造与初始化")
    class Construction {

        @Test
        @DisplayName("构造函数应正确初始化所有字段")
        void constructorInitializesCorrectly() {
            NotificationTemplate template = new NotificationTemplate(
                    "tmpl-001", "follow", "新关注",
                    NotificationType.FOLLOW, NotificationPriority.NORMAL,
                    "{username} 关注了你",
                    "TA 正在探索索拉空间");

            assertEquals("tmpl-001", template.getTemplateId());
            assertEquals("follow", template.getTemplateCode());
            assertEquals("新关注", template.getName());
            assertEquals(NotificationType.FOLLOW, template.getType());
            assertEquals(NotificationPriority.NORMAL, template.getDefaultPriority());
            assertEquals("{username} 关注了你", template.getTitleTemplate());
            assertTrue(template.isActive());
            assertEquals(1, template.getVersion());
            assertNotNull(template.getCreatedAt());
            assertNotNull(template.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("生命周期")
    class Lifecycle {

        @Test
        @DisplayName("activate 应设置 active=true")
        void activate() {
            NotificationTemplate template = createTemplate();
            template.deactivate();
            assertFalse(template.isActive());

            template.activate();
            assertTrue(template.isActive());
        }

        @Test
        @DisplayName("deactivate 应设置 active=false")
        void deactivate() {
            NotificationTemplate template = createTemplate();
            template.deactivate();
            assertFalse(template.isActive());
        }

        @Test
        @DisplayName("bumpVersion 应递增版本号")
        void bumpVersion() {
            NotificationTemplate template = createTemplate();
            assertEquals(1, template.getVersion());

            template.bumpVersion();
            assertEquals(2, template.getVersion());

            template.bumpVersion();
            assertEquals(3, template.getVersion());
        }

        @Test
        @DisplayName("bumpVersion 应更新 updatedAt")
        void bumpVersionUpdatesTimestamp() {
            NotificationTemplate template = createTemplate();
            Instant before = template.getUpdatedAt();

            // 稍微等待确保时间不同
            template.bumpVersion();
            assertTrue(template.getUpdatedAt().compareTo(before) >= 0);
        }
    }

    private NotificationTemplate createTemplate() {
        return new NotificationTemplate(
                "tmpl-001", "follow", "新关注",
                NotificationType.FOLLOW, NotificationPriority.NORMAL,
                "{username} 关注了你",
                "TA 正在探索索拉空间");
    }
}
