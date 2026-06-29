package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.entity.Template;
import com.solra.crt.domain.entity.ProjectConfig;
import com.solra.crt.domain.repository.ProjectRepository;

/**
 * 项目领域服务。
 * 封装跨实体的项目业务逻辑：创建、从模板创建、构建发布等。
 */
public class ProjectDomainService {

    private final ProjectRepository projectRepository;

    public ProjectDomainService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * 创建新项目。
     */
    public SpaceProject createProject(String projectId, String spaceId, String ownerId,
                                       String name, SpaceProject.ProjectType type) {
        SpaceProject project = new SpaceProject(projectId, spaceId, ownerId, name, type);
        return projectRepository.save(project);
    }

    /**
     * 从模板创建项目。
     */
    public SpaceProject createFromTemplate(String projectId, String spaceId, String ownerId,
                                            String name, Template template) {
        SpaceProject project = new SpaceProject(projectId, spaceId, ownerId, name,
                mapTemplateCategory(template.getCategory()));
        project.setConfig(template.getDefaultConfig());
        return projectRepository.save(project);
    }

    /**
     * 发起项目构建。
     */
    public SpaceProject buildProject(String projectId) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.startBuilding();
        return projectRepository.save(project);
    }

    /**
     * 发布项目。
     */
    public SpaceProject publishProject(String projectId) {
        SpaceProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.publish();
        return projectRepository.save(project);
    }

    private SpaceProject.ProjectType mapTemplateCategory(Template.TemplateCategory category) {
        return switch (category) {
            case SPACE -> SpaceProject.ProjectType.SPACE;
            case GALLERY -> SpaceProject.ProjectType.GALLERY;
            case AVATAR -> SpaceProject.ProjectType.AVATAR;
            case GAME -> SpaceProject.ProjectType.SCENE;
        };
    }
}
