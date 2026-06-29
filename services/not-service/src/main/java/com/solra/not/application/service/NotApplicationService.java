package com.solra.not.application.service;

import com.solra.not.application.dto.*;
import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.*;
import com.solra.not.domain.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NotApplicationService — NOT-001/002 通知消息应用层服务。
 * 编排推送通知和应用内消息中心流程。
 */
@Service
public class NotApplicationService {

    private static final Logger log = LoggerFactory.getLogger(NotApplicationService.class);

    private final NotificationRepository notificationRepo;
    private final PushMessageRepository pushMessageRepo;
    private final DeviceRegistrationRepository deviceRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final InboxMessageRepository inboxRepo;
    private final NotificationService notificationService;

    public NotApplicationService(NotificationRepository notificationRepo,
                                  PushMessageRepository pushMessageRepo,
                                  DeviceRegistrationRepository deviceRepo,
                                  NotificationPreferenceRepository prefRepo,
                                  InboxMessageRepository inboxRepo,
                                  NotificationService notificationService) {
        this.notificationRepo = notificationRepo;
        this.pushMessageRepo = pushMessageRepo;
        this.deviceRepo = deviceRepo;
        this.prefRepo = prefRepo;
        this.inboxRepo = inboxRepo;
        this.notificationService = notificationService;
    }

    // ===== 通知发送 =====

    /** 发送通知 */
    public List<NotificationResultDTO> sendNotification(SendNotificationCommand cmd) {
        log.info("NOT-001 sendNotification: targets={} type={}", cmd.getTargetUserIds().size(), cmd.getType());

        List<Notification> notifications = new ArrayList<>();
        NotificationType type = NotificationType.valueOf(cmd.getType());
        NotificationPriority priority = NotificationPriority.valueOf(
                cmd.getPriority() != null ? cmd.getPriority().toUpperCase() : "NORMAL");

        for (String userId : cmd.getTargetUserIds()) {
            Notification notif = new Notification(
                    UUID.randomUUID().toString(), userId, type, priority,
                    cmd.getTitle(), cmd.getBody());
            notif.setImageUrl(cmd.getImageUrl());
            notif.setDeepLink(cmd.getDeepLink());
            if (cmd.getExpiresInHours() > 0) {
                notif.setExpiresAt(Instant.now().plus(cmd.getExpiresInHours(), ChronoUnit.HOURS));
            }
            notifications.add(notif);
        }

        List<Notification> sent = notificationService.send(cmd.getTargetUserIds(), notifications.get(0));
        log.info("NOT-001 sent {} notifications", sent.size());
        return sent.stream().map(NotificationResultDTO::from).collect(Collectors.toList());
    }

    /** 获取收件箱 */
    public InboxPageDTO getInbox(String userId, int page, int size) {
        List<Notification> items = notificationService.getInbox(userId, page, size);
        long unreadCount = notificationService.getUnreadCount(userId);
        return new InboxPageDTO(
                items.stream().map(NotificationResultDTO::from).collect(Collectors.toList()),
                items.size(), unreadCount);
    }

    /** 标记已读 */
    public void markAsRead(String notificationId) {
        notificationRepo.markAsRead(notificationId);
    }

    /** 全部已读 */
    public void markAllAsRead(String userId) {
        notificationRepo.markAllAsRead(userId);
    }

    /** 未读计数 */
    public long getUnreadCount(String userId) {
        return notificationRepo.getUnreadCount(userId);
    }

    /** 删除通知 */
    public void deleteNotification(String notificationId) {
        notificationRepo.delete(notificationId);
    }

    // ===== 设备管理 =====

    /** 注册设备 */
    public DeviceRegistrationResultDTO registerDevice(RegisterDeviceCommand cmd) {
        log.info("NOT-001 registerDevice: user={} platform={}", cmd.userId(), cmd.platform());

        Platform platform = Platform.valueOf(cmd.platform().toUpperCase());
        PushProvider pushProvider = platform == Platform.IOS ? PushProvider.APNS
                : platform == Platform.ANDROID ? PushProvider.FCM : PushProvider.WEBPUSH;

        DeviceRegistration device = new DeviceRegistration(
                UUID.randomUUID().toString(), cmd.userId(), cmd.deviceToken(),
                platform, pushProvider, cmd.deviceName());
        device.setAppVersion(cmd.appVersion());
        device.setOsVersion(cmd.osVersion());

        DeviceRegistration saved = deviceRepo.save(device);
        return new DeviceRegistrationResultDTO(
                saved.getRegistrationId(), saved.getUserId(),
                saved.getPlatform().name(), saved.getStatus().name(), saved.getCreatedAt());
    }

    /** 取消注册设备 */
    public void unregisterDevice(String registrationId) {
        deviceRepo.updateStatus(registrationId, "UNREGISTERED");
    }

