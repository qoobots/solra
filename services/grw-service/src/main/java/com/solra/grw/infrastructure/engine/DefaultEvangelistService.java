package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.Evangelist;
import com.solra.grw.domain.model.UserProfile;
import com.solra.grw.domain.repository.EvangelistRepository;
import com.solra.grw.domain.repository.UserProfileRepository;
import com.solra.grw.domain.service.EvangelistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DefaultEvangelistService — 布道者体系实现。
 * GRW-003: 布道者申请/认证/权益/义务，占DAU 0.5-1%。
 */
@Component
public class DefaultEvangelistService implements EvangelistService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEvangelistService.class);

    private final EvangelistRepository evangelistRepo;
    private final UserProfileRepository userProfileRepo;

    public DefaultEvangelistService(EvangelistRepository evangelistRepo,
                                     UserProfileRepository userProfileRepo) {
        this.evangelistRepo = evangelistRepo;
        this.userProfileRepo = userProfileRepo;
    }

    @Override
    public Evangelist apply(String userId, String displayName, String bio) {
        // 检查是否已有申请
        Optional<Evangelist> existing = evangelistRepo.findByUserId(userId);
        if (existing.isPresent()) {
            Evangelist ev = existing.get();
            if (ev.getStatus() == Evangelist.ApplicationStatus.APPROVED) {
                throw new IllegalStateException("Already an approved evangelist: " + userId);
            }
            if (ev.getStatus() == Evangelist.ApplicationStatus.PENDING) {
                throw new IllegalStateException("Application already pending: " + userId);
            }
        }

        // 检查资格
        if (!isEligible(userId)) {
            throw new IllegalStateException("Not eligible for evangelist: " + userId);
        }

        Evangelist evangelist = new Evangelist(
                UUID.randomUUID().toString(), userId, displayName, bio);
        Evangelist saved = evangelistRepo.save(evangelist);
        log.info("GRW-003 evangelist application submitted: user={}", userId);
        return saved;
    }

    @Override
    public Evangelist review(String applicationId, boolean approved, String reviewerId, String comment) {
        Evangelist evangelist = evangelistRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (approved) {
            evangelist.approve(reviewerId, comment);
            log.info("GRW-003 evangelist approved: user={}", evangelist.getUserId());
        } else {
            evangelist.reject(reviewerId, comment);
            log.info("GRW-003 evangelist rejected: user={}", evangelist.getUserId());
        }

        return evangelistRepo.save(evangelist);
    }

    @Override
    public Optional<Evangelist> getEvangelistStatus(String userId) {
        return evangelistRepo.findByUserId(userId);
    }

    @Override
    public List<Evangelist> getActiveEvangelists(int page, int size) {
        return evangelistRepo.findActive(page, size);
    }

    @Override
    public List<Evangelist> getEvangelistsByTier(Evangelist.EvangelistTier tier, int page, int size) {
        return evangelistRepo.findByTier(tier, page, size);
    }

    @Override
    public void updateFollowers(String userId, int count) {
        evangelistRepo.findByUserId(userId).ifPresent(ev -> {
            ev.updateFollowers(count);
            evangelistRepo.save(ev);
        });
    }

    @Override
    public void updateVisits(String userId, int count) {
        evangelistRepo.findByUserId(userId).ifPresent(ev -> {
            ev.updateVisits(count);
            evangelistRepo.save(ev);
        });
    }

    @Override
    public void addContribution(String userId, double score) {
        evangelistRepo.findByUserId(userId).ifPresent(ev -> {
            ev.updateContributionScore(ev.getContributionScore() + score);
            evangelistRepo.save(ev);
        });
    }

    @Override
    public void suspend(String userId, String reason) {
        Evangelist ev = evangelistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Evangelist not found: " + userId));
        ev.suspend(reason);
        evangelistRepo.save(ev);
        log.warn("GRW-003 evangelist suspended: user={} reason={}", userId, reason);
    }

    @Override
    public void revoke(String userId, String reason) {
        Evangelist ev = evangelistRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Evangelist not found: " + userId));
        ev.revoke(reason);
        evangelistRepo.save(ev);
        log.warn("GRW-003 evangelist revoked: user={} reason={}", userId, reason);
    }

    @Override
    public boolean isEligible(String userId) {
        UserProfile profile = userProfileRepo.findByUserId(userId).orElse(null);
        if (profile == null) return false;
        int totalVisits = profile.getSpacesVisited();
        int friends = profile.getFriendsCount();
        return friends >= Evangelist.EvangelistTier.APPRENTICE.getMinFollowers()
                && totalVisits >= Evangelist.EvangelistTier.APPRENTICE.getMinVisits();
    }

    @Override
    public EvangelistStats getStats() {
        long totalApplications = evangelistRepo.countAll();
        long totalApproved = evangelistRepo.countByStatus(Evangelist.ApplicationStatus.APPROVED);
        long totalActive = evangelistRepo.countByStatus(Evangelist.ApplicationStatus.APPROVED);

        Map<Evangelist.EvangelistTier, Long> byTier = new LinkedHashMap<>();
        for (Evangelist.EvangelistTier tier : Evangelist.EvangelistTier.values()) {
            byTier.put(tier, evangelistRepo.countByTier(tier));
        }

        // 获取活跃布道者以计算统计
        List<Evangelist> active = evangelistRepo.findActive(0, 100);
        long avgFollowers = active.isEmpty() ? 0 :
                active.stream().mapToLong(Evangelist::getFollowersCount).sum() / active.size();
        long avgVisits = active.isEmpty() ? 0 :
                active.stream().mapToLong(Evangelist::getTotalVisits).sum() / active.size();

        // DAU占比估算 (简化)
        long totalUsers = userProfileRepo.count();
        double dauPercent = totalUsers > 0 ? (double) totalActive / totalUsers * 100 : 0.0;

        return new EvangelistStats(totalApplications, totalApproved, totalActive,
                byTier, dauPercent, avgFollowers, avgVisits);
    }
}
