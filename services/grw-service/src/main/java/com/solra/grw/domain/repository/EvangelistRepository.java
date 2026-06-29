package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.Evangelist;
import java.util.List;
import java.util.Optional;

/**
 * 布道者仓储接口。
 */
public interface EvangelistRepository {
    Optional<Evangelist> findById(String applicationId);
    Optional<Evangelist> findByUserId(String userId);
    List<Evangelist> findByStatus(Evangelist.ApplicationStatus status, int page, int size);
    List<Evangelist> findByTier(Evangelist.EvangelistTier tier, int page, int size);
    List<Evangelist> findActive(int page, int size);
    Evangelist save(Evangelist evangelist);
    long countByStatus(Evangelist.ApplicationStatus status);
    long countByTier(Evangelist.EvangelistTier tier);
    long countAll();
    /** 查询申请列表 */
    List<Evangelist> findApplications(int page, int size);
}
