package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.RecallTask;

import java.util.List;
import java.util.Optional;

/**
 * 召回任务仓储接口。
 */
public interface RecallTaskRepository {
    RecallTask save(RecallTask task);
    Optional<RecallTask> findById(String taskId);
    List<RecallTask> findByUserId(String userId);
    List<RecallTask> findByUserIdAndStatus(String userId, String status);
    int countByUserIdAndStatus(String userId, String status);
    List<RecallTask> findRecentByUserId(String userId, int hoursAgo, int limit);
    List<RecallTask> findByStatus(String status, int limit);
}
