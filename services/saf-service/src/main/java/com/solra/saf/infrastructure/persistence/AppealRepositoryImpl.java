package com.solra.saf.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.AppealRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AppealRepositoryImpl implements AppealRepository {

    private final AppealJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public AppealRepositoryImpl(AppealJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Appeal> findById(String appealId) {
        return jpaRepository.findById(appealId).map(this::toDomain);
    }

    @Override
    public List<Appeal> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByCaseId(String caseId) {
        return jpaRepository.findByCaseId(caseId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findByStatus(AppealStatus status) {
        return jpaRepository.findByStatusOrderBySubmittedAtAsc(status.name()).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Appeal> findPendingReview(int limit) {
        return jpaRepository.findByStatusOrderBySubmittedAtAsc(AppealStatus.SUBMITTED.name())
                .stream().limit(limit).map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Appeal save(Appeal appeal) {
        AppealEntity entity = toEntity(appeal);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public boolean existsByCaseIdAndUserId(String caseId, String userId) {
        return jpaRepository.existsByCaseIdAndUserId(caseId, userId);
    }

    private AppealEntity toEntity(Appeal domain) {
        AppealEntity e = new AppealEntity();
        e.setAppealId(domain.getAppealId());
        e.setCaseId(domain.getCaseId());
        e.setUserId(domain.getUserId());
        e.setReason(domain.getReason());
        e.setStatus(domain.getStatus().name());
        e.setDecision(domain.getDecision() != null ? domain.getDecision().name() : null);
        e.setDecisionReason(domain.getDecisionReason());
        e.setReviewerId(domain.getReviewerId());
        e.setSubmittedAt(domain.getSubmittedAt());
        e.setReviewedAt(domain.getReviewedAt());
        e.setResolvedAt(domain.getResolvedAt());
        try {
            e.setEvidenceUrlsJson(objectMapper.writeValueAsString(domain.getEvidenceUrls()));
            e.setMetadataJson(objectMapper.writeValueAsString(domain.getMetadata()));
        } catch (Exception ex) {
            e.setEvidenceUrlsJson("[]");
            e.setMetadataJson("{}");
        }
        return e;
    }

    private Appeal toDomain(AppealEntity entity) {
        try {
            List<String> evidenceUrls = entity.getEvidenceUrlsJson() != null
                    ? objectMapper.readValue(entity.getEvidenceUrlsJson(),
                            new TypeReference<List<String>>() {})
                    : List.of();

            Appeal appeal = Appeal.create(
                    entity.getCaseId(), entity.getUserId(),
                    entity.getReason(), evidenceUrls);

            setField(appeal, "appealId", entity.getAppealId());
            setField(appeal, "status", AppealStatus.valueOf(entity.getStatus()));
            if (entity.getDecision() != null) {
                setField(appeal, "decision", AppealDecision.valueOf(entity.getDecision()));
            }
            setField(appeal, "decisionReason", entity.getDecisionReason());
            setField(appeal, "reviewerId", entity.getReviewerId());
            setField(appeal, "reviewedAt", entity.getReviewedAt());
            setField(appeal, "resolvedAt", entity.getResolvedAt());

            return appeal;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map Appeal entity to domain", e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = Appeal.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
