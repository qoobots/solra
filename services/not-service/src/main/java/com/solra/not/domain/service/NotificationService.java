package com.solra.not.domain.service;

import com.solra.not.domain.model.Notification;
import java.util.List;

/**
 * NotificationService — 通知领域服务接口。
 * NOT-001 推送通知系统的核心业务逻辑。
 */
public interface NotificationService {

    /**
     * 向指定用户发送通知。
     * 创建通知实体并推送到所有已注册设备。
     */
    List<Notification> send(List<String> toUserIds, Notification notification);

    /** 获取用户通知收件箱 */
    List<Notification> getInbox(String userId, int page, int size);

    /** 获取未读计数 */
    long getUnreadCount(String userId);
}
