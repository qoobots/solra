package com.solra.not.domain.repository;

import com.solra.not.domain.model.NotificationPreference;
import java.util.List;
import java.util.Optional;

/** 通知偏好仓储接口 */
public interface NotificationPreferenceRepository {
    NotificationPreference save(NotificationPreference pref);
    Optional<NotificationPreference> findByUserIdAndType(String userId, String notificationType);
    List<NotificationPreference> findByUserId(String userId);
    void update(NotificationPreference pref);
}
