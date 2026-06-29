package com.solra.soc.interfaces.grpc;

import com.solra.soc.application.dto.*;
import com.solra.soc.application.service.SocApplicationService;
import com.solra.soc.domain.model.FriendStatus;
import com.solra.soc.domain.model.ShareType;
import com.solra.soc.domain.service.ChatService;
import com.solra.soc.domain.service.SessionManager;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * SocGrpcService — 社交服务 gRPC 接口层。
 * SOC-001 多人空间会话 + SOC-002 聊天 + SOC-004 好友 + SOC-005 分享裂变 + SOC-006 WebRTC 数据通道。
 */
@GrpcService
public class SocGrpcService {

    private static final Logger log = LoggerFactory.getLogger(SocGrpcService.class);

    private final SocApplicationService appService;

    public SocGrpcService(SocApplicationService appService) {
        this.appService = appService;
    }

    // ===== SOC-001 多人空间会话 =====

    /** 创建会话 */
    public SessionDTO createSession(String spaceId, String hostId, String type,
                                     int maxParticipants, boolean voiceChat, boolean gesture,
                                     boolean screenShare, boolean record) {
        SessionCommand cmd = new SessionCommand();
        cmd.setSpaceId(spaceId);
        cmd.setHostId(hostId);
        cmd.setType(type);
        cmd.setMaxParticipants(maxParticipants);
        cmd.setVoiceChatEnabled(voiceChat);
        cmd.setGestureEnabled(gesture);
        cmd.setScreenShareEnabled(screenShare);
        cmd.setRecordSession(record);
        return appService.createSession(cmd);
    }

    /** 加入会话 */
    public SessionDTO joinSession(String sessionId, String userId,
                                   String avatarId, String webrtcAnswerSdp) {
        JoinSessionCommand cmd = new JoinSessionCommand();
        cmd.setSessionId(sessionId);
        cmd.setUserId(userId);
        cmd.setAvatarId(avatarId);
        cmd.setWebrtcAnswerSdp(webrtcAnswerSdp);
        return appService.joinSession(cmd);
    }

    /** 离开会话 */
    public void leaveSession(String sessionId, String userId) {
        appService.leaveSession(sessionId, userId);
    }

    /** 获取会话详情 */
    public SessionDTO getSession(String sessionId) {
        return appService.getSession(sessionId);
    }

    /** 列出空间内活跃会话 */
    public List<SessionDTO> listActiveSessions(String spaceId) {
        return appService.listActiveSessions(spaceId);
    }

    /** 结束会话 */
    public void endSession(String sessionId) {
        appService.endSession(sessionId);
    }

    /** 获取在线参与者 */
    public OnlineParticipantsDTO getOnlineParticipants(String spaceId) {
        return appService.getOnlineParticipants(spaceId);
    }

    // ===== SOC-006 WebRTC 数据通道 =====

    /** 获取 WebRTC Offer SDP */
    public String getWebrtcOffer(String sessionId) {
        return appService.getWebrtcOffer(sessionId);
    }

    /** 更新参与者媒体状态 */
    public void updateParticipantMedia(String sessionId, String userId,
                                        boolean microphoneOn, boolean cameraOn) {
        appService.updateParticipantMedia(sessionId, userId, microphoneOn, cameraOn);
    }

    /** 获取会话统计 */
    public SessionManager.SessionStats getSessionStats() {
        return appService.getSessionStats();
    }

    // ===== SOC-002 空间内聊天 =====

    /** 发送文字消息 */
    public ChatMessageDTO sendTextMessage(String sessionId, String fromUserId,
                                           String toUserId, String content) {
        return appService.sendTextMessage(sessionId, fromUserId, toUserId, content);
    }

    /** 发送语音消息 */
    public ChatMessageDTO sendVoiceMessage(String sessionId, String fromUserId,
                                            String toUserId, String voiceUrl, int durationMs) {
        return appService.sendVoiceMessage(sessionId, fromUserId, toUserId, voiceUrl, durationMs);
    }

    /** 发送系统消息 */
    public ChatMessageDTO sendSystemMessage(String sessionId, String content) {
        return appService.sendSystemMessage(sessionId, content);
    }

    /** 获取最近消息 */
    public List<ChatMessageDTO> getRecentMessages(String sessionId, int limit) {
        return appService.getRecentMessages(sessionId, limit);
    }

    /** 获取增量消息 */
    public List<ChatMessageDTO> getMessagesSince(String sessionId, String sinceMessageId) {
        return appService.getMessagesSince(sessionId, sinceMessageId);
    }

    /** 获取聊天统计 */
    public ChatService.ChatStats getChatStats(String sessionId) {
        return appService.getChatStats(sessionId);
    }

    // ===== SOC-005 分享 =====

    /** 生成分享链接 */
    public ShareResultDTO createShare(String spaceId, String userId, String shareType) {
        GenerateShareCommand cmd = new GenerateShareCommand();
        cmd.setSpaceId(spaceId);
        cmd.setSharerUserId(userId);
        cmd.setShareType(shareType);
        return appService.generateShare(cmd);
    }

    /** 追踪分享点击 */
    public ClickResultDTO trackShareClick(String shareCode, String visitorUserId,
                                          String ip, String userAgent, String platform) {
        TrackClickCommand cmd = new TrackClickCommand();
        cmd.setShareCode(shareCode);
        cmd.setVisitorUserId(visitorUserId);
        cmd.setIpAddress(ip);
        cmd.setUserAgent(userAgent);
        cmd.setPlatform(platform);
        return appService.trackShareClick(cmd);
    }

    /** 追踪转化 */
    public void trackConversion(String shareCode, String userId) {
        appService.trackConversion(shareCode, userId);
    }

    /** 获取病毒传播统计 */
    public ViralStatsDTO getViralStats(String shareCode) {
        return appService.getViralStats(shareCode);
    }

    // ===== SOC-004 好友 =====

    /** 发送好友请求 */
    public FriendResultDTO sendFriendRequest(String userId, String friendUserId) {
        FriendRequestCommand cmd = new FriendRequestCommand(userId, friendUserId);
        return appService.sendFriendRequest(cmd);
    }

    /** 接受好友请求 */
    public FriendResultDTO acceptFriendRequest(String friendshipId) {
        return appService.acceptFriendRequest(friendshipId);
    }

    /** 拉黑好友 */
    public FriendResultDTO blockFriend(String friendshipId) {
        return appService.blockFriend(friendshipId);
    }

    /** 取消拉黑 */
    public FriendResultDTO unblockFriend(String friendshipId) {
        return appService.unblockFriend(friendshipId);
    }

    /** 获取好友列表 */
    public FriendListDTO getFriendList(String userId, int page, int size) {
        return appService.getFriendList(userId, page, size);
    }
}
