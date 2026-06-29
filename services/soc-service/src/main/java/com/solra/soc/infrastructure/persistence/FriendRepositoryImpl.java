package com.solra.soc.infrastructure.persistence;

import com.solra.soc.domain.model.Friend;
import com.solra.soc.domain.model.FriendStatus;
import com.solra.soc.domain.repository.FriendRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * FriendRepositoryImpl — 将领域仓储接口适配到 JPA 持久化。
 */
@Component
public class FriendRepositoryImpl implements FriendRepository {

    private final FriendJpaRepository jpaRepo;

    public FriendRepositoryImpl(FriendJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Friend save(Friend friend) {
        FriendEntity entity = toEntity(friend);
        FriendEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Friend> findByFriendshipId(String friendshipId) {
        return jpaRepo.findById(friendshipId).map(this::toDomain);
    }

    @Override
    public List<Friend> findByUserId(String userId, int page, int size) {
        return jpaRepo.findByUserId(userId, PageRequest.of(page, size))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByUserId(String userId) {
        return jpaRepo.countByUserId(userId);
    }

    @Override
    public void delete(String friendshipId) {
        jpaRepo.deleteById(friendshipId);
    }

    // ---- mapping ----

    Friend toDomain(FriendEntity e) {
        Friend f = new Friend();
        f.setFriendshipId(e.getFriendshipId());
        f.setUserId(e.getUserId());
        f.setFriendUserId(e.getFriendUserId());
        f.setStatus(FriendStatus.valueOf(e.getStatus()));
        f.setCreatedAt(e.getCreatedAt());
        f.setAcceptedAt(e.getAcceptedAt());
        return f;
    }

    FriendEntity toEntity(Friend f) {
        FriendEntity e = new FriendEntity();
        e.setFriendshipId(f.getFriendshipId());
        e.setUserId(f.getUserId());
        e.setFriendUserId(f.getFriendUserId());
        e.setStatus(f.getStatus().name());
        e.setCreatedAt(f.getCreatedAt());
        e.setAcceptedAt(f.getAcceptedAt());
        return e;
    }
}
