package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.AvatarCollection;
import java.util.Optional;

/**
 * 虚拟人图鉴仓储接口。
 */
public interface AvatarCollectionRepository {
    Optional<AvatarCollection> findByUserId(String userId);
    AvatarCollection save(AvatarCollection collection);
    long count();
}
