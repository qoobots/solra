package com.solra.crt.application.service;

import com.solra.crt.application.dto.PageResult;
import com.solra.crt.application.dto.ProjectDTO;
import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.repository.ProjectRepository;
import com.solra.crt.domain.repository.TemplateRepository;
import com.solra.crt.domain.service.ProjectDomainService;

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

    public ProjectApplicationService(ProjectRepository projectRepository,
                                      TemplateRepository templateRepository) {
        this.projectRepository = projectRepository;
        this.templateRepository = templateRepository;
        this.projectDomainService = new ProjectDomainService(projectRepository);
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
}
