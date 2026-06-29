package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Flow;

/**
 * SpaceDomainService — 空间消费核心领域服务。
 * 编排：获取空间 → 流式加载 → 推荐 → 行为上报。
 */
public class SpaceDomainService {

    private static final Logger log = LoggerFactory.getLogger(SpaceDomainService.class);

    private final SpaceRepository spaceRepo;
    private final UserActionRepository actionRepo;
    private final StreamingLoader streamingLoader;
    private final RecommendationEngine recommendationEngine;
    private final SpaceSearchService searchService;
    private final PreloadManager preloadManager;
    private final TransitionService transitionService;
    private final CdnDistributionService cdnService;

    public SpaceDomainService(SpaceRepository spaceRepo, UserActionRepository actionRepo,
                               StreamingLoader streamingLoader, RecommendationEngine recommendationEngine,
                               SpaceSearchService searchService, PreloadManager preloadManager,
                               TransitionService transitionService, CdnDistributionService cdnService) {
        this.spaceRepo = spaceRepo;
        this.actionRepo = actionRepo;
        this.streamingLoader = streamingLoader;
        this.recommendationEngine = recommendationEngine;
        this.searchService = searchService;
        this.preloadManager = preloadManager;
        this.transitionService = transitionService;
        this.cdnService = cdnService;
    }

    /** SPC-001 + SPC-010: 获取空间并准备流式加载 */
    public Space getSpace(String spaceId) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));
        if (space.getStatus() != SpaceStatus.PUBLISHED) {
            throw new IllegalStateException("Space not published: " + spaceId);
        }
        space.incrementViews();
        spaceRepo.save(space);
        return space;
    }

    /** 获取初始加载资产块 */
    public List<AssetChunk> loadInitialChunks(String spaceId) {
        return streamingLoader.getInitialChunks(spaceId);
    }

    /** 流式加载资产 */
    public Flow.Publisher<AssetChunk> streamAssets(String spaceId, List<String> assetIds,
                                                     StreamingLoader.StreamConfig config) {
        return streamingLoader.streamAssets(spaceId, assetIds, config);
    }

    /** 获取预览卡片 */
    public Optional<PreviewCard> getPreviewCard(String spaceId) {
        return spaceRepo.findById(spaceId).map(this::toPreviewCard);
    }

    /** 批量获取预览卡片 */
    public List<PreviewCard> batchGetPreviewCards(List<String> spaceIds) {
        return spaceRepo.findByIds(spaceIds).stream().map(this::toPreviewCard).toList();
    }

    /** 推荐空间列表 */
    public List<Recommendation> listRecommendations(String userId, String mode, int offset, int limit,
                                                     List<SpaceCategory> categories) {
        return switch (mode.toLowerCase()) {
            case "popular" -> recommendationEngine.popular(limit, categories);
            case "newest" -> recommendationEngine.newest(limit, categories);
            case "trending" -> recommendationEngine.trending(limit, categories);
            default -> recommendationEngine.recommend(userId, limit, categories);
        };
    }

    /** 上报用户行为 */
    public void reportUserAction(UserAction action) {
        actionRepo.save(action);
        recommendationEngine.reportAction(action);

        // 增量更新空间统计
        try {
            switch (action.getActionType()) {
                case VIEW -> spaceRepo.incrementViewCount(action.getSpaceId());
                case LIKE -> spaceRepo.incrementLikeCount(action.getSpaceId());
                case SHARE -> spaceRepo.incrementShareCount(action.getSpaceId());
                case ENTER -> spaceRepo.findById(action.getSpaceId()).ifPresent(s -> {
                    s.incrementVisitors();
                    spaceRepo.save(s);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to update space stats: space={}", action.getSpaceId(), e);
        }
    }

    /** 获取分页空间列表 */
    public List<Space> listSpaces(int offset, int limit, List<SpaceCategory> categories, String sortBy) {
        return spaceRepo.findPublished(offset, limit, categories, sortBy);
    }

    private PreviewCard toPreviewCard(Space space) {
        PreviewCard card = new PreviewCard();
        card.setSpaceId(space.getSpaceId());
        card.setMeta(space.getMeta());
        card.setStats(space.getStats());
        card.setTags(space.getTags());

        // 从内容中提取预览图
        if (space.getMeta() != null && space.getMeta().getThumbnailUrl() != null) {
            card.setPreviewImages(List.of(space.getMeta().getThumbnailUrl()));
        }
        return card;
    }

    // ========== SPC-004: Space Search ==========

    /** 关键词搜索空间 */
    public SpaceSearchService.SearchResult searchSpaces(String keyword, List<SpaceCategory> categories,
                                                         String sortBy, int offset, int limit) {
        return searchService.search(keyword, categories, sortBy, offset, limit);
    }

    /** 分类浏览空间 */
    public List<Space> browseByCategory(List<SpaceCategory> categories, String sortBy,
                                         int offset, int limit) {
        return searchService.browseByCategory(categories, sortBy, offset, limit);
    }

    /** 获取搜索筛选面板 */
    public SpaceSearchService.SearchFacets getSearchFacets() {
        return searchService.getFacets();
    }

    // ========== SPC-005: Preload ==========

    /** 预测性预加载 */
    public PreloadManager.PreloadPrediction predictPreload(String userId,
                                                             String currentSpaceId, int count) {
        return preloadManager.predict(userId, currentSpaceId, count);
    }

    /** 获取缓存预加载预测 */
    public Optional<PreloadManager.PreloadPrediction> getCachedPreload(String userId) {
        return preloadManager.getCachedPrediction(userId);
    }

    /** 获取预加载统计 */
    public PreloadManager.PreloadStats getPreloadStats() {
        return preloadManager.getStats();
    }

    // ========== SPC-006: Loading Transition ==========

    /** 获取空间加载过渡配置 */
    public TransitionService.LoadingTransition getLoadingTransition(String spaceId) {
        return transitionService.getLoadingTransition(spaceId);
    }

    /** 获取过渡预设列表 */
    public List<TransitionService.TransitionPreset> getTransitionPresets() {
        return transitionService.getPresets();
    }

    // ========== SPC-007: Exit Flow ==========

    /** 获取空间退出过渡+下一预览卡片流 */
    public TransitionService.ExitFlow getExitFlow(String userId, String currentSpaceId,
                                                    List<String> nextCandidates) {
        return transitionService.getExitFlow(userId, currentSpaceId, nextCandidates);
    }

    // ========== SPC-011: CDN Distribution ==========

    /** 获取空间CDN分发清单 */
    public CdnDistributionService.CdnManifest getCdnManifest(String spaceId, String clientRegion) {
        return cdnService.getSpaceCdnManifest(spaceId, clientRegion);
    }

    /** 获取多区域CDN分发清单 */
    public CdnDistributionService.MultiRegionManifest getMultiRegionManifest(String spaceId) {
        return cdnService.getMultiRegionManifest(spaceId);
    }

    /** 获取CDN统计 */
    public CdnDistributionService.CdnStats getCdnStats() {
        return cdnService.getStats();
    }

    /** 清除CDN缓存 */
    public void purgeCdnCache(String spaceId) {
        cdnService.purgeCache(spaceId);
    }
}
