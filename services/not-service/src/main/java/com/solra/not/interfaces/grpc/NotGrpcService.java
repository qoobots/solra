package com.solra.not.interfaces.grpc;

import com.solra.not.application.dto.*;
import com.solra.not.application.service.NotApplicationService;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * NotGrpcService — 通知服务 gRPC 接口层。
 * NOT-001 推送通知系统。
 */
@GrpcService
public class NotGrpcService {

    private static final Logger log = LoggerFactory.getLogger(NotGrpcService.class);

    private final NotApplicationService appService;

    public NotGrpcService(NotApplicationService appService) {
        this.appService = appService;
    }

    /** 发送通知 */
    public List<NotificationResultDTO> sendNotification(SendNotificationCommand cmd) {
        return appService.sendNotification(cmd);
    }

    /** 获取收件箱 */
    public InboxPageDTO listNotifications(String userId, int page, int size) {
        return appService.getInbox(userId, page, size);
    }

    /** 标记已读 */
    public void markAsRead(String notificationId) {
        appService.markAsRead(notificationId);
    }

    /** 全部已读 */
    public void markAllAsRead(String userId) {
        appService.markAllAsRead(userId);
    }

    /** 删除通知 */
    public void deleteNotification(String notificationId) {
        appService.deleteNotification(notificationId);
    }

    /** 未读计数 */
    public long getUnreadCount(String userId) {
        return appService.getUnreadCount(userId);
    }

    /** 注册设备 */
    public DeviceRegistrationResultDTO registerDevice(RegisterDeviceCommand cmd) {
        return appService.registerDevice(cmd);
    }

    /** 取消注册设备 */
    public void unregisterDevice(String registrationId) {
        appService.unregisterDevice(registrationId);
    }

    /** 获取用户设备 */
    public List<DeviceRegistrationResultDTO> getUserDevices(String userId) {
        return appService.getUserDevices(userId);
    }

    /** 更新偏好 */
    public PreferenceResultDTO updatePreference(PreferenceUpdateCommand cmd) {
        return appService.updatePreference(cmd);
    }

    /** 获取偏好 */
    public List<PreferenceResultDTO> getPreferences(String userId) {
        return appService.getPreferences(userId);
    }

    // ── NOT-002: 应用内消息中心 ──

    /** 发送收件箱消息 */
    public InboxMessageResultDTO sendInboxMessage(SendInboxMessageCommand cmd) {
        return appService.sendInboxMessage(cmd);
    }

    /** 获取收件箱 */
    public InboxPageResultDTO getInbox(String userId, int page, int size) {
        return appService.getInbox(userId, page, size);
    }

    /** 获取未读消息列表 */
    public List<InboxMessageResultDTO> getUnreadMessages(String userId) {
        return appService.getUnreadMessages(userId);
    }

    /** 获取收件箱未读计数 */
    public long getInboxUnreadCount(String userId) {
        return appService.getInboxUnreadCount(userId);
    }

    /** 标记收件箱消息已读 */
    public void markInboxMessageRead(String messageId) {
        appService.markInboxMessageRead(messageId);
    }

    /** 全部标记已读 */
    public void markAllInboxRead(String userId) {
        appService.markAllInboxRead(userId);
    }

    /** 删除收件箱消息 */
    public void deleteInboxMessage(String messageId) {
        appService.deleteInboxMessage(messageId);
    }

    /** 获取会话消息 */
    public List<InboxMessageResultDTO> getConversationMessages(String conversationId, int page, int size) {
        return appService.getConversationMessages(conversationId, page, size);
    }

    // ── NOT-003: 智能推送策略 ──

    /** 智能发送通知 */
    public List<NotificationResultDTO> smartSendNotification(SmartSendNotificationCommand cmd) {
        return appService.smartSendNotification(cmd);
    }

    /** 评估推送决策 */
    public SmartPushDecisionDTO evaluatePush(String userId, String type, boolean isUrgent) {
        return appService.evaluatePush(userId, type, isUrgent);
    }

    /** 获取用户推送统计 */
    public PushStatsDTO getUserPushStats(String userId) {
        return appService.getUserPushStats(userId);
    }

    /** 检查静默时段 */
    public boolean isInQuietHours(String userId) {
        return appService.isInQuietHours(userId);
    }

    // ── NOT-004: 通知模板管理 ──

    /** 创建模板 */
    public TemplateDTO createTemplate(TemplateCommand cmd) {
        return appService.createTemplate(cmd);
    }

    /** 更新模板 */
    public TemplateDTO updateTemplate(String templateCode, TemplateCommand cmd) {
        return appService.updateTemplate(templateCode, cmd);
    }

    /** 获取模板 */
    public TemplateDTO getTemplate(String templateCode) {
        return appService.getTemplate(templateCode);
    }

    /** 列出模板 */
    public TemplatePageDTO listTemplates(int page, int size) {
        return appService.listTemplates(page, size);
    }

    /** 按分类列出 */
    public List<TemplateDTO> listTemplatesByCategory(String category) {
        return appService.listTemplatesByCategory(category);
    }

    /** 按类型列出 */
    public List<TemplateDTO> listTemplatesByType(String type) {
        return appService.listTemplatesByType(type);
    }

    /** 列出活跃模板 */
    public List<TemplateDTO> listActiveTemplates() {
        return appService.listActiveTemplates();
    }

    /** 切换模板启用状态 */
    public TemplateDTO toggleTemplate(String templateCode, boolean active) {
        return appService.toggleTemplate(templateCode, active);
    }

    /** 删除模板 */
    public void deleteTemplate(String templateCode) {
        appService.deleteTemplate(templateCode);
    }

    /** 渲染模板 */
    public TemplateRenderResultDTO renderTemplate(String templateCode,
                                                   Map<String, String> variables, String locale) {
        return appService.renderTemplate(templateCode, variables, locale);
    }

    /** 预览模板 */
    public TemplateRenderResultDTO previewTemplate(String templateCode) {
        return appService.previewTemplate(templateCode);
    }

    /** 提取模板变量 */
    public TemplateVariablesDTO extractTemplateVariables(String templateCode) {
        return appService.extractTemplateVariables(templateCode);
    }

    /** 从模板发送通知 */
    public List<NotificationResultDTO> sendFromTemplate(SendFromTemplateCommand cmd) {
        return appService.sendFromTemplate(cmd);
    }
}
