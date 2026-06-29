package com.solra.soc.infrastructure.engine;

import com.solra.soc.domain.model.*;
import com.solra.soc.domain.repository.ShareSessionRepository;
import com.solra.soc.domain.service.ShareEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * DefaultShareEngine — ShareEngine 领域服务默认实现。
 * 使用 JPA 持久化和内存缓存实现分享引擎逻辑。
 */
@Component
public class DefaultShareEngine implements ShareEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultShareEngine.class);

    private final ShareSessionRepository sessionRepo;

    public DefaultShareEngine(ShareSessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Override
    public ShareSession generateShareLink(String spaceId, String sharerUserId, String type) {
        ShareType shareType = ShareType.valueOf(type.toUpperCase());
        String shareId = UUID.randomUUID().toString();
        String shareCode = generateShareCode();
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS); // 7天有效期

        ShareSession session = new ShareSession(shareId, spaceId, sharerUserId, shareType, shareCode, expiresAt);
        ShareSession saved = sessionRepo.save(session);

        log.info("ShareChain created: session={} space={} sharer={} type={}", shareId, spaceId, sharerUserId, type);
        return saved;
    }

    @Override
    public Optional<ShareClick> trackClick(String shareCode, VisitorInfo visitorInfo) {
        Optional<ShareSession> sessionOpt = sessionRepo.findByShareCode(shareCode);
        if (sessionOpt.isEmpty()) return Optional.empty();

        ShareSession session = sessionOpt.get();
        if (session.isExpired()) return Optional.empty();

        session.recordClick(visitorInfo.visitorUserId());
        sessionRepo.update(session);

        ShareClick click = new ShareClick(
                UUID.randomUUID().toString(),
                session.getShareId(),
                visitorInfo.visitorUserId(),
                visitorInfo.ipAddress(),
                visitorInfo.userAgent(),
                visitorInfo.platform()
        );

        log.info("ShareChain click: session={} visitor={}", session.getShareId(), visitorInfo.visitorUserId());
        return Optional.of(click);
    }

    @Override
    public boolean trackConversion(String shareCode, String userId) {
        Optional<ShareSession> sessionOpt = sessionRepo.findByShareCode(shareCode);
        if (sessionOpt.isEmpty() || sessionOpt.get().isExpired()) return false;

        ShareSession session = sessionOpt.get();
        session.recordConversion(userId);
        sessionRepo.update(session);

        log.info("ShareChain conversion: session={} user={}", session.getShareId(), userId);
        return true;
    }

    @Override
    public Optional<ViralStats> getViralChainStats(String shareCode) {
        return sessionRepo.findByShareCode(shareCode).map(session -> {
            double conversionRate = session.getClickCount() > 0
                    ? (double) session.getConversionCount() / session.getClickCount()
                    : 0.0;
            return new ViralStats(
                    session.getShareId(),
                    session.getShareCode(),
                    session.getClickCount(),
                    session.getConversionCount(),
                    conversionRate,
                    java.util.List.of(session.getSharerUserId()),
                    String.join(" → ", session.getViralChain())
            );
        });
    }

    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
