package com.solra.soc.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solra.soc.domain.model.FriendGroup;
import com.solra.soc.domain.repository.FriendGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * FriendGroupRepositoryImpl — 好友分组仓储 JPA 实现。
 */
@Component
public class FriendGroupRepositoryImpl implements FriendGroupRepository {

    private static final Logger log = LoggerFactory.getLogger(FriendGroupRepositoryImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final FriendGroupJpaRepository jpaRepo;

    public FriendGroupRepositoryImpl(FriendGroupJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public FriendGroup save(FriendGroup group) {
        FriendGroupEntity entity = toEntity(group);
        FriendGroupEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FriendGroup> findById(String groupId) {
        return jpaRepo.findById(groupId).map(this::toDomain);
    }

    @Override
    public List<FriendGroup> findByUserId(String userId) {
        return jpaRepo.findByUserIdOrderBySortOrder(userId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public List<FriendGroup> findByUserIdAndMember(String userId, String friendUserId) {
        return jpaRepo.findByUserIdAndMember(userId, friendUserId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public void deleteById(String groupId) {
        jpaRepo.deleteById(groupId);
    }

    // ---- mapping ----

    FriendGroup toDomain(FriendGroupEntity e) {
        FriendGroup g = new FriendGroup();
        g.setGroupId(e.getGroupId());
        g.setUserId(e.getUserId());
        g.setGroupName(e.getGroupName());
        g.setSortOrder(e.getSortOrder());
        g.setMemberUserIds(parseMemberIds(e.getMemberUserIds()));
        g.setCreatedAt(e.getCreatedAt());
        g.setUpdatedAt(e.getUpdatedAt());
        return g;
    }

    FriendGroupEntity toEntity(FriendGroup g) {
        FriendGroupEntity e = new FriendGroupEntity();
        e.setGroupId(g.getGroupId());
        e.setUserId(g.getUserId());
        e.setGroupName(g.getGroupName());
        e.setSortOrder(g.getSortOrder());
        e.setMemberUserIds(serializeMemberIds(g.getMemberUserIds()));
        e.setCreatedAt(g.getCreatedAt());
        e.setUpdatedAt(g.getUpdatedAt());
        return e;
    }

    private List<String> parseMemberIds(String json) {
        if (json == null || json.isEmpty()) return new ArrayList<>();
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse member IDs JSON: {}", json, e);
            return new ArrayList<>();
        }
    }

    private String serializeMemberIds(List<String> ids) {
        try {
            return mapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize member IDs", e);
            return "[]";
        }
    }
}
