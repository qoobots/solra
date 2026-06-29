package com.solra.soc.domain.repository;

import com.solra.soc.domain.model.ShareSession;

import java.util.List;
import java.util.Optional;

/**
 * ShareSession 仓储接口。
 * <p>
 * 定义领域层对分享会话的持久化操作契约，不依赖任何基础设施框架。
 */
public interface ShareSessionRepository {

    /**
     * 保存（新增或更新）一个分享会话。
     */
    ShareSession save(ShareSession session);

    Optional<ShareSession> findByShareCode(String shareCode);

    List<ShareSession> findBySharerUserId(String sharerUserId);

    void update(ShareSession session);
}
