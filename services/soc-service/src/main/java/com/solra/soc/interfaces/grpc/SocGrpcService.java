package com.solra.soc.interfaces.grpc;

import com.solra.soc.application.dto.*;
import com.solra.soc.application.service.SocApplicationService;
import com.solra.soc.domain.model.FriendStatus;
import com.solra.soc.domain.model.ShareType;
import com.solra.soc.domain.service.ChatService;
import com.solra.soc.domain.service.SessionManager;
import com.solra.soc.domain.service.SpatialAudioEngine;
import com.solra.soc.domain.service.SocialGestureService;
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

    // ===== SOC-003 空间社交信号系统 =====

    /** 发送社交信号 */
    public GestureDTO sendGesture(String sessionId, String fromUserId, String signal,
                                   String intensity, String targetUserId, String message, int durationMs) {
        SendGestureCommand cmd = new SendGestureCommand();
        cmd.setSessionId(sessionId);
        cmd.setFromUserId(fromUserId);
        cmd.setSignal(signal);
        cmd.setIntensity(intensity);
        cmd.setTargetUserId(targetUserId);
        cmd.setMessage(message);
        cmd.setDurationMs(durationMs);
        return appService.sendGesture(cmd);
    }

    /** 举手 */
    public GestureDTO raiseHand(String sessionId, String userId) {
        return appService.raiseHand(sessionId, userId);
    }

    /** 鼓掌 */
    public GestureDTO applaud(String sessionId, String userId, String intensity) {
        return appService.applaud(sessionId, userId, intensity);
    }

    /** 请求安静 */
    public GestureDTO requestSilence(String sessionId, String userId) {
        return appService.requestSilence(sessionId, userId);
    }

    /** 点赞 */
    public GestureDTO thumbsUp(String sessionId, String userId, String targetUserId) {
        return appService.thumbsUp(sessionId, userId, targetUserId);
    }

    /** 确认社交信号 */
    public void acknowledgeGesture(String sessionId, String gestureId) {
        appService.acknowledgeGesture(sessionId, gestureId);
    }

    /** 获取最近社交信号 */
    public List<GestureDTO> getRecentGestures(String sessionId, int limit) {
        return appService.getRecentGestures(sessionId, limit);
    }

    /** 获取增量社交信号 */
    public List<GestureDTO> getGesturesSince(String sessionId, String sinceGestureId) {
        return appService.getGesturesSince(sessionId, sinceGestureId);
    }

    /** 获取活跃社交信号 */
    public List<GestureDTO> getActiveGestures(String sessionId) {
        return appService.getActiveGestures(sessionId);
    }

    /** 获取社交信号统计 */
    public GestureStatsDTO getGestureStats(String sessionId) {
        return appService.getGestureStats(sessionId);
    }

    // ===== SOC-007 空间音频引擎 =====

    /** 注册声源 */
    public AudioSourceDTO registerAudioSource(String sessionId, String ownerUserId,
                                               String type, float x, float y, float z,
                                               float volume, float minDistance, float maxDistance,
                                               float rolloffFactor, boolean spatialized, boolean loop) {
        AudioSourceCommand cmd = new AudioSourceCommand();
        cmd.setSessionId(sessionId);
        cmd.setOwnerUserId(ownerUserId);
        cmd.setType(type);
        cmd.setPositionX(x);
        cmd.setPositionY(y);
        cmd.setPositionZ(z);
        cmd.setVolume(volume);
        cmd.setMinDistance(minDistance);
        cmd.setMaxDistance(maxDistance);
        cmd.setRolloffFactor(rolloffFactor);
        cmd.setSpatialized(spatialized);
        cmd.setLoop(loop);
        return appService.registerAudioSource(cmd);
    }

    /** 注册麦克风声源 */
    public AudioSourceDTO registerMicrophone(String sessionId, String userId,
                                              float x, float y, float z) {
        return appService.registerMicrophone(sessionId, userId, x, y, z);
    }

    /** 注册背景音乐 */
    public AudioSourceDTO registerBackgroundMusic(String sessionId, String musicId, float volume) {
        return appService.registerBackgroundMusic(sessionId, musicId, volume);
    }

    /** 注册环境音 */
    public AudioSourceDTO registerAmbient(String sessionId, String ambientId,
                                           float x, float y, float z,
                                           float volume, float maxDistance) {
        return appService.registerAmbient(sessionId, ambientId, x, y, z, volume, maxDistance);
    }

    /** 移除声源 */
    public void removeAudioSource(String sessionId, String sourceId) {
        appService.removeAudioSource(sessionId, sourceId);
    }

    /** 更新声源位置 */
    public void updateSourcePosition(String sessionId, String sourceId,
                                      float x, float y, float z) {
        appService.updateSourcePosition(sessionId, sourceId, x, y, z);
    }

    /** 注册听者 */
    public void registerListener(String sessionId, String userId,
                                  float x, float y, float z,
                                  float fx, float fy, float fz) {
        appService.registerListener(sessionId, userId, x, y, z, fx, fy, fz);
    }

    /** 计算空间音频混合 */
    public SpatialAudioMixDTO calculateAudioMix(String sessionId, String listenerUserId) {
        return appService.calculateAudioMix(sessionId, listenerUserId);
    }

    /** 获取会话声源列表 */
    public List<AudioSourceDTO> getSessionAudioSources(String sessionId) {
        return appService.getSessionAudioSources(sessionId);
    }

    /** 获取空间音频引擎统计 */
    public SpatialAudioEngine.AudioEngineStats getAudioEngineStats() {
        return appService.getAudioEngineStats();
    }
}
