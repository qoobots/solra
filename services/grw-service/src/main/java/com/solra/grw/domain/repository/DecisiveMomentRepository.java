package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.DecisiveMoment;
import java.util.List;
import java.util.Optional;

/** 决定性时刻仓储接口 */
public interface DecisiveMomentRepository {
    DecisiveMoment save(DecisiveMoment moment);
    List<DecisiveMoment> findByUserId(String userId);
    Optional<DecisiveMoment> findByUserIdAndType(String userId, String momentType);
}
