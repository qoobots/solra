package com.solra.not.infrastructure.persistence;

import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepo;

    public NotificationRepositoryImpl(NotificationJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public Notification save(Notification n) {
        NotificationEntity e = toEntity(n);
        NotificationEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public List<Notification> findByUserId(String userId, int page, int size) {
        return jpaRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Notification> findUnreadByUserId(String userId) {
        return jpaRepo.findUnreadByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Notification> findById(String notificationId) {
        return jpaRepo.findById(notificationId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void markAsRead(String notificationId) {
        jpaRepo.markAsRead(notificationId);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        jpaRepo.markAllAsRead(userId);
    }

    @Override
    public long getUnreadCount(String userId) {
        return jpaRepo.countUnreadByUserId(userId);
    }

    @Override
    public void delete(String notificationId) {
        jpaRepo.deleteById(notificationId);
    }

    Notification toDomain(NotificationEntity e) {
        Notification n = new Notification();
        n.setNotificationId(e.getNotificationId());
        n.setUserId(e.getUserId());
        n.setType(NotificationType.valueOf(e.getType()));
        n.setPriority(NotificationPriority.valueOf(e.getPriority()));
        n.setTitle(e.getTitle());
        n.setBody(e.getBody());
        n.setImageUrl(e.getImageUrl());
        n.setDeepLink(e.getDeepLink());
        n.setStatus(NotificationStatus.valueOf(e.getStatus()));
        n.setMetadata(e.getMetadata());
        n.setCreatedAt(e.getCreatedAt());
        n.setReadAt(e.getReadAt());
        n.setExpiresAt(e.getExpiresAt());
        return n;
    }

    NotificationEntity toEntity(Notification n) {
        NotificationEntity e = new NotificationEntity();
        e.setNotificationId(n.getNotificationId());
        e.setUserId(n.getUserId());
        e.setType(n.getType().name());
        e.setPriority(n.getPriority().name());
        e.setTitle(n.getTitle());
        e.setBody(n.getBody());
        e.setImageUrl(n.getImageUrl());
        e.setDeepLink(n.getDeepLink());
        e.setStatus(n.getStatus().name());
        e.setMetadata(n.getMetadata());
        e.setCreatedAt(n.getCreatedAt());
        e.setReadAt(n.getReadAt());
        e.setExpiresAt(n.getExpiresAt());
        return e;
    }
}
