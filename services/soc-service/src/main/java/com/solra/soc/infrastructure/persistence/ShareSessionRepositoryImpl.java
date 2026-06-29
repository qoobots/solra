package com.solra.soc.infrastructure.persistence;

import com.solra.soc.domain.model.ShareSession;
import com.solra.soc.domain.model.ShareStatus;
import com.solra.soc.domain.model.ShareType;
import com.solra.soc.domain.repository.ShareSessionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ShareSessionRepositoryImpl — 将领域仓储接口适配到 JPA 持久化。
 */
@Component
public class ShareSessionRepositoryImpl implements ShareSessionRepository {

    private final ShareSessionJpaRepository jpaRepo;
    private final ObjectMapper objectMapper;

    public ShareSessionRepositoryImpl(ShareSessionJpaRepository jpaRepo, ObjectMapper objectMapper) {
        this.jpaRepo = jpaRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public ShareSession save(ShareSession session) {
        ShareSessionEntity entity = toEntity(session);
        ShareSessionEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<ShareSession> findByShareCode(String shareCode) {
        return jpaRepo.findByShareCode(shareCode).map(this::toDomain);
    }

    @Override
    public List<ShareSession> findBySharerUserId(String sharerUserId) {
        return jpaRepo.findBySharerUserIdOrderByCreatedAtDesc(sharerUserId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void update(ShareSession session) {
        save(session);
    }

    // ---- mapping ----

    ShareSessionEntity toEntity(ShareSession d) {
        ShareSessionEntity e = new ShareSessionEntity();
        e.setShareId(d.getShareId());
        e.setSpaceId(d.getSpaceId());
        e.setSharerUserId(d.getSharerUserId());
        e.setShareType(d.getShareType().name());
        e.setShareCode(d.getShareCode());
        e.setClickCount(d.getClickCount());
        e.setConversionCount(d.getConversionCount());
        e.setCreatedAt(d.getCreatedAt());
        e.setExpiresAt(d.getExpiresAt());
        e.setViralChain(toJson(d.getViralChain()));
        e.setStatus(d.getStatus().name());
        return e;
    }

    ShareSession toDomain(ShareSessionEntity e) {
        ShareSession s = new ShareSession();
        s.setShareId(e.getShareId());
        s.setSpaceId(e.getSpaceId());
        s.setSharerUserId(e.getSharerUserId());
        s.setShareType(ShareType.valueOf(e.getShareType()));
        s.setShareCode(e.getShareCode());
        s.setClickCount(e.getClickCount());
        s.setConversionCount(e.getConversionCount());
        s.setCreatedAt(e.getCreatedAt());
        s.setExpiresAt(e.getExpiresAt());
        s.setViralChain(fromJsonList(e.getViralChain()));
        s.setStatus(ShareStatus.valueOf(e.getStatus()));
        return s;
    }

    private String toJson(List<String> list) {
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception ex) { return "[]"; }
    }

    private List<String> fromJsonList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<List<String>>() {}); }
        catch (Exception ex) { return new ArrayList<>(); }
    }
}
