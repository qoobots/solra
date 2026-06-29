package com.solra.grw.infrastructure.persistence;

import com.solra.grw.domain.model.OnboardingPath;
import com.solra.grw.domain.model.OnboardingStatus;
import com.solra.grw.domain.repository.OnboardingPathRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OnboardingPathRepositoryImpl implements OnboardingPathRepository {

    private final OnboardingPathJpaRepository jpaRepo;

    public OnboardingPathRepositoryImpl(OnboardingPathJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public OnboardingPath save(OnboardingPath path) {
        OnboardingPathEntity e = toEntity(path);
        OnboardingPathEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public Optional<OnboardingPath> findByUserId(String userId) {
        return jpaRepo.findByUserId(userId).map(this::toDomain);
    }

    OnboardingPath toDomain(OnboardingPathEntity e) {
        OnboardingPath p = new OnboardingPath();
        p.setPathId(e.getPathId());
        p.setUserId(e.getUserId());
        p.setCurrentStep(e.getCurrentStep());
        p.setTotalSteps(e.getTotalSteps());
        p.setStartTime(e.getStartTime());
        p.setCompletedAt(e.getCompletedAt());
        p.setStatus(OnboardingStatus.valueOf(e.getStatus()));
        return p;
    }

    OnboardingPathEntity toEntity(OnboardingPath p) {
        OnboardingPathEntity e = new OnboardingPathEntity();
        e.setPathId(p.getPathId());
        e.setUserId(p.getUserId());
        e.setCurrentStep(p.getCurrentStep());
        e.setTotalSteps(p.getTotalSteps());
        e.setStartTime(p.getStartTime());
        e.setCompletedAt(p.getCompletedAt());
        e.setStatus(p.getStatus() != null ? p.getStatus().name() : "NOT_STARTED");
        return e;
    }
}
