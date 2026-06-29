package com.solra.crt.application.dto;

/**
 * AI 空间迭代修改请求 DTO (CRT-001)。
 */
public class AIIterateRequest {

    private String projectId;
    private String modification;
    private int iteration;

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getModification() { return modification; }
    public void setModification(String modification) { this.modification = modification; }
    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }
}
