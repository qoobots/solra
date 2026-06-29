package com.solra.crt.interfaces.rest;

import com.solra.crt.application.dto.PageResult;
import com.solra.crt.application.dto.ProjectDTO;
import com.solra.crt.application.service.ProjectApplicationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 项目 REST 控制器。
 * 对应 Proto SpaceCreationService 中项目管理相关的 RPC。
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectApplicationService projectService;

    public ProjectController(ProjectApplicationService projectService) {
        this.projectService = projectService;
    }

    /**
     * 创建项目。
     * POST /api/v1/projects
     */
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(@RequestBody Map<String, Object> request) {
        String spaceId = (String) request.get("space_id");
        String name = (String) request.get("name");
        String type = (String) request.getOrDefault("type", "SPACE");
        String templateId = (String) request.get("template_id");

        ProjectDTO project = projectService.createProject(spaceId,
                extractUserId(), name, type, templateId);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    /**
     * 获取项目详情。
     * GET /api/v1/projects/{projectId}
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable String projectId) {
        ProjectDTO project = projectService.getProject(projectId);
        return ResponseEntity.ok(project);
    }

    /**
     * 更新项目。
     * PUT /api/v1/projects/{projectId}
     */
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> updateProject(@PathVariable String projectId,
                                                     @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        ProjectDTO project = projectService.updateProject(projectId, name, description);
        return ResponseEntity.ok(project);
    }

    /**
     * 删除项目。
     * DELETE /api/v1/projects/{projectId}
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable String projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 列出项目（分页）。
     * GET /api/v1/projects?owner_id=xxx&status=DRAFT&page=1&page_size=20
     */
    @GetMapping
    public ResponseEntity<PageResult<ProjectDTO>> listProjects(
            @RequestParam("owner_id") String ownerId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "20") int pageSize) {
        PageResult<ProjectDTO> result = projectService.listProjects(ownerId, status, page, pageSize);
        return ResponseEntity.ok(result);
    }

    /**
     * 构建项目。
     * POST /api/v1/projects/{projectId}/build
     */
    @PostMapping("/{projectId}/build")
    public ResponseEntity<Map<String, Object>> buildProject(@PathVariable String projectId) {
        ProjectDTO project = projectService.buildProject(projectId);
        return ResponseEntity.ok(Map.of(
                "project", project,
                "build_id", "build-" + projectId,
                "progress", 0.0
        ));
    }

    /**
     * 发布项目。
     * POST /api/v1/projects/{projectId}/publish
     */
    @PostMapping("/{projectId}/publish")
    public ResponseEntity<ProjectDTO> publishProject(@PathVariable String projectId) {
        ProjectDTO project = projectService.publishProject(projectId);
        return ResponseEntity.ok(project);
    }

    /**
     * 从请求上下文提取用户 ID（简化实现，后续接入 auth 服务后替换）。
     */
    private String extractUserId() {
        // TODO: Extract from JWT token / auth context
        return "default-user";
    }
}
