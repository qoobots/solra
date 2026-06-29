package com.solra.spc.domain.repository;

import com.solra.spc.domain.model.UserAction;
import java.util.List;

public interface UserActionRepository {
    void save(UserAction action);
    List<UserAction> findByUserId(String userId, int limit);
    List<UserAction> findByUserId(String userId, int offset, int limit);
    List<UserAction> findBySpaceId(String spaceId, int limit);
}
