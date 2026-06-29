package com.solra.saf.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReviewCaseRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReviewCaseRepositoryImpl implements ReviewCaseRepository {

    private final ReviewCaseJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ReviewCaseRepositoryImpl(ReviewCaseJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ReviewCase> findById(String caseId) {
        return jpaRepository.findById(caseId).map(this::toDomain);
    }

    @Override
    public List<ReviewCase> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ReviewCase> findPending(ReviewStatus status, int limit) {
        return jpaRepository.findByStatusOrderByCreatedAtAsc(status.name())
                .stream().limit(limit).map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public ReviewCase save(ReviewCase reviewCase) {
        ReviewCaseEntity entity = toEntity(reviewCase);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public long countByUserId(String userId) {
        return jpaRepository.countByUserId(userId);
    }

    private ReviewCaseEntity toEntity(ReviewCase domain) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(domain.getCaseId());
        e.setUserId(domain.getUserId());
        e.setContentType(domain.getTarget().getContentType().name());
        e.setContentText(domain.getTarget().getContentText());
        e.setContentUrl(domain.getTarget().getContentUrl());
        e.setContentHash(domain.getTarget().getContentHash());
        e.setContentId(domain.getTarget().getContentId());
        e.setReviewType(domain.getReviewType().name());
        e.setStatus(domain.getStatus().name());
        e.setDecision(domain.getDecision() != null ? domain.getDecision().name() : null);
        e.setPriority(domain.getPriority().name());
        e.setCreatedAt(domain.getSubmittedAt());
        e.setReviewedAt(domain.getReviewedAt());
        e.setReviewerId(domain.getReviewerId());

        try {
            e.setViolationsJson(objectMapper.writeValueAsString(domain.getViolations()));
            e.setMetadataJson(objectMapper.writeValueAsString(domain.getMetadata()));
        } catch (Exception ex) {
            e.setViolationsJson("[]");
            e.setMetadataJson("{}");
        }
        return e;
    }

    private ReviewCase toDomain(ReviewCaseEntity entity) {
        // Build via reflection since constructor is private
        ContentTarget target = ContentTarget.text(entity.getContentId(), entity.getContentText());
        ReviewCase rc = ReviewCase.create(entity.getUserId(), target,
                ReviewType.valueOf(entity.getReviewType()),
                ReviewPriority.valueOf(entity.getPriority()));

        try {
            java.lang.reflect.Field caseIdF = ReviewCase.class.getDeclaredField("caseId");
            caseIdF.setAccessible(true);
            caseIdF.set(rc, entity.getCaseId());

            java.lang.reflect.Field statusF = ReviewCase.class.getDeclaredField("status");
            statusF.setAccessible(true);
            statusF.set(rc, ReviewStatus.valueOf(entity.getStatus()));

            if (entity.getDecision() != null) {
                java.lang.reflect.Field decisionF = ReviewCase.class.getDeclaredField("decision");
                decisionF.setAccessible(true);
                decisionF.set(rc, ReviewDecision.valueOf(entity.getDecision()));
            }

            java.lang.reflect.Field reviewedAtF = ReviewCase.class.getDeclaredField("reviewedAt");
            reviewedAtF.setAccessible(true);
            reviewedAtF.set(rc, entity.getReviewedAt());

            java.lang.reflect.Field reviewerIdF = ReviewCase.class.getDeclaredField("reviewerId");
            reviewerIdF.setAccessible(true);
            reviewerIdF.set(rc, entity.getReviewerId());

        } catch (Exception e) {
            throw new RuntimeException("Failed to map ReviewCase entity to domain", e);
        }

        return rc;
    }
}
