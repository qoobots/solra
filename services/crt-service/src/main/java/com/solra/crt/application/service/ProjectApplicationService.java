package com.solra.crt.application.service;

import com.solra.crt.application.dto.AIGenerateRequest;
import com.solra.crt.application.dto.AIGenerationDTO;
import com.solra.crt.application.dto.AIIterateRequest;
import com.solra.crt.application.dto.PageResult;
import com.solra.crt.application.dto.ProjectDTO;
import com.solra.crt.domain.entity.ProjectConfig;
import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.repository.ProjectRepository;
import com.solra.crt.domain.repository.TemplateRepository;
import com.solra.crt.domain.service.AISpaceGenerator;
import com.solra.crt.domain.service.ProjectDomainService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 项目应用服务。
 * 协调领域服务、仓储和 DTO 转换。
 */
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;
    private final TemplateRepository templateRepository;
    private final ProjectDomainService projectDomainService;
    private final AISpaceGenerator aiSpaceGenerator;

    public ProjectApplicationService(ProjectRepository projectRepository,
                                      TemplateRepository templateRepository) {
        this.projectRepository = projectRepository;
        this.templateRepository = templateRepository;
        this.projectDomainService = new ProjectDomainService(projectRepository);
        this.aiSpaceGenerator = new AISpaceGenerator();
    }

    public ProjectDTO createProject(String spaceId, String ownerId, String name,
                                     String type, String templateId) {
        SpaceProject.ProjectType projectType = SpaceProject.ProjectType.valueOf(type);

        SpaceProject project;
        if (templateId != null && !templateId.isEmpty()) {
            Template template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
            project = projectDomainService.createFromTemplate(
                    UUID.randomUUID().toString(), spaceId, ownerId, name, template);
            template.incrementUsage();
            templateRepository.save(template);
        } else {
            project = projectDomainService.createProject(
                    UUID.randomUUID().toString(), spaceId, ownerId, name, projectType);
        }

        return ProjectDTO.from(project);
    }

    public ProjectDTO getProject(String projectId) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        return ProjectDTO.from(project);
    }

    public ProjectDTO updateProject(String projectId, String name, String description) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.update(name, description);
        project = projectRepository.save(project);
        return ProjectDTO.from(project);
    }

    public void deleteProject(String projectId) {
        projectRepository.deleteById(projectId);
    }

    public PageResult<ProjectDTO> listProjects(String ownerId, String statusFilter,
                                                int page, int pageSize) {
        SpaceProject.ProjectStatus status = statusFilter != null ?
                SpaceProject.ProjectStatus.valueOf(statusFilter) : null;

        int offset = (page - 1) * pageSize;
        List<SpaceProject> projects = projectRepository.findByOwnerIdAndStatus(
                ownerId, status, offset, pageSize);
        long total = projectRepository.countByOwnerIdAndStatus(ownerId, status);

        List<ProjectDTO> dtos = projects.stream()
                .map(ProjectDTO::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtos, page, pageSize, total);
    }

    public ProjectDTO buildProject(String projectId) {
        SpaceProject project = projectDomainService.buildProject(projectId);
        return ProjectDTO.from(project);
    }

    public ProjectDTO publishProject(String projectId) {
        SpaceProject project = projectDomainService.publishProject(projectId);
        return ProjectDTO.from(project);
    }

    // ── CRT-001: AI 辅助空间搭建 ──

    /**
     * AI 根据文字描述生成空间蓝图并创建项目。
     */
    public AIGenerationDTO aiGenerateSpace(AIGenerateRequest request) {
        SpaceProject.ProjectType projectType = SpaceProject.ProjectType.valueOf(
                request.getProjectType() != null ? request.getProjectType() : "SPACE");

        // 创建空项目
        String projectId = UUID.randomUUID().toString();
        SpaceProject project = projectDomainService.createProject(
                projectId, request.getSpaceId(), request.getOwnerId(),
                "AI Generated: " + truncate(request.getDescription(), 50), projectType);

        // AI 生成配置
        ProjectConfig config = aiSpaceGenerator.generateFromDescription(
                request.getDescription(), projectType);
        project.updateConfig(config);
        project = projectRepository.save(project);

        // 计算完成度
        float score = aiSpaceGenerator.calculateCompletionScore(config);

        AIGenerationDTO result = new AIGenerationDTO();
        result.setProjectId(projectId);
        result.setDescription(request.getDescription());
        result.setCompletionScore(score);
        result.setIterationCount(0);
        result.setGeneratedAt(Instant.now());
        result.setProject(ProjectDTO.from(project));
        return result;
    }

    /**
     * AI 迭代修改已有空间。
     */
    public AIGenerationDTO aiIterateModification(AIIterateRequest request) {
        SpaceProject project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + request.getProjectId()));

        ProjectConfig currentConfig = project.getConfig();
        if (currentConfig == null) {
            currentConfig = new ProjectConfig();
        }

        ProjectConfig modified = aiSpaceGenerator.iterateModification(
                currentConfig, request.getModification(), request.getIteration());
        project.updateConfig(modified);
        project = projectRepository.save(project);

        float score = aiSpaceGenerator.calculateCompletionScore(modified);

        AIGenerationDTO result = new AIGenerationDTO();
        result.setProjectId(request.getProjectId());
        result.setDescription(request.getModification());
        result.setCompletionScore(score);
        result.setIterationCount(request.getIteration());
        result.setGeneratedAt(Instant.now());
        result.setProject(ProjectDTO.from(project));
        return result;
    }

    // ── CRT-002: 空间可视化编辑器 ──

    /**
     * 更新项目场景图。
     */
    public ProjectDTO updateSceneGraph(String projectId, ProjectConfig.SceneGraph sceneGraph) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectConfig config = project.getConfig() != null ? project.getConfig() : new ProjectConfig();
        config.setSceneGraph(sceneGraph);
        project.updateConfig(config);
        project = projectRepository.save(project);
        return ProjectDTO.from(project);
    }

    /**
     * 获取项目场景图。
     */
    public ProjectConfig.SceneGraph getSceneGraph(String projectId) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectConfig config = project.getConfig();
        return config != null ? config.getSceneGraph() : new ProjectConfig.SceneGraph();
    }

    /**
     * 更新项目灯光配置。
     */
    public ProjectDTO updateLighting(String projectId, ProjectConfig.LightingConfig lighting) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectConfig config = project.getConfig() != null ? project.getConfig() : new ProjectConfig();
        config.setLighting(lighting);
        project.updateConfig(config);
        project = projectRepository.save(project);
        return ProjectDTO.from(project);
    }

    /**
     * 更新项目音频配置。
     */
    public ProjectDTO updateAudio(String projectId, ProjectConfig.AudioConfig audio) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectConfig config = project.getConfig() != null ? project.getConfig() : new ProjectConfig();
        config.setAudio(audio);
        project.updateConfig(config);
        project = projectRepository.save(project);
        return ProjectDTO.from(project);
    }

    /**
     * 更新项目物理配置。
     */
    public ProjectDTO updatePhysics(String projectId, ProjectConfig.PhysicsConfig physics) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        ProjectConfig config = project.getConfig() != null ? project.getConfig() : new ProjectConfig();
        config.setPhysics(physics);
        project.updateConfig(config);
        project = projectRepository.save(project);
        return ProjectDTO.from(project);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
