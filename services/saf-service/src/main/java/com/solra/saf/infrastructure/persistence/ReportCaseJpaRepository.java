package com.solra.saf.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportCaseJpaRepository extends JpaRepository<ReportCaseEntity, String> {
    List<ReportCaseEntity> findByReporterUserId(String userId);
    List<ReportCaseEntity> findByStatusOrderBySubmittedAtAsc(String status);
    long countByReporterUserId(String userId);
    long countByReportedUserId(String userId);
}
