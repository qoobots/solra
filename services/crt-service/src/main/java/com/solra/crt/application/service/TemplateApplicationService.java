package com.solra.crt.application.service;

import com.solra.crt.application.dto.PageResult;
import com.solra.crt.application.dto.TemplateDTO;
import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.repository.TemplateRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模板应用服务。
 */
public class TemplateApplicationService {

    private final TemplateRepository templateRepository;

    public TemplateApplicationService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public PageResult<TemplateDTO> listTemplates(String category, String keyword,
                                                   int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<Template> templates;
        long total;

        if (keyword != null && !keyword.isEmpty()) {
            templates = templateRepository.searchByKeyword(keyword, offset, pageSize);
            total = templateRepository.countByKeyword(keyword);
        } else if (category != null && !category.isEmpty()) {
            Template.TemplateCategory cat = Template.TemplateCategory.valueOf(category);
            templates = templateRepository.findByCategory(cat, offset, pageSize);
            total = templateRepository.countByCategory(cat);
        } else {
            templates = templateRepository.findByCategory(null, offset, pageSize);
            total = templateRepository.countByCategory(null);
        }

        List<TemplateDTO> dtos = templates.stream()
                .map(TemplateDTO::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtos, page, pageSize, total);
    }
}
