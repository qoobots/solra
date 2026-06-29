package com.solra.saf.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solra.saf.domain.model.*;
import com.solra.saf.domain.repository.ReportCaseRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReportCaseRepositoryImpl implements ReportCaseRepository {

    private final ReportCaseJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public ReportCaseRepositoryImpl(ReportCaseJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ReportCase> findById(String reportId) {
        return jpaRepository.findById(reportId).map(this::toDomain);
    }

    @Override
    public List<ReportCase> findByReporterUserId(String userId) {
        return jpaRepository.findByReporterUserId(userId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ReportCase> findByStatus(ReportStatus status) {
        return jpaRepository.findByStatusOrderBySubmittedAtAsc(status.name()).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ReportCase> findPendingReview(int limit) {
        return jpaRepository.findByStatusOrderBySubmittedAtAsc(ReportStatus.SUBMITTED.name())
                .stream().limit(limit).map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public ReportCase save(ReportCase reportCase) {
        ReportCaseEntity entity = toEntity(reportCase);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public long countByReporterUserId(String userId) {
        return jpaRepository.countByReporterUserId(userId);
    }

    @Override
    public long countByReportedUserId(String userId) {
        return jpaRepository.countByReportedUserId(userId);
    }

    private ReportCaseEntity toEntity(ReportCase domain) {
        ReportCaseEntity e = new ReportCaseEntity();
        e.setReportId(domain.getReportId());
        e.setReporterUserId(domain.getReporterUserId());
        e.setReportedUserId(domain.getReportedUserId());
        e.setReportedContentId(domain.getReportedContentId());
        e.setReportReason(domain.getReportReason());
        e.setStatus(domain.getStatus().name());
        e.setCategory(domain.getCategory().name());
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

    private ReportCase toDomain(ReportCaseEntity entity) {
        try {
            List<String> evidenceUrls = entity.getEvidenceUrlsJson() != null
                    ? objectMapper.readValue(entity.getEvidenceUrlsJson(),
                            new TypeReference<List<String>>() {})
                    : List.of();

            ReportCase rc = ReportCase.create(
                    entity.getReporterUserId(),
                    entity.getReportedUserId(),
                    entity.getReportedContentId(),
                    entity.getReportReason(),
                    ReportCategory.valueOf(entity.getCategory()),
                    evidenceUrls);

            // Restore fields via reflection
            setField(rc, "reportId", entity.getReportId());
            setField(rc, "status", ReportStatus.valueOf(entity.getStatus()));
            if (entity.getDecision() != null) {
                setField(rc, "decision", ReviewDecision.valueOf(entity.getDecision()));
            }
            setField(rc, "decisionReason", entity.getDecisionReason());
            setField(rc, "reviewerId", entity.getReviewerId());
            setField(rc, "reviewedAt", entity.getReviewedAt());
            setField(rc, "resolvedAt", entity.getResolvedAt());

            return rc;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map ReportCase entity to domain", e);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = ReportCase.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
