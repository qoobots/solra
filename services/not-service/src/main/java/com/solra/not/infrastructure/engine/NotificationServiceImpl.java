package com.solra.not.infrastructure.engine;

import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.*;
import com.solra.not.domain.service.NotificationService;
import com.solra.not.infrastructure.push.PushDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * NotificationServiceImpl — 通知领域服务默认实现。
 * 创建通知实体并分发到各设备推送通道。
 */
@Component
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepo;
    private final DeviceRegistrationRepository deviceRepo;
    private final PushMessageRepository pushMessageRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final PushDispatcher pushDispatcher;

    public NotificationServiceImpl(NotificationRepository notificationRepo,
                                    DeviceRegistrationRepository deviceRepo,
                                    PushMessageRepository pushMessageRepo,
                                    NotificationPreferenceRepository prefRepo,
                                    PushDispatcher pushDispatcher) {
        this.notificationRepo = notificationRepo;
        this.deviceRepo = deviceRepo;
        this.pushMessageRepo = pushMessageRepo;
        this.prefRepo = prefRepo;
        this.pushDispatcher = pushDispatcher;
    }

    @Override
    public List<Notification> send(List<String> toUserIds, Notification template) {
        List<Notification> results = new ArrayList<>();

        for (String userId : toUserIds) {
            // 创建独立通知
            Notification notif = new Notification(
                    UUID.randomUUID().toString(), userId, template.getType(),
                    template.getPriority(), template.getTitle(), template.getBody());
            notif.setImageUrl(template.getImageUrl());
            notif.setDeepLink(template.getDeepLink());
            notif.setExpiresAt(template.getExpiresAt());

            Notification saved = notificationRepo.save(notif);
            saved.sent();
            notificationRepo.save(saved);
            results.add(saved);

            // 推送到所有已注册设备
            List<DeviceRegistration> devices = deviceRepo.findByUserId(userId);
            for (DeviceRegistration device : devices) {
                if (!device.isActive()) continue;

                // 检查用户偏好
                Optional<NotificationPreference> pref = prefRepo.findByUserIdAndType(
                        userId, template.getType().name());
                if (pref.isPresent() && !pref.get().isEnabled()) {
                    log.debug("User {} has disabled {} notifications", userId, template.getType());
                    continue;
                }

                // 发送推送
                PushProvider.PushResult result = pushDispatcher.send(
                        device.getPlatform(), device.getDeviceToken(),
                        template.getTitle(), template.getBody());

                PushMessage push = new PushMessage(
                        UUID.randomUUID().toString(), saved.getNotificationId(),
                        device.getDeviceToken(), device.getPlatform(), device.getPushProvider());

                if (result.success()) {
                    push.sent(result.providerMessageId());
                    saved.delivered();
                } else {
                    push.failed(result.error());
                }

                pushMessageRepo.save(push);
            }
        }

        log.info("NOT-001 sent {} notifications to {} users", results.size(), toUserIds.size());
        return results;
    }

    @Override
    public List<Notification> getInbox(String userId, int page, int size) {
        return notificationRepo.findByUserId(userId, page, size);
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepo.getUnreadCount(userId);
    }
}
