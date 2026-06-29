package com.solra.crt.application.dto;

import java.time.Instant;

/**
 * AI 空间生成结果 DTO (CRT-001)。
 */
public class AIGenerationDTO {

    private String projectId;
    private String description;
    private float completionScore;
    private int iterationCount;
    private Instant generatedAt;
    private ProjectDTO project;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public float getCompletionScore() { return completionScore; }
    public void setCompletionScore(float completionScore) { this.completionScore = completionScore; }
    public int getIterationCount() { return iterationCount; }
    public void setIterationCount(int iterationCount) { this.iterationCount = iterationCount; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public ProjectDTO getProject() { return project; }
    public void setProject(ProjectDTO project) { this.project = project; }
}
