package com.solra.not.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface NotificationJpaRepository extends JpaRepository<NotificationEntity, String> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT n FROM NotificationEntity n WHERE n.userId = :userId AND n.status IN ('PENDING','SENT','DELIVERED')")
    List<NotificationEntity> findUnreadByUserId(String userId);

    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId AND n.status IN ('PENDING','SENT','DELIVERED')")
    long countUnreadByUserId(String userId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'READ', n.readAt = CURRENT_TIMESTAMP WHERE n.notificationId = :notificationId")
    void markAsRead(String notificationId);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.status = 'READ', n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.status IN ('PENDING','SENT','DELIVERED')")
    void markAllAsRead(String userId);
}

@Repository
interface PushMessageJpaRepository extends JpaRepository<PushMessageEntity, String> {}

@Repository
interface DeviceRegistrationJpaRepository extends JpaRepository<DeviceRegistrationEntity, String> {
    List<DeviceRegistrationEntity> findByUserId(String userId);
    java.util.Optional<DeviceRegistrationEntity> findByDeviceToken(String deviceToken);
    List<DeviceRegistrationEntity> findByUserIdAndStatus(String userId, String status);
}

@Repository
interface NotificationPreferenceJpaRepository extends JpaRepository<NotificationPreferenceEntity, String> {
    java.util.Optional<NotificationPreferenceEntity> findByUserIdAndNotificationType(String userId, String notificationType);
    List<NotificationPreferenceEntity> findByUserId(String userId);
}

@Repository
interface InboxMessageJpaRepository extends JpaRepository<InboxMessageEntity, String> {
    List<InboxMessageEntity> findByRecipientIdOrderBySentAtDesc(String recipientId, Pageable pageable);
    List<InboxMessageEntity> findByRecipientIdAndStatusIn(String recipientId, List<String> statuses);
    long countByRecipientIdAndStatusIn(String recipientId, List<String> statuses);
    List<InboxMessageEntity> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);
}
