package com.solra.not.infrastructure.persistence;

import com.solra.not.domain.model.NotificationPriority;
import com.solra.not.domain.model.NotificationTemplate;
import com.solra.not.domain.model.NotificationType;
import com.solra.not.domain.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * NotificationTemplateRepositoryImpl — 通知模板仓储内存实现。
 * NOT-004: 系统通知模板管理。
 * 生产环境应替换为 JPA + PostgreSQL 实现。
 */
@Repository
public class NotificationTemplateRepositoryImpl implements NotificationTemplateRepository {

    private static final Logger log = LoggerFactory.getLogger(NotificationTemplateRepositoryImpl.class);
    private final Map<String, NotificationTemplate> store = new ConcurrentHashMap<>();

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        store.put(template.getTemplateId(), template);
        log.debug("Saved template: {}", template.getTemplateCode());
        return template;
    }

    @Override
    public Optional<NotificationTemplate> findById(String templateId) {
        return Optional.ofNullable(store.get(templateId));
    }

    @Override
    public Optional<NotificationTemplate> findByTemplateCode(String templateCode) {
        return store.values().stream()
                .filter(t -> t.getTemplateCode().equals(templateCode))
                .findFirst();
    }

    @Override
    public List<NotificationTemplate> findByType(NotificationType type) {
        return store.values().stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplate> findByCategory(String category) {
        return store.values().stream()
                .filter(t -> category.equals(t.getCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplate> findAllActive() {
        return store.values().stream()
                .filter(NotificationTemplate::isActive)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationTemplate> findAll(int page, int size) {
        return store.values().stream()
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public void delete(String templateId) {
        store.remove(templateId);
        log.debug("Deleted template: {}", templateId);
    }
}
