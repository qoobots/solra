package com.solra.crt.domain.repository;

import com.solra.crt.domain.entity.Template;
import java.util.List;
import java.util.Optional;

/**
 * 模板仓储接口（领域层端口）。
 */
public interface TemplateRepository {

    Optional<Template> findById(String templateId);

    List<Template> findByCategory(Template.TemplateCategory category, int offset, int limit);

    List<Template> searchByKeyword(String keyword, int offset, int limit);

    long countByCategory(Template.TemplateCategory category);

    long countByKeyword(String keyword);

    Template save(Template template);
}
