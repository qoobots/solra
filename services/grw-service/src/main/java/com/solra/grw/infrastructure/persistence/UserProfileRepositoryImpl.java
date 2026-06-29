package com.solra.grw.infrastructure.persistence;

import com.solra.grw.domain.model.FaithLevel;
import com.solra.grw.domain.model.UserProfile;
import com.solra.grw.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final UserProfileJpaRepository jpaRepo;

    public UserProfileRepositoryImpl(UserProfileJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileEntity e = toEntity(profile);
        UserProfileEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<UserProfile> findByUserId(String userId) {
        return jpaRepo.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public void updatePresenceScore(String userId, double delta) {
        jpaRepo.findByUserId(userId).ifPresent(e -> {
            e.setPresenceScore(Math.max(0, e.getPresenceScore() + delta));
            jpaRepo.save(e);
        });
    }

    @Override
    public void updateFaithLevel(String userId, String newLevel) {
        jpaRepo.findByUserId(userId).ifPresent(e -> {
            e.setFaithLevel(newLevel);
            jpaRepo.save(e);
        });
    }

    UserProfile toDomain(UserProfileEntity e) {
        UserProfile p = new UserProfile();
        p.setUserId(e.getUserId());
        p.setPresenceScore(e.getPresenceScore());
        p.setFaithLevel(FaithLevel.valueOf(e.getFaithLevel()));
        p.setTotalInteractions(e.getTotalInteractions());
        p.setSpacesVisited(e.getSpacesVisited());
        p.setConversationsHad(e.getConversationsHad());
        p.setFriendsCount(e.getFriendsCount());
        p.setCreatedAt(e.getCreatedAt());
        p.setUpdatedAt(e.getUpdatedAt());
        p.setLastActiveAt(e.getLastActiveAt());
        p.setCurrentOnboardingStep(e.getCurrentOnboardingStep());
        p.setOnboardingCompleted(e.isOnboardingCompleted());
        return p;
    }

    UserProfileEntity toEntity(UserProfile p) {
        UserProfileEntity e = new UserProfileEntity();
        e.setUserId(p.getUserId());
        e.setPresenceScore(p.getPresenceScore());
        e.setFaithLevel(p.getFaithLevel() != null ? p.getFaithLevel().name() : "SEEKER");
        e.setTotalInteractions(p.getTotalInteractions());
        e.setSpacesVisited(p.getSpacesVisited());
        e.setConversationsHad(p.getConversationsHad());
        e.setFriendsCount(p.getFriendsCount());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setLastActiveAt(p.getLastActiveAt());
        e.setCurrentOnboardingStep(p.getCurrentOnboardingStep());
        e.setOnboardingCompleted(p.isOnboardingCompletedFlag());
        return e;
    }
}
