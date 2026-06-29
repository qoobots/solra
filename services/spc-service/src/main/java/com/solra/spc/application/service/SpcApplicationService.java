package com.solra.spc.application.service;

import com.solra.spc.application.dto.*;
import com.solra.spc.domain.model.*;
import com.solra.spc.domain.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    // ========== SPC-004: Space Search ==========

    /** 关键词搜索空间 */
    public SpaceSearchService.SearchResult searchSpaces(String keyword,
                                                         List<SpaceCategory> categories,
                                                         String sortBy, int offset, int limit) {
        return domainService.searchSpaces(keyword, categories, sortBy, offset, limit);
    }

    /** 分类浏览空间 */
    public List<SpcResultDTO.SpaceDTO> browseByCategory(List<SpaceCategory> categories,
                                                          String sortBy, int offset, int limit) {
        return domainService.browseByCategory(categories, sortBy, offset, limit)
                .stream().map(SpcResultDTO.SpaceDTO::from).collect(Collectors.toList());
    }

    /** 获取搜索筛选面板 */
    public SpaceSearchService.SearchFacets getSearchFacets() {
        return domainService.getSearchFacets();
    }

    // ========== SPC-005: Preload ==========

    /** 预测性预加载 */
    public PreloadManager.PreloadPrediction predictPreload(String userId,
                                                             String currentSpaceId, int count) {
        return domainService.predictPreload(userId, currentSpaceId, count);
    }

    /** 获取预加载统计 */
    public PreloadManager.PreloadStats getPreloadStats() {
        return domainService.getPreloadStats();
    }

    // ========== SPC-006: Loading Transition ==========

    /** 获取空间加载过渡配置 */
    public TransitionService.LoadingTransition getLoadingTransition(String spaceId) {
        return domainService.getLoadingTransition(spaceId);
    }

    /** 获取过渡预设列表 */
    public List<TransitionService.TransitionPreset> getTransitionPresets() {
        return domainService.getTransitionPresets();
    }

    // ========== SPC-007: Exit Flow ==========

    /** 获取空间退出过渡+下一预览卡片流 */
    public TransitionService.ExitFlow getExitFlow(String userId, String currentSpaceId,
                                                    List<String> nextCandidates) {
        return domainService.getExitFlow(userId, currentSpaceId, nextCandidates);
    }

    // ========== SPC-011: CDN Distribution ==========

    /** 获取空间CDN分发清单 */
    public CdnDistributionService.CdnManifest getCdnManifest(String spaceId,
                                                               String clientRegion) {
        return domainService.getCdnManifest(spaceId, clientRegion);
    }

    /** 获取多区域CDN分发清单 */
    public CdnDistributionService.MultiRegionManifest getMultiRegionManifest(String spaceId) {
        return domainService.getMultiRegionManifest(spaceId);
    }

    /** 获取CDN统计 */
    public CdnDistributionService.CdnStats getCdnStats() {
        return domainService.getCdnStats();
    }

    // ========== SPC-008: Leaderboard ==========

    /** 获取空间排行榜 */
    public List<SpcResultDTO.LeaderboardEntryDTO> getLeaderboard(String period, int topN) {
        LeaderboardPeriod p = parsePeriod(period);
        List<LeaderboardEntry> entries = domainService.getLeaderboard(p, topN);
        return entries.stream().map(SpcResultDTO.LeaderboardEntryDTO::from).collect(Collectors.toList());
    }

    /** 按分类获取空间排行榜 */
    public List<SpcResultDTO.LeaderboardEntryDTO> getLeaderboardByCategory(String period,
                                                                            List<SpaceCategory> categories, int topN) {
        LeaderboardPeriod p = parsePeriod(period);
        List<LeaderboardEntry> entries = domainService.getLeaderboardByCategory(p, categories, topN);
        return entries.stream().map(SpcResultDTO.LeaderboardEntryDTO::from).collect(Collectors.toList());
    }

    /** 强制刷新排行榜 */
    public void refreshLeaderboard() {
        domainService.refreshLeaderboard();
        log.info("Leaderboard refresh triggered");
    }

    /** 获取排行榜快照时间 */
    public Map<String, Instant> getLeaderboardSnapshotTimes() {
        Map<LeaderboardPeriod, Instant> times = domainService.getLeaderboardSnapshotTimes();
        Map<String, Instant> result = new LinkedHashMap<>();
        times.forEach((p, t) -> result.put(p.name(), t));
        return result;
    }

    private LeaderboardPeriod parsePeriod(String period) {
        if (period == null) return LeaderboardPeriod.DAILY;
        return switch (period.toUpperCase()) {
            case "WEEKLY" -> LeaderboardPeriod.WEEKLY;
            case "MONTHLY" -> LeaderboardPeriod.MONTHLY;
            default -> LeaderboardPeriod.DAILY;
        };
    }
}
