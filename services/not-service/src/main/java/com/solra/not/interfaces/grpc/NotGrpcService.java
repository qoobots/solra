package com.solra.not.interfaces.grpc;

import com.solra.not.application.dto.*;
import com.solra.not.application.service.NotApplicationService;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
}
