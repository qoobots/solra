package com.solra.spc.infrastructure.persistence;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.SpaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SpaceRepositoryImpl implements SpaceRepository {

    private final SpaceJpaRepository jpa;

    public SpaceRepositoryImpl(SpaceJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public Optional<Space> findById(String spaceId) {
        return jpa.findById(spaceId).map(this::toDomain);
    }

    @Override
    public List<Space> findByIds(List<String> spaceIds) {
        return jpa.findByIdIn(spaceIds).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Space> findPublished(int offset, int limit, List<SpaceCategory> categories, String sortBy) {
        String cat = (categories != null && !categories.isEmpty()) ? categories.get(0).name() : null;
        var pageable = PageRequest.of(offset / Math.max(1, limit), limit);
        List<SpaceEntity> entities = "popular".equals(sortBy)
                ? jpa.findPublishedByPopular(pageable, cat)
                : jpa.findPublished(pageable, cat);
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countPublished(List<SpaceCategory> categories) {
        String cat = (categories != null && !categories.isEmpty()) ? categories.get(0).name() : null;
        return jpa.countPublished(cat);
    }

    @Override
    @Transactional
    public void save(Space space) { jpa.save(toEntity(space)); }

    @Override
    @Transactional
    public void incrementViewCount(String spaceId) { jpa.incrementViewCount(spaceId); }

    @Override
    @Transactional
    public void incrementLikeCount(String spaceId) { jpa.incrementLikeCount(spaceId); }

    @Override
    @Transactional
    public void incrementShareCount(String spaceId) { jpa.incrementShareCount(spaceId); }

    private Space toDomain(SpaceEntity e) {
        Space s = new Space();
        s.setSpaceId(e.getSpaceId());
        SpaceMeta meta = new SpaceMeta(e.getTitle(), e.getDescription(),
                e.getCategory() != null ? SpaceCategory.valueOf(e.getCategory()) : SpaceCategory.PERSONAL);
        meta.setThumbnailUrl(e.getThumbnailUrl());
        meta.setLatitude(e.getLatitude());
        meta.setLongitude(e.getLongitude());
        meta.setPrivacy(e.getPrivacy() != null ? SpacePrivacy.valueOf(e.getPrivacy()) : SpacePrivacy.PUBLIC);
        meta.setLanguageCode(e.getLanguageCode());
        s.setMeta(meta);

        SpaceStats stats = new SpaceStats();
        stats.setViewCount(e.getViewCount());
        stats.setLikeCount(e.getLikeCount());
        stats.setShareCount(e.getShareCount());
        stats.setVisitorCount(e.getVisitorCount());
        stats.setConversationCount(e.getConversationCount());
        stats.setRating(e.getRating());
        s.setStats(stats);
        s.setCreatorId(e.getCreatorId());
        s.setStatus(e.getStatus() != null ? SpaceStatus.valueOf(e.getStatus()) : SpaceStatus.DRAFT);
        s.setCreatedAt(e.getCreatedAt());
        s.setUpdatedAt(e.getUpdatedAt());
        s.setTags(e.getTags());
        s.setMetadata(e.getMetadata());

        SpaceContent content = new SpaceContent();
        content.setSceneFileUrl(e.getSceneFileUrl());
        content.setEntryPoint(e.getEntryPoint());
        s.setContent(content);
        return s;
    }

    private SpaceEntity toEntity(Space s) {
        SpaceEntity e = new SpaceEntity();
        e.setSpaceId(s.getSpaceId());
        if (s.getMeta() != null) {
            e.setTitle(s.getMeta().getTitle());
            e.setDescription(s.getMeta().getDescription());
            e.setCategory(s.getMeta().getCategory() != null ? s.getMeta().getCategory().name() : null);
            e.setThumbnailUrl(s.getMeta().getThumbnailUrl());
            e.setLatitude(s.getMeta().getLatitude());
            e.setLongitude(s.getMeta().getLongitude());
            e.setPrivacy(s.getMeta().getPrivacy() != null ? s.getMeta().getPrivacy().name() : null);
            e.setLanguageCode(s.getMeta().getLanguageCode());
        }
        if (s.getContent() != null) {
            e.setSceneFileUrl(s.getContent().getSceneFileUrl());
            e.setEntryPoint(s.getContent().getEntryPoint());
        }
        if (s.getStats() != null) {
            e.setViewCount(s.getStats().getViewCount());
            e.setLikeCount(s.getStats().getLikeCount());
            e.setShareCount(s.getStats().getShareCount());
            e.setVisitorCount(s.getStats().getVisitorCount());
            e.setConversationCount(s.getStats().getConversationCount());
            e.setRating(s.getStats().getRating());
        }
        e.setCreatorId(s.getCreatorId());
        e.setStatus(s.getStatus() != null ? s.getStatus().name() : "DRAFT");
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        e.setTags(s.getTags());
        e.setMetadata(s.getMetadata());
        return e;
    }
}
