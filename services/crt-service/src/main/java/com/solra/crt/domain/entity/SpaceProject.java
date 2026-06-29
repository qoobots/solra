package com.solra.crt.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 空间项目领域实体。
 * 代表一个空间创作项目，包含场景配置、协作者和构建发布状态。
 */
public class SpaceProject {

    private String projectId;
    private String spaceId;
    private String ownerId;
    private String name;
    private String description;
    private ProjectType type;
    private ProjectStatus status;
    private ProjectConfig config;
    private List<String> collaboratorIds;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastEditedAt;

    public enum ProjectType {
        SPACE, GALLERY, AVATAR, SCENE
    }

    public enum ProjectStatus {
        DRAFT, BUILDING, PUBLISHED, ARCHIVED
    }

    public SpaceProject(String projectId, String spaceId, String ownerId,
                        String name, ProjectType type) {
        this.projectId = projectId;
        this.spaceId = spaceId;
        this.ownerId = ownerId;
        this.name = name;
        this.type = type != null ? type : ProjectType.SPACE;
        this.status = ProjectStatus.DRAFT;
        this.collaboratorIds = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.lastEditedAt = Instant.now();
    }

    public void update(String name, String description) {
        this.name = name != null ? name : this.name;
        this.description = description != null ? description : this.description;
        this.updatedAt = Instant.now();
        this.lastEditedAt = Instant.now();
    }

    public void updateConfig(ProjectConfig config) {
        this.config = config;
        this.updatedAt = Instant.now();
        this.lastEditedAt = Instant.now();
    }

    public void startBuilding() {
        this.status = ProjectStatus.BUILDING;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.status = ProjectStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public void addCollaborator(String userId) {
        if (!this.collaboratorIds.contains(userId)) {
            this.collaboratorIds.add(userId);
        }
    }

    public void removeCollaborator(String userId) {
        this.collaboratorIds.remove(userId);
    }

    // Getters and setters
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProjectType getType() { return type; }
    public void setType(ProjectType type) { this.type = type; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public ProjectConfig getConfig() { return config; }
    public void setConfig(ProjectConfig config) { this.config = config; }
    public List<String> getCollaboratorIds() { return collaboratorIds; }
    public void setCollaboratorIds(List<String> collaboratorIds) { this.collaboratorIds = collaboratorIds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getLastEditedAt() { return lastEditedAt; }
}
