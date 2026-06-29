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

    public SpaceDomainService(SpaceRepository spaceRepo, UserActionRepository actionRepo,
                               StreamingLoader streamingLoader, RecommendationEngine recommendationEngine) {
        this.spaceRepo = spaceRepo;
        this.actionRepo = actionRepo;
        this.streamingLoader = streamingLoader;
        this.recommendationEngine = recommendationEngine;
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
}
