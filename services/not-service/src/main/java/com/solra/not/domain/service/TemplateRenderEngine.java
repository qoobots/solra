package com.solra.not.domain.service;

import com.solra.not.domain.model.NotificationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TemplateRenderEngine — 模板渲染引擎。
 * NOT-004: 系统通知模板管理。
 *
 * 负责将模板中的占位变量替换为实际值，支持多语言渲染。
 */
@Service
public class TemplateRenderEngine {

    private static final Logger log = LoggerFactory.getLogger(TemplateRenderEngine.class);
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{(\\w+)}");

    /**
     * 渲染模板标题和内容。
     *
     * @param template  通知模板
     * @param variables 变量映射，如 {"username":"张三", "space_name":"星空咖啡馆"}
     * @param locale    语言代码，如 "zh-CN"，为 null 时使用默认模板
     * @return 渲染结果
     */
    public RenderResult render(NotificationTemplate template, Map<String, String> variables, String locale) {
        String title = selectLocalized(template.getTitleTemplate(), template.getLocalizedTitles(), locale);
        String body = selectLocalized(template.getBodyTemplate(), template.getLocalizedBodies(), locale);
        String deepLink = template.getDeepLinkTemplate();

        title = replaceVariables(title, variables);
        body = replaceVariables(body, variables);
        if (deepLink != null) {
            deepLink = replaceVariables(deepLink, variables);
        }

        log.debug("Rendered template {}: title={}", template.getTemplateCode(), title);
        return new RenderResult(title, body, deepLink, template.getImageUrl());
    }

    /**
     * 渲染结果。
     */
    public record RenderResult(String title, String body, String deepLink, String imageUrl) {}

    /**
     * 预览模板渲染（使用默认变量）。
     */
    public RenderResult preview(NotificationTemplate template) {
        Map<String, String> defaultVars = Map.of(
                "username", "用户",
                "space_name", "示例空间",
                "avatar_name", "虚拟人小索",
                "count", "5",
                "time", "刚刚"
        );
        return render(template, defaultVars, "zh-CN");
    }

    /**
     * 验证模板变量是否完整。
     * 返回模板中需要的变量列表。
     */
    public java.util.List<String> extractVariables(NotificationTemplate template) {
        java.util.List<String> vars = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        extractVariablesFrom(template.getTitleTemplate(), vars, seen);
        extractVariablesFrom(template.getBodyTemplate(), vars, seen);
        if (template.getDeepLinkTemplate() != null) {
            extractVariablesFrom(template.getDeepLinkTemplate(), vars, seen);
        }
        return vars;
    }

    private String selectLocalized(String defaultText, Map<String, String> localized, String locale) {
        if (locale != null && localized != null && localized.containsKey(locale)) {
            return localized.get(locale);
        }
        return defaultText;
    }

    private String replaceVariables(String template, Map<String, String> variables) {
        if (template == null || variables == null || variables.isEmpty()) {
            return template;
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, "{" + varName + "}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void extractVariablesFrom(String text, java.util.List<String> vars, java.util.Set<String> seen) {
        if (text == null) return;
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            String varName = matcher.group(1);
            if (seen.add(varName)) {
                vars.add(varName);
            }
        }
    }
}
