package com.solra.crt.interfaces.rest;

import com.solra.crt.application.dto.PageResult;
import com.solra.crt.application.dto.TemplateDTO;
import com.solra.crt.application.service.TemplateApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 模板 REST 控制器。
 * 对应 Proto SpaceCreationService 中模板相关的 RPC。
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateApplicationService templateService;

    public TemplateController(TemplateApplicationService templateService) {
        this.templateService = templateService;
    }

    /**
     * 列出模板（分页+搜索）。
     * GET /api/v1/templates?category=SPACE&keyword=xxx&page=1&page_size=20
     */
    @GetMapping
    public ResponseEntity<PageResult<TemplateDTO>> listTemplates(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        PageResult<TemplateDTO> result = templateService.listTemplates(category, keyword, page, pageSize);
        return ResponseEntity.ok(result);
    }
}
