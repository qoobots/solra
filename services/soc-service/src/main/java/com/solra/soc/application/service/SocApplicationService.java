package com.solra.soc.application.service;

import com.solra.soc.application.dto.*;
import com.solra.soc.domain.event.SocDomainEvents;
import com.solra.soc.domain.model.*;
import com.solra.soc.domain.repository.FriendRepository;
import com.solra.soc.domain.repository.ShareSessionRepository;
import com.solra.soc.domain.service.ShareEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * SocApplicationService — SOC 应用层服务。
 * 编排分享引擎和好友领域服务，SOC-004 + SOC-005。
 */
@Service
public class SocApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SocApplicationService.class);

    private final ShareEngine shareEngine;
    private final ShareSessionRepository shareSessionRepository;
    private final FriendRepository friendRepository;

    public SocApplicationService(ShareEngine shareEngine,
                                 ShareSessionRepository shareSessionRepository,
                                 FriendRepository friendRepository) {
        this.shareEngine = shareEngine;
        this.shareSessionRepository = shareSessionRepository;
        this.friendRepository = friendRepository;
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
