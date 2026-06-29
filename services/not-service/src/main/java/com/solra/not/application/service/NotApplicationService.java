package com.solra.not.application.service;

import com.solra.not.application.dto.*;
import com.solra.not.domain.model.*;
import com.solra.not.domain.repository.*;
import com.solra.not.domain.service.NotificationService;
import com.solra.not.domain.service.SmartPushEngine;
import com.solra.not.domain.service.TemplateRenderEngine;
import com.solra.not.infrastructure.engine.NotificationServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NotApplicationService — 通知消息应用层服务。
 * NOT-001: 推送通知系统 | NOT-002: 应用内消息中心
 * NOT-003: 智能推送策略 | NOT-004: 系统通知模板管理
 */
@Service
public class NotApplicationService {

    private static final Logger log = LoggerFactory.getLogger(NotApplicationService.class);

    private final NotificationRepository notificationRepo;
    private final PushMessageRepository pushMessageRepo;
    private final DeviceRegistrationRepository deviceRepo;
    private final NotificationPreferenceRepository prefRepo;
    private final InboxMessageRepository inboxRepo;
    private final NotificationTemplateRepository templateRepo;
    private final NotificationService notificationService;
    private final SmartPushEngine smartPushEngine;
    private final TemplateRenderEngine templateRenderEngine;

    public NotApplicationService(NotificationRepository notificationRepo,
                                  PushMessageRepository pushMessageRepo,
                                  DeviceRegistrationRepository deviceRepo,
                                  NotificationPreferenceRepository prefRepo,
                                  InboxMessageRepository inboxRepo,
                                  NotificationTemplateRepository templateRepo,
                                  NotificationService notificationService,
                                  SmartPushEngine smartPushEngine,
                                  TemplateRenderEngine templateRenderEngine) {
        this.notificationRepo = notificationRepo;
        this.pushMessageRepo = pushMessageRepo;
        this.deviceRepo = deviceRepo;
        this.prefRepo = prefRepo;
        this.inboxRepo = inboxRepo;
        this.templateRepo = templateRepo;
        this.notificationService = notificationService;
        this.smartPushEngine = smartPushEngine;
        this.templateRenderEngine = templateRenderEngine;
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

    // ===== NOT-003: 智能推送策略 =====

    /**
     * 发送通知（带智能推送策略）。
     * 智能引擎评估静默时段、频率限制、用户偏好后决定是否推送。
     */
    public List<NotificationResultDTO> smartSendNotification(SmartSendNotificationCommand cmd) {
        log.info("NOT-003 smartSendNotification: targets={} type={} bypassSmart={}",
                cmd.getTargetUserIds().size(), cmd.getType(), cmd.isBypassSmartEngine());

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

        // 使用带智能引擎的发送
        NotificationServiceImpl impl = (NotificationServiceImpl) notificationService;
        List<Notification> sent = impl.send(cmd.getTargetUserIds(), notifications.get(0),
                cmd.isBypassSmartEngine());
        log.info("NOT-003 smart sent {} notifications", sent.size());
        return sent.stream().map(NotificationResultDTO::from).collect(Collectors.toList());
    }

    /**
     * 评估推送决策（不实际发送）。
     */
    public SmartPushDecisionDTO evaluatePush(String userId, String type, boolean isUrgent) {
        NotificationType notifType = NotificationType.valueOf(type);
        SmartPushEngine.PushDecision decision = smartPushEngine.evaluate(userId, notifType, isUrgent);
        return new SmartPushDecisionDTO(
                decision.decision().name(), decision.reason(),
                decision.shouldSendPush(), decision.shouldSendInApp());
    }

    /**
     * 获取用户推送统计。
     */
    public PushStatsDTO getUserPushStats(String userId) {
        Map<NotificationType, Integer> stats = smartPushEngine.getUserDailyStats(userId);
        Map<String, Integer> result = new HashMap<>();
        stats.forEach((k, v) -> result.put(k.name(), v));
        return new PushStatsDTO(userId, result);
    }

    /**
     * 检查用户是否处于静默时段。
     */
    public boolean isInQuietHours(String userId) {
        return smartPushEngine.isInQuietHours(userId);
    }

    // ===== NOT-004: 通知模板管理 =====

    /**
     * 创建通知模板。
     */
    public TemplateDTO createTemplate(TemplateCommand cmd) {
        log.info("NOT-004 createTemplate: code={} name={}", cmd.templateCode(), cmd.name());

        NotificationType type = NotificationType.valueOf(cmd.type());
        NotificationPriority priority = NotificationPriority.valueOf(
                cmd.defaultPriority() != null ? cmd.defaultPriority().toUpperCase() : "NORMAL");

        NotificationTemplate template = new NotificationTemplate(
                UUID.randomUUID().toString(), cmd.templateCode(), cmd.name(),
                type, priority, cmd.titleTemplate(), cmd.bodyTemplate());
        template.setImageUrl(cmd.imageUrl());
        template.setDeepLinkTemplate(cmd.deepLinkTemplate());
        template.setCategory(cmd.category());
        template.setLocalizedTitles(cmd.localizedTitles());
        template.setLocalizedBodies(cmd.localizedBodies());

        template = templateRepo.save(template);
        return TemplateDTO.from(template);
    }

    /**
     * 更新通知模板。
     */
    public TemplateDTO updateTemplate(String templateCode, TemplateCommand cmd) {
        log.info("NOT-004 updateTemplate: code={}", templateCode);

        Optional<NotificationTemplate> existing = templateRepo.findByTemplateCode(templateCode);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateCode);
        }

