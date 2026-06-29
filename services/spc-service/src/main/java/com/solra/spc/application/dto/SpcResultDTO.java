package com.solra.spc.application.dto;

import com.solra.spc.domain.model.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpcResultDTO {

    public record SpaceDTO(String spaceId, String title, String description, String category,
                            String thumbnailUrl, String privacy, String creatorId, String status,
                            SpaceStatsDTO stats, List<String> tags, Instant createdAt, Instant updatedAt) {
        public static SpaceDTO from(Space s) {
            SpaceMeta m = s.getMeta();
            SpaceStats st = s.getStats();
            return new SpaceDTO(s.getSpaceId(),
                    m != null ? m.getTitle() : "",
                    m != null ? m.getDescription() : "",
                    m != null && m.getCategory() != null ? m.getCategory().name() : "",
                    m != null ? m.getThumbnailUrl() : "",
                    m != null && m.getPrivacy() != null ? m.getPrivacy().name() : "",
                    s.getCreatorId(), s.getStatus() != null ? s.getStatus().name() : "",
                    st != null ? SpaceStatsDTO.from(st) : null,
                    s.getTags(), s.getCreatedAt(), s.getUpdatedAt());
        }
    }

    public record SpaceStatsDTO(long viewCount, long likeCount, long shareCount, long visitorCount,
                                  long conversationCount, float rating) {
        public static SpaceStatsDTO from(SpaceStats s) {
            return new SpaceStatsDTO(s.getViewCount(), s.getLikeCount(), s.getShareCount(),
                    s.getVisitorCount(), s.getConversationCount(), s.getRating());
        }
    }

    public record PreviewCardDTO(String spaceId, SpaceDTO meta, List<String> previewImages,
                                  String previewVideoUrl, SpaceStatsDTO stats, List<String> tags) {
        public static PreviewCardDTO from(PreviewCard c) {
            return new PreviewCardDTO(c.getSpaceId(),
                    c.getMeta() != null ? SpaceDTO.from(fromMeta(c.getSpaceId(), c.getMeta())) : null,
                    c.getPreviewImages(), c.getPreviewVideoUrl(),
                    c.getStats() != null ? SpaceStatsDTO.from(c.getStats()) : null,
                    c.getTags());
        }
        private static Space fromMeta(String spaceId, SpaceMeta m) {
            Space s = new Space();
            s.setSpaceId(spaceId);
            s.setMeta(m);
            return s;
        }
    }

    public record RecommendationDTO(String spaceId, float relevance, float popularity, float freshness,
                                      float overall, List<String> reasons, Instant generatedAt) {
        public static RecommendationDTO from(Recommendation r) {
            RecommendScore sc = r.getScore();
            return new RecommendationDTO(r.getSpaceId(),
                    sc != null ? sc.getRelevance() : 0, sc != null ? sc.getPopularity() : 0,
                    sc != null ? sc.getFreshness() : 0, sc != null ? sc.getOverall() : 0,
                    r.getRecommendReasons(), r.getGeneratedAt());
        }
    }

    public record AssetChunkDTO(String assetId, int chunkIndex, int totalChunks, boolean isFinal,
                                  int compressionLevel) {
        public static AssetChunkDTO from(AssetChunk c) {
            return new AssetChunkDTO(c.getAssetId(), c.getChunkIndex(), c.getTotalChunks(),
                    c.isFinal(), c.getCompressionLevel());
        }
    }

    /** SPC-008: 排行榜条目 DTO */
    public record LeaderboardEntryDTO(String spaceId, String title, String thumbnailUrl,
                                       String category, int rank, long hotScore,
                                       long viewCount, long likeCount, long shareCount,
                                       long visitorCount, float rating, int rankChange,
                                       String period, Instant snapshotAt) {
        public static LeaderboardEntryDTO from(LeaderboardEntry e) {
            return new LeaderboardEntryDTO(
                    e.getSpaceId(),
                    e.getTitle() != null ? e.getTitle() : "",
                    e.getThumbnailUrl() != null ? e.getThumbnailUrl() : "",
                    e.getCategory() != null ? e.getCategory().name() : "",
                    e.getRank(),
                    e.getHotScore(),
                    e.getViewCount(),
                    e.getLikeCount(),
                    e.getShareCount(),
                    e.getVisitorCount(),
                    e.getRating(),
                    e.getRankChange(),
                    e.getPeriod() != null ? e.getPeriod().name() : "DAILY",
                    e.getSnapshotAt()
            );
        }
    }

    /** SPC-008: 排行榜快照时间 DTO */
    public record LeaderboardSnapshotDTO(Map<String, Instant> snapshotTimes) {
        public static LeaderboardSnapshotDTO from(Map<String, Instant> times) {
            return new LeaderboardSnapshotDTO(new LinkedHashMap<>(times));
        }
    }
}
