package com.solra.grw.infrastructure.persistence;

import com.solra.grw.domain.model.*;
import com.solra.grw.domain.repository.RecallTaskRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RecallTaskRepositoryImpl implements RecallTaskRepository {

    private final RecallTaskJpaRepository jpaRepo;

    public RecallTaskRepositoryImpl(RecallTaskJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public RecallTask save(RecallTask task) {
        return toDomain(jpaRepo.save(toEntity(task)));
    }

    @Override
    public Optional<RecallTask> findById(String taskId) {
        return jpaRepo.findById(taskId).map(this::toDomain);
    }

    @Override
    public List<RecallTask> findByUserId(String userId) {
        return jpaRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RecallTask> findByUserIdAndStatus(String userId, String status) {
        return jpaRepo.findByUserIdAndStatus(userId, status).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public int countByUserIdAndStatus(String userId, String status) {
        return jpaRepo.countByUserIdAndStatus(userId, status);
    }

    @Override
    public List<RecallTask> findRecentByUserId(String userId, int hoursAgo, int limit) {
        Instant since = Instant.now().minus(hoursAgo, ChronoUnit.HOURS);
        return jpaRepo.findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(userId, since,
                        org.springframework.data.domain.PageRequest.of(0, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RecallTask> findByStatus(String status, int limit) {
        return jpaRepo.findByStatusOrderByCreatedAtAsc(status,
                        org.springframework.data.domain.PageRequest.of(0, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private RecallTask toDomain(RecallTaskEntity e) {
        RecallTask t = new RecallTask();
        t.setTaskId(e.getTaskId());
        t.setUserId(e.getUserId());
        t.setStrategyId(e.getStrategyId());
        t.setStrategyName(e.getStrategyName());
        t.setRiskLevel(e.getRiskLevel() != null ? ChurnRiskLevel.valueOf(e.getRiskLevel()) : null);
        t.setInactiveDays(e.getInactiveDays());
        t.setChannel(e.getChannel() != null ? RecallChannel.valueOf(e.getChannel()) : null);
        t.setStatus(e.getStatus() != null ? RecallTaskStatus.valueOf(e.getStatus()) : null);
        t.setTitle(e.getTitle());
        t.setMessage(e.getMessage());
        t.setAttemptNumber(e.getAttemptNumber());
        t.setCreatedAt(e.getCreatedAt());
        t.setSentAt(e.getSentAt());
        t.setClickedAt(e.getClickedAt());
        t.setConvertedAt(e.getConvertedAt());
        return t;
    }

    private RecallTaskEntity toEntity(RecallTask t) {
        RecallTaskEntity e = new RecallTaskEntity();
        e.setTaskId(t.getTaskId());
        e.setUserId(t.getUserId());
        e.setStrategyId(t.getStrategyId());
        e.setStrategyName(t.getStrategyName());
        e.setRiskLevel(t.getRiskLevel() != null ? t.getRiskLevel().name() : null);
        e.setInactiveDays(t.getInactiveDays());
        e.setChannel(t.getChannel() != null ? t.getChannel().name() : null);
        e.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
        e.setTitle(t.getTitle());
        e.setMessage(t.getMessage());
        e.setAttemptNumber(t.getAttemptNumber());
        e.setCreatedAt(t.getCreatedAt());
        e.setSentAt(t.getSentAt());
        e.setClickedAt(t.getClickedAt());
        e.setConvertedAt(t.getConvertedAt());
        return e;
    }
}
