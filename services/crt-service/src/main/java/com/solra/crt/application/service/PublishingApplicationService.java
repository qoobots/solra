package com.solra.crt.application.service;

import com.solra.crt.domain.entity.SpaceProject;
import com.solra.crt.domain.repository.ProjectRepository;
import com.solra.crt.domain.service.PublishingPipeline;

import java.util.List;
import java.util.Map;

/**
 * 发布审核应用服务 (CRT-008)。
 * 协调 PublishingPipeline 领域服务。
 */
public class PublishingApplicationService {

    private final PublishingPipeline publishingPipeline;
    private final ProjectRepository projectRepository;

    public PublishingApplicationService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
        this.publishingPipeline = new PublishingPipeline(projectRepository);
    }

    /**
     * 提交项目进行审核。
     */
    public PublishingPipeline.ReviewSubmission submitForReview(String projectId, String submitterId, String notes) {
        return publishingPipeline.submitForReview(projectId, submitterId, notes);
    }

    /**
     * 审核员认领审核任务。
     */
    public PublishingPipeline.ReviewSubmission claimReview(String reviewId, String reviewerId) {
        return publishingPipeline.claimReview(reviewId, reviewerId);
    }

    /**
     * 审核通过。
     */
    public PublishingPipeline.ReviewSubmission approveReview(String reviewId, String reviewerId, String comment) {
        return publishingPipeline.approveReview(reviewId, reviewerId, comment);
    }

    /**
     * 审核驳回。
     */
    public PublishingPipeline.ReviewSubmission rejectReview(String reviewId, String reviewerId, String reason) {
        return publishingPipeline.rejectReview(reviewId, reviewerId, reason);
    }

    /**
     * 获取项目审核历史。
     */
    public List<PublishingPipeline.ReviewRecord> getReviewHistory(String projectId) {
        return publishingPipeline.getReviewHistory(projectId);
    }

    /**
     * 获取待审核列表。
     */
    public List<PublishingPipeline.ReviewRecord> getPendingReviews() {
        return publishingPipeline.getPendingReviews();
    }

    /**
     * 获取审核员的工作队列。
     */
    public List<PublishingPipeline.ReviewRecord> getReviewerQueue(String reviewerId) {
        return publishingPipeline.getReviewerQueue(reviewerId);
    }

    /**
     * 获取审核统计。
     */
    public Map<String, Object> getReviewStats() {
        return publishingPipeline.getReviewStats();
    }

    /**
     * 获取审核管线实例（用于测试和高级场景）。
     */
    public PublishingPipeline getPipeline() {
        return publishingPipeline;
    }
}
