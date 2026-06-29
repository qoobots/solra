package com.solra.soc.application.service;

import com.solra.soc.application.dto.*;
import com.solra.soc.domain.event.SocDomainEvents;
import com.solra.soc.domain.model.*;
import com.solra.soc.domain.repository.FriendRepository;
import com.solra.soc.domain.repository.ShareSessionRepository;
import com.solra.soc.domain.service.ChatService;
import com.solra.soc.domain.service.SessionManager;
import com.solra.soc.domain.service.ShareEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SocApplicationService — SOC 应用层服务。
 * 编排 SOC-001 多人空间会话、SOC-002 聊天、SOC-004 好友、SOC-005 分享裂变、SOC-006 WebRTC 数据通道。
 */
@Service
public class SocApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SocApplicationService.class);

    private final SessionManager sessionManager;
    private final ChatService chatService;
    private final ShareEngine shareEngine;
    private final ShareSessionRepository shareSessionRepository;
    private final FriendRepository friendRepository;

    public SocApplicationService(SessionManager sessionManager,
                                 ChatService chatService,
                                 ShareEngine shareEngine,
                                 ShareSessionRepository shareSessionRepository,
                                 FriendRepository friendRepository) {
        this.sessionManager = sessionManager;
        this.chatService = chatService;
        this.shareEngine = shareEngine;
        this.shareSessionRepository = shareSessionRepository;
        this.friendRepository = friendRepository;
    }

    // ===== SOC-001 多人空间会话 =====

    /** 创建多人空间会话 */
    public SessionDTO createSession(SessionCommand cmd) {
        log.info("SOC-001 createSession: space={} host={} type={} maxParticipants={}",
                cmd.getSpaceId(), cmd.getHostId(), cmd.getType(), cmd.getMaxParticipants());

        Session.SessionType sessionType = Session.SessionType.valueOf(cmd.getType());
        SessionConfig config = new SessionConfig();
        config.setVoiceChatEnabled(cmd.isVoiceChatEnabled());
        config.setGestureEnabled(cmd.isGestureEnabled());
        config.setScreenShareEnabled(cmd.isScreenShareEnabled());
        config.setRecordSession(cmd.isRecordSession());

        Session session = sessionManager.createSession(
                cmd.getSpaceId(), cmd.getHostId(), sessionType,
                cmd.getMaxParticipants(), config);

        log.info("SOC-001 session created: id={} sdp=true", session.getSessionId());
        return SessionDTO.from(session);
    }

    /** 加入会话 */
    public SessionDTO joinSession(JoinSessionCommand cmd) {
        log.info("SOC-001 joinSession: session={} user={}", cmd.getSessionId(), cmd.getUserId());
        Session session = sessionManager.joinSession(
                cmd.getSessionId(), cmd.getUserId(), cmd.getAvatarId(), cmd.getWebrtcAnswerSdp());
        return SessionDTO.from(session);
    }

    /** 离开会话 */
    public void leaveSession(String sessionId, String userId) {
        log.info("SOC-001 leaveSession: session={} user={}", sessionId, userId);
        sessionManager.leaveSession(sessionId, userId);
    }

    /** 获取会话详情 */
    public SessionDTO getSession(String sessionId) {
        Session session = sessionManager.getSession(sessionId);
        return SessionDTO.from(session);
    }

    /** 列出空间内活跃会话 */
    public List<SessionDTO> listActiveSessions(String spaceId) {
        return sessionManager.listActiveSessions(spaceId).stream()
                .map(SessionDTO::from)
                .toList();
    }

    /** 结束会话 */
    public void endSession(String sessionId) {
        log.info("SOC-001 endSession: {}", sessionId);
        sessionManager.endSession(sessionId);
        chatService.cleanup(sessionId);
    }

    /** 获取在线参与者 */
    public OnlineParticipantsDTO getOnlineParticipants(String spaceId) {
        List<Participant> participants = sessionManager.getOnlineParticipants(spaceId);
        List<SessionDTO.ParticipantDTO> dtos = participants.stream()
                .map(SessionDTO.ParticipantDTO::from)
                .toList();
        return new OnlineParticipantsDTO(dtos, participants.size());
    }

    // ===== SOC-006 WebRTC 数据通道 =====

    /** 获取 WebRTC Offer SDP */
    public String getWebrtcOffer(String sessionId) {
        log.info("SOC-006 getWebrtcOffer: session={}", sessionId);
        return sessionManager.getWebrtcOffer(sessionId);
    }

    /** 更新参与者媒体状态 */
    public void updateParticipantMedia(String sessionId, String userId,
                                        boolean microphoneOn, boolean cameraOn) {
        sessionManager.updateParticipantMedia(sessionId, userId, microphoneOn, cameraOn);
    }

    /** 获取会话统计 */
    public SessionManager.SessionStats getSessionStats() {
        return sessionManager.getStats();
    }

    // ===== SOC-002 空间内聊天 =====

    /** 发送文字消息 */
    public ChatMessageDTO sendTextMessage(String sessionId, String fromUserId,
                                           String toUserId, String content) {
        log.info("SOC-002 sendTextMessage: session={} from={}", sessionId, fromUserId);
        ChatMessage msg = chatService.sendTextMessage(sessionId, fromUserId, toUserId, content);
        return ChatMessageDTO.from(msg);
    }

    /** 发送语音消息 */
    public ChatMessageDTO sendVoiceMessage(String sessionId, String fromUserId,
                                            String toUserId, String voiceUrl, int durationMs) {
        log.info("SOC-002 sendVoiceMessage: session={} from={}", sessionId, fromUserId);
        ChatMessage msg = chatService.sendVoiceMessage(sessionId, fromUserId, toUserId, voiceUrl, durationMs);
        return ChatMessageDTO.from(msg);
    }

    /** 发送系统消息 */
    public ChatMessageDTO sendSystemMessage(String sessionId, String content) {
        log.info("SOC-002 sendSystemMessage: session={}", sessionId);
        ChatMessage msg = chatService.sendSystemMessage(sessionId, content);
        return ChatMessageDTO.from(msg);
    }

    /** 获取最近消息 */
    public List<ChatMessageDTO> getRecentMessages(String sessionId, int limit) {
        return chatService.getRecentMessages(sessionId, limit).stream()
                .map(ChatMessageDTO::from)
                .toList();
    }

    /** 获取指定消息之后的增量消息 */
    public List<ChatMessageDTO> getMessagesSince(String sessionId, String sinceMessageId) {
        return chatService.getMessagesSince(sessionId, sinceMessageId).stream()
                .map(ChatMessageDTO::from)
                .toList();
    }

    /** 获取聊天统计 */
    public ChatService.ChatStats getChatStats(String sessionId) {
        return chatService.getStats(sessionId);
    }

    // ===== SOC-005 空间分享裂变 =====

    /** 生成分享链接 */
    public ShareResultDTO generateShare(GenerateShareCommand cmd) {
        log.info("SOC-005 generateShare: space={} user={} type={}", cmd.getSpaceId(), cmd.getSharerUserId(), cmd.getShareType());
        ShareSession session = shareEngine.generateShareLink(cmd.getSpaceId(), cmd.getSharerUserId(), cmd.getShareType());
        log.info("SOC-005 share generated: shareId={} code={}", session.getShareId(), session.getShareCode());
        return ShareResultDTO.from(session);
    }

    /** 追踪分享点击 */
    public ClickResultDTO trackShareClick(TrackClickCommand cmd) {
        log.info("SOC-005 trackClick: code={} visitor={}", cmd.getShareCode(), cmd.getVisitorUserId());

        ShareEngine.VisitorInfo visitorInfo = ShareEngine.VisitorInfo.of(
                cmd.getVisitorUserId(), cmd.getIpAddress(), cmd.getUserAgent(), cmd.getPlatform());
        Optional<ShareClick> click = shareEngine.trackClick(cmd.getShareCode(), visitorInfo);

        return click.map(c -> ClickResultDTO.from(c,
                "https://solra.space/redirect?code=" + cmd.getShareCode()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired share code: " + cmd.getShareCode()));
    }

    /** 追踪转化 */
    public void trackConversion(String shareCode, String userId) {
        log.info("SOC-005 trackConversion: code={} user={}", shareCode, userId);
        boolean ok = shareEngine.trackConversion(shareCode, userId);
        if (!ok) {
            throw new IllegalArgumentException("Invalid share code: " + shareCode);
        }
    }

    /** 获取病毒传播统计 */
    public ViralStatsDTO getViralStats(String shareCode) {
        return shareEngine.getViralChainStats(shareCode)
                .map(ViralStatsDTO::from)
                .orElseThrow(() -> new IllegalArgumentException("Share not found: " + shareCode));
    }

    // ===== SOC-004 好友系统 =====

    /** 发送好友请求 */
    public FriendResultDTO sendFriendRequest(FriendRequestCommand cmd) {
        log.info("SOC-004 sendFriendRequest: {} → {}", cmd.getUserId(), cmd.getFriendUserId());

        String friendshipId = UUID.randomUUID().toString();
        Friend friend = new Friend(friendshipId, cmd.getUserId(), cmd.getFriendUserId());
        Friend saved = friendRepository.save(friend);

        log.info("SOC-004 friend request created: {}", friendshipId);
        return FriendResultDTO.from(saved);
    }

    /** 接受好友请求 */
    public FriendResultDTO acceptFriendRequest(String friendshipId) {
        log.info("SOC-004 acceptFriendRequest: {}", friendshipId);

        Friend friend = friendRepository.findByFriendshipId(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found: " + friendshipId));
        friend.accept();
        Friend saved = friendRepository.save(friend);

        log.info("SOC-004 friend accepted: {}", friendshipId);
        return FriendResultDTO.from(saved);
    }

    /** 拉黑好友 */
    public FriendResultDTO blockFriend(String friendshipId) {
        log.info("SOC-004 blockFriend: {}", friendshipId);

        Friend friend = friendRepository.findByFriendshipId(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found: " + friendshipId));
        friend.block();
        Friend saved = friendRepository.save(friend);

        log.info("SOC-004 friend blocked: {}", friendshipId);
        return FriendResultDTO.from(saved);
    }

    /** 取消拉黑 */
    public FriendResultDTO unblockFriend(String friendshipId) {
        log.info("SOC-004 unblockFriend: {}", friendshipId);

        Friend friend = friendRepository.findByFriendshipId(friendshipId)
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found: " + friendshipId));
        friend.unblock();
        Friend saved = friendRepository.save(friend);

        log.info("SOC-004 friend unblocked: {}", friendshipId);
        return FriendResultDTO.from(saved);
    }

    /** 获取好友列表 */
    public FriendListDTO getFriendList(String userId, int page, int size) {
        var friends = friendRepository.findByUserId(userId, page, size);
        var items = friends.stream().map(FriendResultDTO::from).toList();
        long total = friendRepository.countByUserId(userId);
        return new FriendListDTO(items, total, page, size);
    }
}
