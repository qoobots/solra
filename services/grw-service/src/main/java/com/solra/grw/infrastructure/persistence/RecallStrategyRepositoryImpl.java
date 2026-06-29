package com.solra.grw.infrastructure.persistence;

import com.solra.grw.domain.model.ChurnRiskLevel;
import com.solra.grw.domain.model.RecallChannel;
import com.solra.grw.domain.model.RecallStrategy;
import com.solra.grw.domain.repository.RecallStrategyRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class RecallStrategyRepositoryImpl implements RecallStrategyRepository {

    private final RecallStrategyJpaRepository jpaRepo;

    public RecallStrategyRepositoryImpl(RecallStrategyJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public RecallStrategy save(RecallStrategy strategy) {
        return toDomain(jpaRepo.save(toEntity(strategy)));
    }

    @Override
    public Optional<RecallStrategy> findById(String strategyId) {
        return jpaRepo.findById(strategyId).map(this::toDomain);
    }

    @Override
    public List<RecallStrategy> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RecallStrategy> findByRiskLevel(String riskLevel) {
        return jpaRepo.findByTargetRiskLevel(riskLevel).stream()
                .map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<RecallStrategy> findActive() {
        return jpaRepo.findByActiveTrue().stream().map(this::toDomain).collect(Collectors.toList());
    }

    private RecallStrategy toDomain(RecallStrategyEntity e) {
        RecallStrategy s = new RecallStrategy();
        s.setStrategyId(e.getStrategyId());
        s.setName(e.getName());
        s.setTargetRiskLevel(ChurnRiskLevel.valueOf(e.getTargetRiskLevel()));
        s.setInactiveDaysMin(e.getInactiveDaysMin());
        s.setInactiveDaysMax(e.getInactiveDaysMax());
        s.setTitleTemplate(e.getTitleTemplate());
        s.setMessageTemplate(e.getMessageTemplate());
        s.setChannels(parseChannels(e.getChannels()));
        s.setMaxAttempts(e.getMaxAttempts());
        s.setCooldownHours(e.getCooldownHours());
        s.setActive(e.isActive());
        s.setCreatedAt(e.getCreatedAt());
        s.setUpdatedAt(e.getUpdatedAt());
        return s;
    }

    private RecallStrategyEntity toEntity(RecallStrategy s) {
        RecallStrategyEntity e = new RecallStrategyEntity();
        e.setStrategyId(s.getStrategyId());
        e.setName(s.getName());
        e.setTargetRiskLevel(s.getTargetRiskLevel() != null ? s.getTargetRiskLevel().name() : "LOW");
        e.setInactiveDaysMin(s.getInactiveDaysMin());
        e.setInactiveDaysMax(s.getInactiveDaysMax());
        e.setTitleTemplate(s.getTitleTemplate());
        e.setMessageTemplate(s.getMessageTemplate());
        e.setChannels(channelsToString(s.getChannels()));
        e.setMaxAttempts(s.getMaxAttempts());
        e.setCooldownHours(s.getCooldownHours());
        e.setActive(s.isActive());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }

    private List<RecallChannel> parseChannels(String channels) {
        if (channels == null || channels.isEmpty()) return List.of();
        return Arrays.stream(channels.split(","))
                .map(String::trim)
                .filter(c -> !c.isEmpty())
                .map(RecallChannel::valueOf)
                .collect(Collectors.toList());
    }

    private String channelsToString(List<RecallChannel> channels) {
        if (channels == null || channels.isEmpty()) return "";
        return channels.stream().map(Enum::name).collect(Collectors.joining(","));
    }
}
