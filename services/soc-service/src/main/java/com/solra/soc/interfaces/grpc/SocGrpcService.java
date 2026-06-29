package com.solra.soc.interfaces.grpc;

import com.solra.soc.application.dto.*;
import com.solra.soc.application.service.SocApplicationService;
import com.solra.soc.domain.model.FriendStatus;
import com.solra.soc.domain.model.ShareType;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SocGrpcService — 社交服务 gRPC 接口层。
 * SOC-004 好友 + SOC-005 分享裂变。
 */
@GrpcService
public class SocGrpcService {

    private static final Logger log = LoggerFactory.getLogger(SocGrpcService.class);

    private final SocApplicationService appService;

    public SocGrpcService(SocApplicationService appService) {
        this.appService = appService;
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
