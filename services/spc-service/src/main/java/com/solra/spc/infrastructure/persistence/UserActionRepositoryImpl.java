package com.solra.spc.infrastructure.persistence;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.UserActionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserActionRepositoryImpl implements UserActionRepository {
    private final UserActionJpaRepository jpa;

    public UserActionRepositoryImpl(UserActionJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public void save(UserAction action) { jpa.save(toEntity(action)); }

    @Override
    public List<UserAction> findByUserId(String userId, int limit) {
        return jpa.findByUserIdOrderByActionTimeDesc(userId, PageRequest.of(0, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserAction> findBySpaceId(String spaceId, int limit) {
        return jpa.findBySpaceIdOrderByActionTimeDesc(spaceId, PageRequest.of(0, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private UserAction toDomain(UserActionEntity e) {
        UserAction a = new UserAction();
        a.setActionId(e.getActionId());
        a.setUserId(e.getUserId());
        a.setSpaceId(e.getSpaceId());
        a.setActionType(UserActionType.valueOf(e.getActionType()));
        a.setDwellDurationMs(e.getDwellDurationMs());
        a.setActionTime(e.getActionTime());
        return a;
    }

    private UserActionEntity toEntity(UserAction a) {
        UserActionEntity e = new UserActionEntity();
        e.setActionId(a.getActionId());
        e.setUserId(a.getUserId());
        e.setSpaceId(a.getSpaceId());
        e.setActionType(a.getActionType().name());
        e.setDwellDurationMs(a.getDwellDurationMs());
        e.setActionTime(a.getActionTime());
        return e;
    }
}