        NotificationTemplate template = existing.get();
        template.setName(cmd.name());
        template.setType(NotificationType.valueOf(cmd.type()));
        template.setDefaultPriority(NotificationPriority.valueOf(
                cmd.defaultPriority() != null ? cmd.defaultPriority().toUpperCase() : "NORMAL"));
        template.setTitleTemplate(cmd.titleTemplate());
        template.setBodyTemplate(cmd.bodyTemplate());
        template.setImageUrl(cmd.imageUrl());
        template.setDeepLinkTemplate(cmd.deepLinkTemplate());
        template.setCategory(cmd.category());
        template.setLocalizedTitles(cmd.localizedTitles());
        template.setLocalizedBodies(cmd.localizedBodies());
        template.bumpVersion();

        template = templateRepo.save(template);
        return TemplateDTO.from(template);
    }

    /**
     * 获取模板详情。
     */
    public TemplateDTO getTemplate(String templateCode) {
        return templateRepo.findByTemplateCode(templateCode)
                .map(TemplateDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateCode));
    }

    /**
     * 列出所有模板（分页）。
     */
    public TemplatePageDTO listTemplates(int page, int size) {
        List<NotificationTemplate> templates = templateRepo.findAll(page, size);
        long total = templateRepo.count();
        return new TemplatePageDTO(
                templates.stream().map(TemplateDTO::from).collect(Collectors.toList()),
                total);
    }

    /**
     * 按分类列出模板。
     */
    public List<TemplateDTO> listTemplatesByCategory(String category) {
        return templateRepo.findByCategory(category).stream()
                .map(TemplateDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 按类型列出模板。
     */
    public List<TemplateDTO> listTemplatesByType(String type) {
        NotificationType notifType = NotificationType.valueOf(type);
        return templateRepo.findByType(notifType).stream()
                .map(TemplateDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 列出所有活跃模板。
     */
    public List<TemplateDTO> listActiveTemplates() {
        return templateRepo.findAllActive().stream()
                .map(TemplateDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 激活/停用模板。
     */
    public TemplateDTO toggleTemplate(String templateCode, boolean active) {
        Optional<NotificationTemplate> existing = templateRepo.findByTemplateCode(templateCode);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateCode);
        }
        NotificationTemplate template = existing.get();
        if (active) {
            template.activate();
        } else {
            template.deactivate();
        }
        template = templateRepo.save(template);
        return TemplateDTO.from(template);
    }

    /**
     * 删除模板。
     */
    public void deleteTemplate(String templateCode) {
        Optional<NotificationTemplate> existing = templateRepo.findByTemplateCode(templateCode);
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Template not found: " + templateCode);
        }
        templateRepo.delete(existing.get().getTemplateId());
        log.info("NOT-004 deleted template: {}", templateCode);
    }

    /**
     * 渲染模板（预览）。
     */
    public TemplateRenderResultDTO renderTemplate(String templateCode, Map<String, String> variables, String locale) {
        NotificationTemplate template = templateRepo.findByTemplateCode(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateCode));

        TemplateRenderEngine.RenderResult result = templateRenderEngine.render(template, variables, locale);
        return new TemplateRenderResultDTO(result.title(), result.body(), result.deepLink(), result.imageUrl());
    }

    /**
     * 预览模板（使用默认变量）。
     */
    public TemplateRenderResultDTO previewTemplate(String templateCode) {
        NotificationTemplate template = templateRepo.findByTemplateCode(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateCode));

        TemplateRenderEngine.RenderResult result = templateRenderEngine.preview(template);
        return new TemplateRenderResultDTO(result.title(), result.body(), result.deepLink(), result.imageUrl());
    }

    /**
     * 提取模板变量。
     */
    public TemplateVariablesDTO extractTemplateVariables(String templateCode) {
        NotificationTemplate template = templateRepo.findByTemplateCode(templateCode)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateCode));

        return new TemplateVariablesDTO(templateCode, templateRenderEngine.extractVariables(template));
    }

    /**
     * 从模板发送通知。
     * 渲染模板后通过智能推送引擎发送。
     */
    public List<NotificationResultDTO> sendFromTemplate(SendFromTemplateCommand cmd) {
        log.info("NOT-004 sendFromTemplate: template={} targets={}", cmd.templateCode(), cmd.targetUserIds().size());

        NotificationTemplate template = templateRepo.findByTemplateCode(cmd.templateCode())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + cmd.templateCode()));

        TemplateRenderEngine.RenderResult rendered = templateRenderEngine.render(
                template, cmd.variables(), cmd.locale());

        SmartSendNotificationCommand sendCmd = new SmartSendNotificationCommand();
        sendCmd.setTargetUserIds(cmd.targetUserIds());
        sendCmd.setType(template.getType().name());
        sendCmd.setTitle(rendered.title());
        sendCmd.setBody(rendered.body());
        sendCmd.setImageUrl(rendered.imageUrl());
        sendCmd.setDeepLink(rendered.deepLink());
        sendCmd.setPriority(template.getDefaultPriority().name());
        sendCmd.setBypassSmartEngine(false);

        return smartSendNotification(sendCmd);
    }
}
