package com.solra.not.infrastructure.engine;

import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.*;
import com.solra.not.domain.service.NotificationService;
import com.solra.not.domain.service.SmartPushEngine;
import com.solra.not.infrastructure.push.PushDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * NotificationServiceImpl — 通知领域服务默认实现。
 * 创建通知实体并分发到各设备推送通道。
 * NOT-001: 基础推送通知 | NOT-003: 智能推送策略
 */
@Component
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepo;
    private final DeviceRegistrationRepository deviceRepo;
    private final PushMessageRepository pushMessageRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final PushDispatcher pushDispatcher;
    private final SmartPushEngine smartPushEngine;

    public NotificationServiceImpl(NotificationRepository notificationRepo,
                                    DeviceRegistrationRepository deviceRepo,
                                    PushMessageRepository pushMessageRepo,
                                    NotificationPreferenceRepository prefRepo,
                                    PushDispatcher pushDispatcher,
                                    SmartPushEngine smartPushEngine) {
        this.notificationRepo = notificationRepo;
        this.deviceRepo = deviceRepo;
        this.pushMessageRepo = pushMessageRepo;
        this.prefRepo = prefRepo;
        this.pushDispatcher = pushDispatcher;
        this.smartPushEngine = smartPushEngine;
    }

    @Override
    public List<Notification> send(List<String> toUserIds, Notification template) {
        return send(toUserIds, template, false);
    }

    /**
     * 发送通知（带智能推送策略）。
     * NOT-003: 智能推送策略 —— 静默时段检测 + 频率控制 + 偏好过滤 + 优先级感知。
     *
     * @param toUserIds 目标用户ID列表
     * @param template  通知模板
     * @param bypassSmartEngine 是否绕过智能引擎（URGENT 优先级自动绕过）
     * @return 已发送的通知列表
     */
    public List<Notification> send(List<String> toUserIds, Notification template, boolean bypassSmartEngine) {
        List<Notification> results = new ArrayList<>();
        boolean isUrgent = template.getPriority() == NotificationPriority.URGENT;

        for (String userId : toUserIds) {
            // 智能推送决策（URGENT 自动绕过）
            if (!bypassSmartEngine && !isUrgent) {
                SmartPushEngine.PushDecision decision = smartPushEngine.evaluate(
                        userId, template.getType(), false);
                switch (decision.decision()) {
                    case BLOCKED_BY_PREFERENCE:
                        log.debug("NOT-003 blocked: user={} reason={}", userId, decision.reason());
                        continue;
                    case BLOCKED_BY_RATE_LIMIT:
                        log.info("NOT-003 rate limited: user={} type={}", userId, template.getType());
                        // 仍然创建通知记录但不推送
                        Notification rateLimited = createAndSaveNotification(userId, template);
                        rateLimited.dismiss();
                        notificationRepo.save(rateLimited);
                        results.add(rateLimited);
                        continue;
                    case DEFER:
                        log.info("NOT-003 deferred: user={} reason={}", userId, decision.reason());
                        // 创建通知但不立即推送，标记为延迟
                        Notification deferred = createAndSaveNotification(userId, template);
                        deferred.setMetadata("{\"deferred\":true,\"reason\":\"" + decision.reason() + "\"}");
                        results.add(deferred);
                        continue;
                    case DOWNGRADE_TO_IN_APP:
                        log.info("NOT-003 downgraded to in-app: user={} reason={}", userId, decision.reason());
                        // 降级为仅应用内消息
                        Notification downgraded = createAndSaveNotification(userId, template);
                        downgraded.sent();
                        notificationRepo.save(downgraded);
                        results.add(downgraded);
                        continue;
                    case ALLOW:
                        // 继续推送
                        break;
                }
            }

            // 创建并保存通知
            Notification notif = createAndSaveNotification(userId, template);
            notif.sent();
            notificationRepo.save(notif);
            results.add(notif);

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
                        template.getTitle(), template.getBody(), isUrgent);

                PushMessage push = new PushMessage(
                        UUID.randomUUID().toString(), notif.getNotificationId(),
                        device.getDeviceToken(), device.getPlatform(), device.getPushProvider());

                if (result.success()) {
                    push.sent(result.providerMessageId());
                    notif.delivered();
                } else {
                    push.failed(result.error());
                }

                pushMessageRepo.save(push);
            }
        }

        log.info("NOT-001/NOT-003 sent {} notifications to {} users", results.size(), toUserIds.size());
        return results;
    }

    private Notification createAndSaveNotification(String userId, Notification template) {
        Notification notif = new Notification(
                UUID.randomUUID().toString(), userId, template.getType(),
                template.getPriority(), template.getTitle(), template.getBody());
        notif.setImageUrl(template.getImageUrl());
        notif.setDeepLink(template.getDeepLink());
        notif.setExpiresAt(template.getExpiresAt());
        return notificationRepo.save(notif);
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
