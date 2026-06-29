package com.solra.not.infrastructure.persistence;

import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final NotificationPreferenceJpaRepository jpaRepo;

    public NotificationPreferenceRepositoryImpl(NotificationPreferenceJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public NotificationPreference save(NotificationPreference pref) {
        NotificationPreferenceEntity e = toEntity(pref);
        NotificationPreferenceEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<NotificationPreference> findByUserIdAndType(String userId, String notificationType) {
        return jpaRepo.findByUserIdAndNotificationType(userId, notificationType).map(this::toDomain);
    }

    @Override
    public List<NotificationPreference> findByUserId(String userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void update(NotificationPreference pref) {
        jpaRepo.save(toEntity(pref));
    }

    NotificationPreference toDomain(NotificationPreferenceEntity e) {
        NotificationPreference p = new NotificationPreference();
        p.setPrefId(e.getPrefId());
        p.setUserId(e.getUserId());
        p.setNotificationType(NotificationType.valueOf(e.getNotificationType()));
        p.setChannel(DeliveryChannel.valueOf(e.getChannel()));
        p.setEnabled(e.isEnabled());
        p.setQuietHoursStart(e.getQuietHoursStart());
        p.setQuietHoursEnd(e.getQuietHoursEnd());
        return p;
    }

    NotificationPreferenceEntity toEntity(NotificationPreference p) {
        NotificationPreferenceEntity e = new NotificationPreferenceEntity();
        e.setPrefId(p.getPrefId());
        e.setUserId(p.getUserId());
        e.setNotificationType(p.getNotificationType().name());
        e.setChannel(p.getChannel().name());
        e.setEnabled(p.isEnabled());
        e.setQuietHoursStart(p.getQuietHoursStart());
        e.setQuietHoursEnd(p.getQuietHoursEnd());
        return e;
    }
}
