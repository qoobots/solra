package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.ExperienceEvent;
import java.time.Instant;
import java.util.List;

/** 经验事件仓储接口 */
public interface ExperienceEventRepository {
    ExperienceEvent save(ExperienceEvent event);
    List<ExperienceEvent> findByUserId(String userId, int limit);
    List<ExperienceEvent> findByUserIdAndTimeRange(String userId, Instant from, Instant to);
    int sumValueByUserId(String userId);
}
