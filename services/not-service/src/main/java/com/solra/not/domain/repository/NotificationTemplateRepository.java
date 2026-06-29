package com.solra.not.domain.repository;

import com.solra.not.domain.model.NotificationTemplate;
import com.solra.not.domain.model.NotificationType;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板仓储接口。
 * NOT-004: 系统通知模板管理。
 */
public interface NotificationTemplateRepository {
    NotificationTemplate save(NotificationTemplate template);
    Optional<NotificationTemplate> findById(String templateId);
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
    List<NotificationTemplate> findByType(NotificationType type);
    List<NotificationTemplate> findByCategory(String category);
    List<NotificationTemplate> findAllActive();
    List<NotificationTemplate> findAll(int page, int size);
    long count();
    void delete(String templateId);
}
