package com.solra.crt.application.dto;

/**
 * AI 空间生成请求 DTO (CRT-001)。
 */
public class AIGenerateRequest {

    private String description;
    private String projectType;
    private String spaceId;
    private String ownerId;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
}
