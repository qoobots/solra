package com.solra.crt.infrastructure.persistence;

import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.repository.TemplateRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 模板仓储内存实现（开发阶段，后续替换为 JPA/PostgreSQL）。
 */
public class InMemoryTemplateRepository implements TemplateRepository {

    private final Map<String, Template> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Template> findById(String templateId) {
        return Optional.ofNullable(store.get(templateId));
    }

    @Override
    public List<Template> findByCategory(Template.TemplateCategory category, int offset, int limit) {
        return store.values().stream()
                .filter(t -> category == null || t.getCategory() == category)
                .sorted(Comparator.comparingInt(Template::getUsageCount).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Template> searchByKeyword(String keyword, int offset, int limit) {
        String kw = keyword.toLowerCase();
        return store.values().stream()
                .filter(t -> t.getName().toLowerCase().contains(kw)
                        || t.getDescription().toLowerCase().contains(kw)
                        || t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(kw)))
                .sorted(Comparator.comparingInt(Template::getUsageCount).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCategory(Template.TemplateCategory category) {
        return store.values().stream()
                .filter(t -> category == null || t.getCategory() == category)
                .count();
    }

    @Override
    public long countByKeyword(String keyword) {
        String kw = keyword.toLowerCase();
        return store.values().stream()
                .filter(t -> t.getName().toLowerCase().contains(kw)
                        || t.getDescription().toLowerCase().contains(kw)
                        || t.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(kw)))
                .count();
    }

    @Override
    public Template save(Template template) {
        store.put(template.getTemplateId(), template);
        return template;
    }
}
