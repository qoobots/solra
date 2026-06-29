package com.solra.not.domain.repository;

import com.solra.not.domain.model.Notification;
import java.util.List;
import java.util.Optional;

/** 通知仓储接口 */
public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> findByUserId(String userId, int page, int size);
    List<Notification> findUnreadByUserId(String userId);
    Optional<Notification> findById(String notificationId);
    void markAsRead(String notificationId);
    void markAllAsRead(String userId);
    long getUnreadCount(String userId);
    void delete(String notificationId);
}
