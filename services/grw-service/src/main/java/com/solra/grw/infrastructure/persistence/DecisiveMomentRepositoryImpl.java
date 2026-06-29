package com.solra.grw.infrastructure.persistence;

import com.solra.grw.domain.model.DecisiveMoment;
import com.solra.grw.domain.model.DecisiveMomentType;
import com.solra.grw.domain.repository.DecisiveMomentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DecisiveMomentRepositoryImpl implements DecisiveMomentRepository {

    private final DecisiveMomentJpaRepository jpaRepo;
    private final ObjectMapper objectMapper;

    public DecisiveMomentRepositoryImpl(DecisiveMomentJpaRepository jpaRepo, ObjectMapper objectMapper) {
        this.jpaRepo = jpaRepo;
        this.objectMapper = objectMapper;
    }

    @Override
    public DecisiveMoment save(DecisiveMoment moment) {
        DecisiveMomentEntity e = toEntity(moment);
        DecisiveMomentEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public List<DecisiveMoment> findByUserId(String userId) {
        return jpaRepo.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<DecisiveMoment> findByUserIdAndType(String userId, String momentType) {
        return jpaRepo.findByUserIdAndMomentType(userId, momentType).map(this::toDomain);
    }

    DecisiveMoment toDomain(DecisiveMomentEntity e) {
        DecisiveMoment dm = new DecisiveMoment(e.getMomentId(), e.getUserId(),
                DecisiveMomentType.valueOf(e.getMomentType()));
        dm.setDetectedAt(e.getDetectedAt());
        dm.setConversionValue(e.getConversionValue());
        dm.setTriggered(e.isTriggered());
        dm.setStateSnapshot(fromJsonMap(e.getStateBefore()), fromJsonMap(e.getStateAfter()));
        return dm;
    }

    DecisiveMomentEntity toEntity(DecisiveMoment dm) {
        DecisiveMomentEntity e = new DecisiveMomentEntity();
        e.setMomentId(dm.getMomentId());
        e.setUserId(dm.getUserId());
        e.setMomentType(dm.getMomentType() != null ? dm.getMomentType().name() : null);
        e.setDetectedAt(dm.getDetectedAt());
        e.setConversionValue(dm.getConversionValue());
        e.setTriggered(dm.getTriggered());
        e.setStateBefore(toJson(dm.getUserStateBefore()));
        e.setStateAfter(toJson(dm.getUserStateAfter()));
        return e;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try { return objectMapper.readValue(json, Map.class); } catch (Exception ex) { return new HashMap<>(); }
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        try { return objectMapper.writeValueAsString(map); } catch (Exception ex) { return "{}"; }
    }
}