    /** 获取用户设备列表 */
    public List<DeviceRegistrationResultDTO> getUserDevices(String userId) {
        return deviceRepo.findByUserId(userId).stream()
                .map(d -> new DeviceRegistrationResultDTO(
                        d.getRegistrationId(), d.getUserId(), d.getPlatform().name(),
                        d.getStatus().name(), d.getCreatedAt()))
                .collect(Collectors.toList());
    }

    // ===== 偏好管理 =====

    /** 更新通知偏好 */
    public PreferenceResultDTO updatePreference(PreferenceUpdateCommand cmd) {
        NotificationType type = NotificationType.valueOf(cmd.notificationType());
        DeliveryChannel channel = DeliveryChannel.valueOf(cmd.channel().toUpperCase());

        Optional<NotificationPreference> existing = prefRepo.findByUserIdAndType(cmd.userId(), cmd.notificationType());
        NotificationPreference pref;
        if (existing.isPresent()) {
            pref = existing.get();
            pref.setEnabled(cmd.enabled());
            pref.setChannel(channel);
            prefRepo.update(pref);
        } else {
            pref = new NotificationPreference(UUID.randomUUID().toString(),
                    cmd.userId(), type, channel, cmd.enabled());
            prefRepo.save(pref);
        }

        return new PreferenceResultDTO(pref.getPrefId(), pref.getUserId(),
                pref.getNotificationType().name(), pref.getChannel().name(), pref.isEnabled());
    }

    /** 获取通知偏好 */
    public List<PreferenceResultDTO> getPreferences(String userId) {
        return prefRepo.findByUserId(userId).stream()
                .map(p -> new PreferenceResultDTO(p.getPrefId(), p.getUserId(),
                        p.getNotificationType().name(), p.getChannel().name(), p.isEnabled()))
                .collect(Collectors.toList());
    }

    // ===== NOT-002: 应用内消息中心 =====

    /**
     * 发送收件箱消息。
     */
    public InboxMessageResultDTO sendInboxMessage(SendInboxMessageCommand cmd) {
        log.info("NOT-002 sendInboxMessage: from={} to={} type={}", cmd.getSenderId(), cmd.getRecipientId(), cmd.getType());

        MessageType type = cmd.getType() != null ? MessageType.valueOf(cmd.getType()) : MessageType.TEXT;
        InboxMessage message = new InboxMessage(
                UUID.randomUUID().toString(), cmd.getSenderId(), cmd.getRecipientId(),
                type, cmd.getTitle(), cmd.getContent());
        message.setAttachmentUrl(cmd.getAttachmentUrl());
        message.setMetadata(cmd.getMetadata());
        message.setConversationId(cmd.getConversationId());

        message = inboxRepo.save(message);
        return toInboxDTO(message);
    }

    /**
     * 获取用户收件箱。
     */
    public InboxPageResultDTO getInbox(String userId, int page, int size) {
        List<InboxMessage> messages = inboxRepo.findByRecipientId(userId, page, size);
        long unreadCount = inboxRepo.countUnreadByRecipientId(userId);

        List<InboxMessageResultDTO> items = messages.stream()
                .map(this::toInboxDTO)
                .collect(Collectors.toList());

        return new InboxPageResultDTO(items, items.size(), unreadCount);
    }

    /**
     * 获取未读消息列表。
     */
    public List<InboxMessageResultDTO> getUnreadMessages(String userId) {
        return inboxRepo.findUnreadByRecipientId(userId).stream()
                .map(this::toInboxDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取未读消息计数。
     */
    public long getInboxUnreadCount(String userId) {
        return inboxRepo.countUnreadByRecipientId(userId);
    }

    /**
     * 标记收件箱消息已读。
     */
    public void markInboxMessageRead(String messageId) {
        inboxRepo.markAsRead(messageId);
    }

    /**
     * 全部标记已读。
     */
    public void markAllInboxRead(String userId) {
        inboxRepo.markAllAsRead(userId);
    }

    /**
     * 删除收件箱消息。
     */
    public void deleteInboxMessage(String messageId) {
        inboxRepo.deleteById(messageId);
    }

    /**
     * 获取会话消息列表。
     */
    public List<InboxMessageResultDTO> getConversationMessages(String conversationId, int page, int size) {
        return inboxRepo.findByConversationId(conversationId, page, size).stream()
                .map(this::toInboxDTO)
                .collect(Collectors.toList());
    }

    private InboxMessageResultDTO toInboxDTO(InboxMessage m) {
        return new InboxMessageResultDTO(
                m.getMessageId(), m.getSenderId(), m.getRecipientId(),
                m.getType() != null ? m.getType().name() : null,
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getTitle(), m.getContent(), m.getAttachmentUrl(),
                m.getConversationId(), m.getSentAt(), m.getReadAt());
    }
}
