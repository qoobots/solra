package com.solra.grw.domain.repository;

import com.solra.grw.domain.model.UserProfile;
import java.util.Optional;

/** 用户画像仓储接口 */
public interface UserProfileRepository {
    UserProfile save(UserProfile profile);
    Optional<UserProfile> findByUserId(String userId);
    void updatePresenceScore(String userId, double delta);
    void updateFaithLevel(String userId, String newLevel);
}
