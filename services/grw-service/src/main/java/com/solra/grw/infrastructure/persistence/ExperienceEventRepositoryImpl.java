package com.solra.grw.infrastructure.persistence;

import com.solra.grw.domain.model.ExperienceEvent;
import com.solra.grw.domain.repository.ExperienceEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExperienceEventRepositoryImpl implements ExperienceEventRepository {

    private final ExperienceEventJpaRepository jpaRepo;

    public ExperienceEventRepositoryImpl(ExperienceEventJpaRepository jpaRepo) { this.jpaRepo = jpaRepo; }

    @Override
    public ExperienceEvent save(ExperienceEvent event) {
        ExperienceEventEntity e = toEntity(event);
        ExperienceEventEntity saved = jpaRepo.save(e);
        return toDomain(saved);
    }

    @Override
    public List<ExperienceEvent> findByUserId(String userId, int limit) {
        return jpaRepo.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ExperienceEvent> findByUserIdAndTimeRange(String userId, Instant from, Instant to) {
        return jpaRepo.findByUserIdAndTimestampBetween(userId, from, to)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public int sumValueByUserId(String userId) {
        return jpaRepo.sumValueByUserId(userId);
    }

    ExperienceEvent toDomain(ExperienceEventEntity e) {
        ExperienceEvent ev = new ExperienceEvent();
        ev.setEventId(e.getEventId());
        ev.setUserId(e.getUserId());
        ev.setEventType(e.getEventType());
        ev.setSpaceId(e.getSpaceId());
        ev.setValue(e.getValue());
        ev.setMetadata(e.getMetadata());
        ev.setTimestamp(e.getTimestamp());
        return ev;
    }

    ExperienceEventEntity toEntity(ExperienceEvent ev) {
        ExperienceEventEntity e = new ExperienceEventEntity();
        e.setEventId(ev.getEventId());
        e.setUserId(ev.getUserId());
        e.setEventType(ev.getEventType());
        e.setSpaceId(ev.getSpaceId());
        e.setValue(ev.getValue());
        e.setMetadata(ev.getMetadata());
        e.setTimestamp(ev.getTimestamp());
        return e;
    }
}
