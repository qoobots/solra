package com.solra.soc.domain.event;

import java.time.Instant;

/**
 * SOC 领域事件定义。
 * <p>
 * 使用 Java record 定义不可变的领域事件，用于事件驱动通信。
 * 事件由领域服务产生，应用层可选择性地发布到事件总线。
 */
public final class SocDomainEvents {

    private SocDomainEvents() {}

    /** 分享链接已创建事件。 */
    public record ShareLinkCreated(String shareId, String spaceId, String sharerUserId, Instant at) {}

    /** 分享链接被点击事件。 */
    public record ShareLinkClicked(String shareId, String visitorUserId, Instant at) {}

    /** 分享链接产生转化事件（用户注册/加入空间等）。 */
    public record ShareConverted(String shareId, String newUserId, Instant at) {}

    /** 好友请求已发送事件。 */
    public record FriendRequestSent(String friendshipId, String userId, String friendUserId, Instant at) {}

    /** 好友请求已被接受事件。 */
    public record FriendAccepted(String friendshipId, String userId, String friendUserId, Instant at) {}

    /** SOC-003: 社交信号已发送事件。 */
    public record SocialGestureSent(String gestureId, String sessionId, String fromUserId,
                                     String signal, String targetUserId, Instant at) {}

    /** SOC-003: 社交信号已被确认事件。 */
    public record SocialGestureAcknowledged(String gestureId, String sessionId,
                                             String fromUserId, Instant at) {}

    /** SOC-007: 声源已注册事件。 */
    public record AudioSourceRegistered(String sourceId, String sessionId,
                                         String type, Instant at) {}

    /** SOC-007: 声源已移除事件。 */
    public record AudioSourceRemoved(String sourceId, String sessionId, Instant at) {}

    /** SOC-004: 好友分组已创建事件。 */
    public record FriendGroupCreated(String groupId, String userId, String groupName, Instant at) {}

    /** SOC-004: 好友已添加到分组事件。 */
    public record FriendAddedToGroup(String groupId, String friendUserId, Instant at) {}

    /** SOC-004: 好友上线事件。 */
    public record FriendOnline(String userId, String spaceId, String spaceName, Instant at) {}

    /** SOC-004: 好友下线事件。 */
    public record FriendOffline(String userId, Instant at) {}

    /** SOC-004: 好友进入同空间事件。 */
    public record FriendEnteredSameSpace(String userId, String friendUserId, String spaceId, Instant at) {}

    /** SOC-008: 虚拟人主持人已创建事件。 */
    public record HostAvatarCreated(String hostId, String sessionId, String avatarId, String mode, Instant at) {}

    /** SOC-008: 主持人话题已变更事件。 */
    public record HostTopicChanged(String hostId, String sessionId, String topic, Instant at) {}

    /** SOC-008: 主持人授予发言权事件。 */
    public record HostGrantedSpeakingTurn(String hostId, String sessionId, String speakerUserId, Instant at) {}

    /** SOC-008: 冷场检测事件。 */
    public record SilenceDetected(String sessionId, String suggestedAction, long silenceSeconds, Instant at) {}
}
