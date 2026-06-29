package com.solra.spc.interfaces.grpc;

import com.solra.avt.application.dto.*;
import com.solra.spc.application.service.SpcApplicationService;
import com.solra.spc.domain.model.*;
import com.solra.spc.domain.service.StreamingLoader;
import com.solra.apis.common.v1.Common;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.UUID;

/**
 * SpcGrpcService — SPC gRPC 接口适配层。
 * 注意：proto 生成的 Java 类型与 domain.model 命名冲突，
 * 使用完全限定名 com.solra.apis.spc.v1.* 引用 proto 类型。
 */
@GrpcService
public class SpcGrpcService extends com.solra.apis.spc.v1.SpcServiceGrpc.SpcServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(SpcGrpcService.class);
    private final SpcApplicationService appService;

    public SpcGrpcService(SpcApplicationService appService) {
        this.appService = appService;
    }

    // ==================== RPC 实现 ====================

    @Override
    public void getSpace(com.solra.apis.spc.v1.GetSpaceRequest request,
                          StreamObserver<com.solra.apis.spc.v1.GetSpaceResponse> responseObserver) {
        try {
            SpcResultDTO.SpaceDTO dto = appService.getSpace(request.getSpaceId().getValue());
            var resp = com.solra.apis.spc.v1.GetSpaceResponse.newBuilder()
                    .setSpace(buildProtoSpace(dto)).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetSpace failed", e);
            responseObserver.onNext(com.solra.apis.spc.v1.GetSpaceResponse.newBuilder()
                    .setError(Common.SolraError.newBuilder().setMessage(e.getMessage()).build()).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listRecommendSpaces(com.solra.apis.spc.v1.ListRecommendSpacesRequest request,
                                     StreamObserver<com.solra.apis.spc.v1.ListRecommendSpacesResponse> responseObserver) {
        try {
            SpcCommand.ListSpacesCommand cmd = new SpcCommand.ListSpacesCommand(
                    request.getUserId().getValue(),
                    request.getMode().name().replace("LIST_RECOMMEND_MODE_", "").toLowerCase(),
                    request.hasPage() ? (int) request.getPage().getPage() * (int) request.getPage().getSize() : 0,
                    request.hasPage() ? (int) request.getPage().getSize() : 20,
                    request.getCategoriesList().stream()
                            .map(c -> SpaceCategory.valueOf(c.name().replace("SPACE_CATEGORY_", ""))).toList()
            );

            List<SpcResultDTO.RecommendationDTO> recs = appService.listRecommendations(cmd);
            // 将推荐条目映射为 proto PreviewCard（推荐列表通过预览卡片返回）
            List<com.solra.apis.spc.v1.PreviewCard> protoCards = recs.stream().map(r ->
                com.solra.apis.spc.v1.PreviewCard.newBuilder()
                    .setSpaceId(Common.SpaceId.newBuilder().setValue(r.spaceId()).build())
                    .setMeta(buildProtoMeta(r.spaceId()))
                    .setStats(com.solra.apis.spc.v1.SpaceStats.newBuilder()
                            .setViewCount(0).setLikeCount(0).setShareCount(0)
                            .setVisitorCount(0).setConversationCount(0).setRating(
                                    (r.relevance() + r.popularity() + r.freshness()) / 3).build())
                    .addAllTags(r.reasons())
                    .build()
            ).toList();

            var resp = com.solra.apis.spc.v1.ListRecommendSpacesResponse.newBuilder()
                    .setCards(com.solra.apis.spc.v1.PreviewCardList.newBuilder()
                            .setListId("rec-" + UUID.randomUUID())
                            .setListName(request.getMode().name().replace("LIST_RECOMMEND_MODE_", ""))
                            .addAllCards(protoCards)
                            .setPage(Common.PageResponse.newBuilder()
                                    .setPage(request.hasPage() ? request.getPage().getPage() : 0)
                                    .setSize(request.hasPage() ? request.getPage().getSize() : protoCards.size())
                                    .setTotalItems(protoCards.size()).build())
                            .build())
                    .build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("ListRecommendSpaces failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void loadSpace(com.solra.apis.spc.v1.LoadSpaceRequest request,
                           StreamObserver<com.solra.apis.spc.v1.LoadSpaceResponse> responseObserver) {
        try {
            List<SpcResultDTO.AssetChunkDTO> chunks = appService.loadSpaceInitial(
                    request.getSpaceId().getValue());
            List<com.solra.apis.spc.v1.AssetChunk> protoChunks = chunks.stream().map(c ->
                com.solra.apis.spc.v1.AssetChunk.newBuilder()
                    .setAssetId(Common.AssetId.newBuilder().setValue(c.assetId()).build())
                    .setChunkIndex(c.chunkIndex()).setTotalChunks(c.totalChunks())
                    .setIsFinal(c.isFinal()).setCompressionLevel(c.compressionLevel())
                    .build()
            ).toList();

            var resp = com.solra.apis.spc.v1.LoadSpaceResponse.newBuilder()
                    .setSpace(com.solra.apis.spc.v1.Space.newBuilder()
                            .setSpaceId(Common.SpaceId.newBuilder().setValue(request.getSpaceId().getValue()).build())
                            .setStatus(com.solra.apis.spc.v1.SpaceStatus.SPACE_STATUS_PUBLISHED).build())
                    .addAllInitialChunks(protoChunks).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("LoadSpace failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void streamSpaceAssets(com.solra.apis.spc.v1.StreamSpaceAssetsRequest request,
                                   StreamObserver<com.solra.apis.spc.v1.AssetChunk> responseObserver) {
        try {
            String spaceId = request.getSpaceId().getValue();
            List<String> assetIds = request.getAssetIdsList().stream()
                    .map(Common.AssetId::getValue).toList();

            var cfg = request.getConfig();
            StreamingLoader.StreamConfig config = new StreamingLoader.StreamConfig(
                    cfg.getMaxConcurrentChunks() > 0 ? cfg.getMaxConcurrentChunks() : 4,
                    cfg.getChunkSizeBytes() > 0 ? cfg.getChunkSizeBytes() : 65536,
                    cfg.getPriorityAssetsList(), cfg.getEnableCompression(), cfg.getLodThreshold());

            Flow.Publisher<AssetChunk> publisher = appService.streamSpaceAssets(spaceId, assetIds, config);
            publisher.subscribe(new FlowSubscriberAdapter<>(
                    chunk -> responseObserver.onNext(
                            com.solra.apis.spc.v1.AssetChunk.newBuilder()
                                    .setAssetId(Common.AssetId.newBuilder().setValue(chunk.getAssetId()).build())
                                    .setChunkIndex(chunk.getChunkIndex()).setTotalChunks(chunk.getTotalChunks())
                                    .setData(com.google.protobuf.ByteString.copyFrom(chunk.getData()))
                                    .setIsFinal(chunk.isFinal()).setCompressionLevel(chunk.getCompressionLevel())
                                    .build()),
                    responseObserver::onCompleted,
                    e -> { log.error("StreamSpaceAssets failed", e); responseObserver.onError(e); }));
        } catch (Exception e) {
            log.error("StreamSpaceAssets setup failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getPreviewCard(com.solra.apis.spc.v1.GetPreviewCardRequest request,
                                StreamObserver<com.solra.apis.spc.v1.GetPreviewCardResponse> responseObserver) {
        try {
            var cardOpt = appService.getPreviewCard(request.getSpaceId().getValue());
            if (cardOpt.isPresent()) {
                responseObserver.onNext(com.solra.apis.spc.v1.GetPreviewCardResponse.newBuilder()
                        .setCard(buildProtoPreviewCard(cardOpt.get())).build());
            } else {
                responseObserver.onNext(com.solra.apis.spc.v1.GetPreviewCardResponse.newBuilder()
                        .setError(Common.SolraError.newBuilder().setMessage("Space not found").build()).build());
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetPreviewCard failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void batchGetPreviewCards(com.solra.apis.spc.v1.BatchGetPreviewCardsRequest request,
                                      StreamObserver<com.solra.apis.spc.v1.BatchGetPreviewCardsResponse> responseObserver) {
        try {
            List<String> ids = request.getSpaceIdsList().stream().map(Common.SpaceId::getValue).toList();
            var cards = appService.batchGetPreviewCards(ids);
            responseObserver.onNext(com.solra.apis.spc.v1.BatchGetPreviewCardsResponse.newBuilder()
                    .addAllCards(cards.stream().map(this::buildProtoPreviewCard).toList()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("BatchGetPreviewCards failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void reportUserAction(com.solra.apis.spc.v1.ReportUserActionRequest request,
                                  StreamObserver<com.solra.apis.spc.v1.ReportUserActionResponse> responseObserver) {
        try {
            SpcCommand.ReportActionCommand cmd = new SpcCommand.ReportActionCommand(
                    request.getUserId().getValue(), request.getSpaceId().getValue(),
                    request.getActionType().name().replace("USER_ACTION_TYPE_", ""), request.getDwellDurationMs());
            appService.reportUserAction(cmd);
            responseObserver.onNext(com.solra.apis.spc.v1.ReportUserActionResponse.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("ReportUserAction failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== SPC-004: Space Search ==========

    @Override
    public void searchSpaces(com.solra.apis.spc.v1.SearchSpacesRequest request,
                              StreamObserver<com.solra.apis.spc.v1.SearchSpacesResponse> responseObserver) {
        try {
            var cats = request.getCategoriesList().stream()
                    .map(c -> SpaceCategory.valueOf(c.name().replace("SPACE_CATEGORY_", ""))).toList();
            int off = request.hasPage() ? (int) request.getPage().getPage() * (int) request.getPage().getSize() : 0;
            int lim = request.hasPage() ? (int) request.getPage().getSize() : 20;

            var result = appService.searchSpaces(request.getKeyword(), cats,
                    request.getSortBy(), off, lim);

            List<com.solra.apis.spc.v1.Space> protoSpaces = result.spaces().stream()
                    .map(s -> buildProtoSpace(SpcResultDTO.SpaceDTO.from(s))).toList();

            responseObserver.onNext(com.solra.apis.spc.v1.SearchSpacesResponse.newBuilder()
                    .addAllSpaces(protoSpaces)
                    .setTotalResults(result.total())
                    .setPage(Common.PageResponse.newBuilder()
                            .setPage(request.hasPage() ? request.getPage().getPage() : 0)
                            .setSize(lim).setTotalItems(result.total()).build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("SearchSpaces failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void browseByCategory(com.solra.apis.spc.v1.BrowseByCategoryRequest request,
                                  StreamObserver<com.solra.apis.spc.v1.BrowseByCategoryResponse> responseObserver) {
        try {
            var cats = request.getCategoriesList().stream()
                    .map(c -> SpaceCategory.valueOf(c.name().replace("SPACE_CATEGORY_", ""))).toList();
            int off = request.hasPage() ? (int) request.getPage().getPage() * (int) request.getPage().getSize() : 0;
            int lim = request.hasPage() ? (int) request.getPage().getSize() : 20;

            var spaces = appService.browseByCategory(cats, request.getSortBy(), off, lim);
            List<com.solra.apis.spc.v1.Space> protoSpaces = spaces.stream()
                    .map(this::buildProtoSpace).toList();

            responseObserver.onNext(com.solra.apis.spc.v1.BrowseByCategoryResponse.newBuilder()
                    .addAllSpaces(protoSpaces)
                    .setPage(Common.PageResponse.newBuilder()
                            .setPage(request.hasPage() ? request.getPage().getPage() : 0)
                            .setSize(lim).setTotalItems(protoSpaces.size()).build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("BrowseByCategory failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSearchFacets(com.solra.apis.spc.v1.GetSearchFacetsRequest request,
                                 StreamObserver<com.solra.apis.spc.v1.GetSearchFacetsResponse> responseObserver) {
        try {
            var facets = appService.getSearchFacets();

            var catCounts = com.solra.apis.spc.v1.GetSearchFacetsResponse.newBuilder();
            facets.categoryCounts().forEach((cat, count) ->
                    catCounts.putCategoryCounts("SPACE_CATEGORY_" + cat.name(), count));
            facets.topTags().forEach(catCounts::putTopTags);

            responseObserver.onNext(catCounts.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetSearchFacets failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== SPC-005: Preload ==========

    @Override
    public void predictPreload(com.solra.apis.spc.v1.PredictPreloadRequest request,
                                StreamObserver<com.solra.apis.spc.v1.PredictPreloadResponse> responseObserver) {
        try {
            int count = request.getCount() > 0 ? request.getCount() : 3;
            var prediction = appService.predictPreload(request.getUserId().getValue(),
                    request.getCurrentSpaceId().getValue(), count);

            responseObserver.onNext(com.solra.apis.spc.v1.PredictPreloadResponse.newBuilder()
                    .addAllPredictedSpaceIds(prediction.predictedSpaceIds())
                    .putAllLoadTimeEstimatesMs(prediction.loadTimeEstimatesMs())
                    .setPreloadCoverageRate(prediction.preloadCoverageRate())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("PredictPreload failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getPreloadStats(com.solra.apis.spc.v1.GetPreloadStatsRequest request,
                                 StreamObserver<com.solra.apis.spc.v1.GetPreloadStatsResponse> responseObserver) {
        try {
            var stats = appService.getPreloadStats();
            responseObserver.onNext(com.solra.apis.spc.v1.GetPreloadStatsResponse.newBuilder()
                    .setActiveUsers(stats.activeUsers())
                    .setTotalPredictions(stats.totalPredictions())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetPreloadStats failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== SPC-006: Loading Transition ==========

    @Override
    public void getLoadingTransition(com.solra.apis.spc.v1.GetLoadingTransitionRequest request,
                                      StreamObserver<com.solra.apis.spc.v1.GetLoadingTransitionResponse> responseObserver) {
        try {
            var transition = appService.getLoadingTransition(request.getSpaceId().getValue());

            List<com.solra.apis.spc.v1.TransitionKeyframeDTO> protoKeyframes = new ArrayList<>();
            for (var kf : transition.keyframes()) {
                protoKeyframes.add(com.solra.apis.spc.v1.TransitionKeyframeDTO.newBuilder()
                        .setProgress(kf.progress())
                        .setCssTransform(kf.cssTransform()).build());
            }

            responseObserver.onNext(com.solra.apis.spc.v1.GetLoadingTransitionResponse.newBuilder()
                    .setTransition(com.solra.apis.spc.v1.LoadingTransitionDTO.newBuilder()
                            .setSpaceId(transition.spaceId())
                            .setEffect(transition.effect().name())
                            .setDurationMs(transition.durationMs())
                            .setEasing(transition.easing())
                            .addAllKeyframes(protoKeyframes)
                            .setThumbnailUrl(nn(transition.thumbnailUrl())).build())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetLoadingTransition failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getTransitionPresets(com.solra.apis.spc.v1.GetTransitionPresetsRequest request,
                                      StreamObserver<com.solra.apis.spc.v1.GetTransitionPresetsResponse> responseObserver) {
        try {
            var presets = appService.getTransitionPresets();
            List<com.solra.apis.spc.v1.TransitionPresetDTO> protoPresets = new ArrayList<>();
            for (var p : presets) {
                protoPresets.add(com.solra.apis.spc.v1.TransitionPresetDTO.newBuilder()
                        .setId(p.id()).setName(p.name()).setEffect(p.effect().name())
                        .setDefaultDurationMs(p.defaultDurationMs())
                        .setEasing(p.easing()).setDescription(p.description()).build());
            }
            responseObserver.onNext(com.solra.apis.spc.v1.GetTransitionPresetsResponse.newBuilder()
                    .addAllPresets(protoPresets).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetTransitionPresets failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== SPC-007: Exit Flow ==========

    @Override
    public void getExitFlow(com.solra.apis.spc.v1.GetExitFlowRequest request,
                             StreamObserver<com.solra.apis.spc.v1.GetExitFlowResponse> responseObserver) {
        try {
            List<String> candidates = request.getNextCandidatesList().stream()
                    .map(Common.SpaceId::getValue).toList();

            var exitFlow = appService.getExitFlow(request.getUserId().getValue(),
                    request.getCurrentSpaceId().getValue(), candidates);

            // Build exit transition
            List<com.solra.apis.spc.v1.TransitionKeyframeDTO> protoKfs = new ArrayList<>();
            for (var kf : exitFlow.exit().keyframes()) {
                protoKfs.add(com.solra.apis.spc.v1.TransitionKeyframeDTO.newBuilder()
                        .setProgress(kf.progress()).setCssTransform(kf.cssTransform()).build());
            }

            var exitTransition = com.solra.apis.spc.v1.ExitTransitionDTO.newBuilder()
                    .setSpaceId(exitFlow.exit().spaceId())
                    .setEffect(exitFlow.exit().effect().name())
                    .setDurationMs(exitFlow.exit().durationMs())
                    .setEasing(exitFlow.exit().easing())
                    .addAllKeyframes(protoKfs).build();

            // Build next preview card (if available)
            var builder = com.solra.apis.spc.v1.GetExitFlowResponse.newBuilder()
                    .setExitTransition(exitTransition)
                    .setTotalDurationMs(exitFlow.totalDurationMs());

            if (exitFlow.nextPreview() != null) {
                builder.setNextPreview(buildProtoPreviewCard(
                        SpcResultDTO.PreviewCardDTO.from(exitFlow.nextPreview())));
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetExitFlow failed", e);
            responseObserver.onError(e);
        }
    }

    // ========== SPC-011: CDN Distribution ==========

    @Override
    public void getCdnManifest(com.solra.apis.spc.v1.GetCdnManifestRequest request,
                                StreamObserver<com.solra.apis.spc.v1.GetCdnManifestResponse> responseObserver) {
        try {
            var manifest = appService.getCdnManifest(request.getSpaceId().getValue(),
                    request.getClientRegion());

            List<com.solra.apis.spc.v1.CdnAssetUrlDTO> protoUrls = new ArrayList<>();
            for (var au : manifest.assetUrls()) {
                protoUrls.add(com.solra.apis.spc.v1.CdnAssetUrlDTO.newBuilder()
                        .setAssetId(au.assetId()).setSignedUrl(au.signedUrl())
                        .setRegion(au.region()).setEdgeBaseUrl(au.edgeBaseUrl()).build());
            }

            responseObserver.onNext(com.solra.apis.spc.v1.GetCdnManifestResponse.newBuilder()
                    .setSpaceId(manifest.spaceId())
                    .setEdgeNode(manifest.edgeNode())
                    .setRegion(manifest.region())
                    .addAllAssetUrls(protoUrls)
                    .setCachePolicy(com.solra.apis.spc.v1.CdnCachePolicyDTO.newBuilder()
                            .setTtlSeconds(manifest.cachePolicy().ttlSeconds())
                            .setEnableCompression(manifest.cachePolicy().enableCompression())
                            .setCacheControlHeader(manifest.cachePolicy().cacheControlHeader())
                            .setContentEncoding(manifest.cachePolicy().contentEncoding()).build())
                    .setGeneratedAt(ts(manifest.generatedAt())).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetCdnManifest failed", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCdnStats(com.solra.apis.spc.v1.GetCdnStatsRequest request,
                             StreamObserver<com.solra.apis.spc.v1.GetCdnStatsResponse> responseObserver) {
        try {
            var stats = appService.getCdnStats();
            responseObserver.onNext(com.solra.apis.spc.v1.GetCdnStatsResponse.newBuilder()
                    .setTotalNodes(stats.totalNodes())
                    .setHealthyNodes(stats.healthyNodes())
                    .setCachedUrls(stats.cachedUrls())
                    .setHealthRate(stats.healthRate()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("GetCdnStats failed", e);
            responseObserver.onError(e);
        }
    }

    // ==================== 映射工具方法 ====================

    private com.solra.apis.spc.v1.Space buildProtoSpace(SpcResultDTO.SpaceDTO dto) {
        return com.solra.apis.spc.v1.Space.newBuilder()
                .setSpaceId(Common.SpaceId.newBuilder().setValue(dto.spaceId()).build())
                .setMeta(com.solra.apis.spc.v1.SpaceMeta.newBuilder()
                        .setTitle(nn(dto.title()))
                        .setDescription(nn(dto.description()))
                        .setCategory(mapCat(dto.category()))
                        .setThumbnailUrl(nn(dto.thumbnailUrl()))
                        .setPrivacy(mapPrivacy(dto.privacy())).build())
                .setStats(com.solra.apis.spc.v1.SpaceStats.newBuilder()
                        .setViewCount(dto.stats() != null ? dto.stats().viewCount() : 0)
                        .setLikeCount(dto.stats() != null ? dto.stats().likeCount() : 0)
                        .setShareCount(dto.stats() != null ? dto.stats().shareCount() : 0)
                        .setVisitorCount(dto.stats() != null ? dto.stats().visitorCount() : 0)
                        .setConversationCount(dto.stats() != null ? dto.stats().conversationCount() : 0)
                        .setRating(dto.stats() != null ? dto.stats().rating() : 0f).build())
                .setCreatorId(Common.UserId.newBuilder().setValue(nn(dto.creatorId())).build())
                .setStatus(mapStatus(dto.status()))
                .setCreatedAt(ts(dto.createdAt())).setUpdatedAt(ts(dto.updatedAt()))
                .addAllTags(dto.tags() != null ? dto.tags() : List.of()).build();
    }

    private com.solra.apis.spc.v1.PreviewCard buildProtoPreviewCard(SpcResultDTO.PreviewCardDTO c) {
        return com.solra.apis.spc.v1.PreviewCard.newBuilder()
                .setSpaceId(Common.SpaceId.newBuilder().setValue(c.spaceId()).build())
                .setMeta(com.solra.apis.spc.v1.SpaceMeta.newBuilder()
                        .setTitle(c.meta() != null ? nn(c.meta().title()) : "")
                        .setDescription(c.meta() != null ? nn(c.meta().description()) : "")
                        .setCategory(c.meta() != null ? mapCat(c.meta().category())
                                : com.solra.apis.spc.v1.SpaceCategory.SPACE_CATEGORY_UNSPECIFIED)
                        .setThumbnailUrl(c.meta() != null ? nn(c.meta().thumbnailUrl()) : "")
                        .setPrivacy(c.meta() != null ? mapPrivacy(c.meta().privacy())
                                : com.solra.apis.spc.v1.SpacePrivacy.SPACE_PRIVACY_UNSPECIFIED).build())
                .addAllPreviewImages(c.previewImages() != null ? c.previewImages() : List.of())
                .setPreviewVideoUrl(nn(c.previewVideoUrl()))
                .setStats(c.stats() != null ? com.solra.apis.spc.v1.SpaceStats.newBuilder()
                        .setViewCount(c.stats().viewCount()).setLikeCount(c.stats().likeCount())
                        .setShareCount(c.stats().shareCount()).setVisitorCount(c.stats().visitorCount())
                        .setConversationCount(c.stats().conversationCount()).setRating(c.stats().rating()).build()
                        : com.solra.apis.spc.v1.SpaceStats.getDefaultInstance())
                .addAllTags(c.tags() != null ? c.tags() : List.of()).build();
    }

    private com.solra.apis.spc.v1.SpaceMeta buildProtoMeta(String spaceId) {
        return com.solra.apis.spc.v1.SpaceMeta.newBuilder()
                .setTitle("Space " + spaceId).setDescription("").build();
    }

    private com.solra.apis.spc.v1.SpaceCategory mapCat(String n) {
        try { return com.solra.apis.spc.v1.SpaceCategory.valueOf("SPACE_CATEGORY_" + (n != null ? n.toUpperCase() : "UNSPECIFIED")); }
        catch (Exception e) { return com.solra.apis.spc.v1.SpaceCategory.SPACE_CATEGORY_UNSPECIFIED; }
    }

    private com.solra.apis.spc.v1.SpacePrivacy mapPrivacy(String n) {
        try { return com.solra.apis.spc.v1.SpacePrivacy.valueOf("SPACE_PRIVACY_" + (n != null ? n.toUpperCase() : "UNSPECIFIED")); }
        catch (Exception e) { return com.solra.apis.spc.v1.SpacePrivacy.SPACE_PRIVACY_UNSPECIFIED; }
    }

    private com.solra.apis.spc.v1.SpaceStatus mapStatus(String n) {
        try { return com.solra.apis.spc.v1.SpaceStatus.valueOf("SPACE_STATUS_" + (n != null ? n.toUpperCase() : "UNSPECIFIED")); }
        catch (Exception e) { return com.solra.apis.spc.v1.SpaceStatus.SPACE_STATUS_UNSPECIFIED; }
    }

    private Common.SolraTimestamp ts(Instant i) {
        return i == null ? Common.SolraTimestamp.getDefaultInstance()
                : Common.SolraTimestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }

    private String nn(String s) { return s != null ? s : ""; }

    // ---- Flow → gRPC stream 适配器 ----
    private static class FlowSubscriberAdapter<T> implements Flow.Subscriber<T> {
        private final java.util.function.Consumer<T> onNext;
        private final Runnable onComplete;
        private final java.util.function.Consumer<Throwable> onError;

        FlowSubscriberAdapter(java.util.function.Consumer<T> onNext, Runnable onComplete,
                              java.util.function.Consumer<Throwable> onError) {
            this.onNext = onNext; this.onComplete = onComplete; this.onError = onError;
        }
        @Override public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
        @Override public void onNext(T item) { onNext.accept(item); }
        @Override public void onError(Throwable t) { onError.accept(t); }
        @Override public void onComplete() { onComplete.run(); }
    }
}
