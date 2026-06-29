package com.solra.not.domain.service;

import com.solra.not.domain.model.NotificationPriority;
import com.solra.not.domain.model.NotificationTemplate;
import com.solra.not.domain.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateRenderEngine 单元测试。
 * NOT-004: 系统通知模板管理。
 */
@DisplayName("TemplateRenderEngine — 模板渲染引擎测试")
class TemplateRenderEngineTest {

    private TemplateRenderEngine engine;
    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        engine = new TemplateRenderEngine();
        template = new NotificationTemplate(
                "tmpl-001", "follow", "新关注",
                NotificationType.FOLLOW, NotificationPriority.NORMAL,
                "{username} 关注了你",
                "TA 正在索拉探索空间，去看看 TA 的空间吧");
    }

    @Nested
    @DisplayName("render — 渲染模板")
    class Render {

        @Test
        @DisplayName("基本变量替换")
        void basicVariableReplacement() {
            Map<String, String> vars = Map.of("username", "张三");

            TemplateRenderEngine.RenderResult result = engine.render(template, vars, null);

            assertEquals("张三 关注了你", result.title());
            assertTrue(result.body().contains("TA 正在索拉探索空间"));
        }

        @Test
        @DisplayName("多个变量替换")
        void multipleVariableReplacement() {
            NotificationTemplate multiVarTemplate = new NotificationTemplate(
                    "tmpl-002", "interaction", "互动",
                    NotificationType.INTERACTION, NotificationPriority.NORMAL,
                    "{username} 在「{space_name}」与你互动",
                    "TA 与你互动了 {count} 次");

            Map<String, String> vars = Map.of(
                    "username", "李四",
                    "space_name", "星空咖啡馆",
                    "count", "3");

            TemplateRenderEngine.RenderResult result = engine.render(multiVarTemplate, vars, null);

            assertEquals("李四 在「星空咖啡馆」与你互动", result.title());
            assertEquals("TA 与你互动了 3 次", result.body());
        }

        @Test
        @DisplayName("缺少变量时保留占位符")
        void missingVariableKeepsPlaceholder() {
            Map<String, String> vars = Map.of();

            TemplateRenderEngine.RenderResult result = engine.render(template, vars, null);

            assertEquals("{username} 关注了你", result.title());
        }

        @Test
        @DisplayName("空变量映射时返回原始模板")
        void emptyVariablesReturnsRawTemplate() {
            TemplateRenderEngine.RenderResult result = engine.render(template, null, null);

            assertEquals("{username} 关注了你", result.title());
        }

        @Test
        @DisplayName("多语言渲染")
        void localizedRendering() {
            template.setLocalizedTitles(Map.of("en", "{username} followed you"));
            template.setLocalizedBodies(Map.of("en", "They are exploring spaces on Solra"));

            Map<String, String> vars = Map.of("username", "Alice");
            TemplateRenderEngine.RenderResult result = engine.render(template, vars, "en");

            assertEquals("Alice followed you", result.title());
        }

        @Test
        @DisplayName("不存在的语言回退到默认模板")
        void unknownLocaleFallsBack() {
            template.setLocalizedTitles(Map.of("en", "{username} followed you"));

            Map<String, String> vars = Map.of("username", "王五");
            TemplateRenderEngine.RenderResult result = engine.render(template, vars, "ja");

            assertEquals("王五 关注了你", result.title());
        }

        @Test
        @DisplayName("deepLink 模板渲染")
        void deepLinkRendering() {
            template.setDeepLinkTemplate("solra://user/{username}");
            Map<String, String> vars = Map.of("username", "test_user");

            TemplateRenderEngine.RenderResult result = engine.render(template, vars, null);

            assertEquals("solra://user/test_user", result.deepLink());
        }
    }

    @Nested
    @DisplayName("preview — 模板预览")
    class Preview {

        @Test
        @DisplayName("预览使用默认变量")
        void previewWithDefaults() {
            TemplateRenderEngine.RenderResult result = engine.preview(template);

            assertEquals("用户 关注了你", result.title());
        }
    }

    @Nested
    @DisplayName("extractVariables — 变量提取")
    class ExtractVariables {

        @Test
        @DisplayName("提取标题和内容中的变量")
        void extractAllVariables() {
            List<String> vars = engine.extractVariables(template);

            assertTrue(vars.contains("username"));
        }

        @Test
        @DisplayName("提取多模板变量并去重")
        void extractMultipleVariables() {
            NotificationTemplate multiTemplate = new NotificationTemplate(
                    "tmpl-003", "invite", "邀请",
                    NotificationType.SESSION_INVITE, NotificationPriority.HIGH,
                    "{username} 邀请你加入「{space_name}」",
                    "快来和 {count} 位好友一起探索");

            List<String> vars = engine.extractVariables(multiTemplate);

            assertEquals(3, vars.size());
            assertTrue(vars.contains("username"));
            assertTrue(vars.contains("space_name"));
            assertTrue(vars.contains("count"));
        }

        @Test
        @DisplayName("deepLink 中的变量也应提取")
        void extractDeepLinkVariables() {
            template.setDeepLinkTemplate("solra://space/{space_id}/user/{username}");
            List<String> vars = engine.extractVariables(template);

            assertTrue(vars.contains("username"));
            assertTrue(vars.contains("space_id"));
        }
    }
}
