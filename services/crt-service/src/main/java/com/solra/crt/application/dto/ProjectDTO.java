package com.solra.crt.application.dto;

import com.solra.crt.domain.entity.SpaceProject;
import java.time.Instant;
import java.util.List;

/**
 * 项目数据传输对象。
 */
public class ProjectDTO {

    private String projectId;
    private String spaceId;
    private String ownerId;
    private String name;
    private String description;
    private String type;
    private String status;
    private List<String> collaboratorIds;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastEditedAt;

    public static ProjectDTO from(SpaceProject project) {
        ProjectDTO dto = new ProjectDTO();
        dto.projectId = project.getProjectId();
        dto.spaceId = project.getSpaceId();
        dto.ownerId = project.getOwnerId();
        dto.name = project.getName();
        dto.description = project.getDescription();
        dto.type = project.getType().name();
        dto.status = project.getStatus().name();
        dto.collaboratorIds = project.getCollaboratorIds();
        dto.createdAt = project.getCreatedAt();
        dto.updatedAt = project.getUpdatedAt();
        dto.lastEditedAt = project.getLastEditedAt();
        return dto;
    }

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
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<String> getCollaboratorIds() { return collaboratorIds; }
    public void setCollaboratorIds(List<String> collaboratorIds) { this.collaboratorIds = collaboratorIds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getLastEditedAt() { return lastEditedAt; }
    public void setLastEditedAt(Instant lastEditedAt) { this.lastEditedAt = lastEditedAt; }
}
