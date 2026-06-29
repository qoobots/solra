package com.solra.spc.application.service;

import com.solra.spc.application.dto.*;
import com.solra.spc.domain.model.*;
import com.solra.spc.domain.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

/**
 * SpcApplicationService — 空间消费应用层服务。
 * 编排领域服务、审计日志。
 */
@Service
public class SpcApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SpcApplicationService.class);

    private final SpaceDomainService domainService;
    private final StreamingLoader streamingLoader;

    public SpcApplicationService(SpaceDomainService domainService, StreamingLoader streamingLoader) {
        this.domainService = domainService;
        this.streamingLoader = streamingLoader;
    }

    /** SPC-001: 获取空间详情 */
    public SpcResultDTO.SpaceDTO getSpace(String spaceId) {
        Space space = domainService.getSpace(spaceId);
        return SpcResultDTO.SpaceDTO.from(space);
    }

    /** SPC-001: 加载空间初始资产 */
    public List<SpcResultDTO.AssetChunkDTO> loadSpaceInitial(String spaceId) {
        return domainService.loadInitialChunks(spaceId).stream()
                .map(SpcResultDTO.AssetChunkDTO::from).collect(Collectors.toList());
    }

    /** SPC-001: 流式加载空间资产 */
    public Flow.Publisher<AssetChunk> streamSpaceAssets(String spaceId, List<String> assetIds,
                                                          StreamingLoader.StreamConfig config) {
        return domainService.streamAssets(spaceId, assetIds,
                config != null ? config : StreamingLoader.StreamConfig.defaults());
    }

    /** SPC-003: 获取预览卡片 */
    public Optional<SpcResultDTO.PreviewCardDTO> getPreviewCard(String spaceId) {
        return domainService.getPreviewCard(spaceId).map(SpcResultDTO.PreviewCardDTO::from);
    }

    /** SPC-003: 批量获取预览卡片 */
    public List<SpcResultDTO.PreviewCardDTO> batchGetPreviewCards(List<String> spaceIds) {
        return domainService.batchGetPreviewCards(spaceIds).stream()
                .map(SpcResultDTO.PreviewCardDTO::from).collect(Collectors.toList());
    }

    /** SPC-002/SPC-009: 推荐空间列表 */
    public List<SpcResultDTO.RecommendationDTO> listRecommendations(SpcCommand.ListSpacesCommand cmd) {
        List<Recommendation> recs = domainService.listRecommendations(cmd.userId(), cmd.mode(),
                cmd.offset(), cmd.limit(), cmd.categories());
        return recs.stream().map(SpcResultDTO.RecommendationDTO::from).collect(Collectors.toList());
    }

    /** SPC-002: 上报用户行为 */
    public void reportUserAction(SpcCommand.ReportActionCommand cmd) {
        UserAction action = new UserAction();
        action.setActionId(UUID.randomUUID().toString());
        action.setUserId(cmd.userId());
        action.setSpaceId(cmd.spaceId());
        action.setActionType(UserActionType.valueOf(cmd.actionType().toUpperCase()));
        action.setDwellDurationMs(cmd.dwellDurationMs());
        action.setActionTime(java.time.Instant.now());
        domainService.reportUserAction(action);
        log.info("SPC action reported: user={} space={} type={}", cmd.userId(), cmd.spaceId(), cmd.actionType());
    }

    /** 获取流式加载器（用于 gRPC streaming） */
    public StreamingLoader getStreamingLoader() {
        return streamingLoader;
    }
}
