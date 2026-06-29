package com.solra.grw.infrastructure.engine;

import com.solra.grw.domain.model.AvatarCollection;
import com.solra.grw.domain.repository.AvatarCollectionRepository;
import com.solra.grw.domain.service.AvatarCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DefaultAvatarCollectionService — 虚拟人收集与养成实现。
 * GRW-004: ≥10个基础虚拟人，图鉴系统。
 */
@Component
public class DefaultAvatarCollectionService implements AvatarCollectionService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAvatarCollectionService.class);

    /** 全部可收集虚拟人类型数 (≥10) */
    private static final int TOTAL_AVAILABLE_TYPES = 12;

    private final AvatarCollectionRepository collectionRepo;

    public DefaultAvatarCollectionService(AvatarCollectionRepository collectionRepo) {
        this.collectionRepo = collectionRepo;
    }

    @Override
    public AvatarCollection getOrCreateCollection(String userId) {
        return collectionRepo.findByUserId(userId)
                .orElseGet(() -> {
                    AvatarCollection collection = new AvatarCollection(
                            UUID.randomUUID().toString(), userId);
                    return collectionRepo.save(collection);
                });
    }

    @Override
    public AvatarCollection.AvatarEntry collectAvatar(String userId, String avatarTypeId,
                                                       String name, AvatarCollection.AvatarRarity rarity,
                                                       AvatarCollection.AvatarElement element) {
        AvatarCollection collection = getOrCreateCollection(userId);
        String avatarId = UUID.randomUUID().toString();
        AvatarCollection.AvatarEntry entry = collection.collectAvatar(
                avatarId, avatarTypeId, name, rarity, element);
        collectionRepo.save(collection);
        log.info("GRW-004 avatar collected: user={} type={} name={} rarity={}",
                userId, avatarTypeId, name, rarity);
        return entry;
    }

    @Override
    public AvatarCollection.AvatarEntry addAvatarExperience(String userId, String avatarTypeId, int amount) {
        AvatarCollection collection = getOrCreateCollection(userId);
        collection.upgradeAvatar(avatarTypeId, amount);
        collectionRepo.save(collection);
        return collection.getAvatars().get(avatarTypeId);
    }

    @Override
    public AvatarCollection.AvatarEntry addAvatarAffection(String userId, String avatarTypeId, int amount) {
        AvatarCollection collection = getOrCreateCollection(userId);
        collection.increaseAffection(avatarTypeId, amount);
        collectionRepo.save(collection);
        return collection.getAvatars().get(avatarTypeId);
    }

    @Override
    public void setFavoriteAvatar(String userId, String avatarTypeId) {
        AvatarCollection collection = getOrCreateCollection(userId);
        collection.setFavorite(avatarTypeId);
        collectionRepo.save(collection);
        log.info("GRW-004 favorite avatar set: user={} type={}", userId, avatarTypeId);
    }

    @Override
    public CollectionProgress getCollectionProgress(String userId) {
        AvatarCollection collection = getOrCreateCollection(userId);
        double completion = collection.getCollectionCompletion(TOTAL_AVAILABLE_TYPES);
        Map<AvatarCollection.AvatarRarity, Long> rarityDist = collection.getRarityDistribution();
        Map<AvatarCollection.AvatarElement, Long> elementDist = collection.getElementDistribution();

        return new CollectionProgress(userId, collection.getUsedSlots(),
                TOTAL_AVAILABLE_TYPES, completion, rarityDist, elementDist,
                collection.getTotalSlots(), collection.getUsedSlots());
    }

    @Override
    public List<AvatarCollection.AvatarEntry> getUserAvatars(String userId) {
        AvatarCollection collection = getOrCreateCollection(userId);
        return new ArrayList<>(collection.getAvatars().values());
    }

    @Override
    public Optional<AvatarCollection.AvatarEntry> getHighestAffectionAvatar(String userId) {
        AvatarCollection collection = getOrCreateCollection(userId);
        return collection.getHighestAffection();
    }

    @Override
    public Optional<AvatarCollection.AvatarEntry> getHighestLevelAvatar(String userId) {
        AvatarCollection collection = getOrCreateCollection(userId);
        return collection.getHighestLevel();
    }

    @Override
    public AvatarCollection expandSlots(String userId, int additionalSlots) {
        AvatarCollection collection = getOrCreateCollection(userId);
        collection.expandSlots(additionalSlots);
        return collectionRepo.save(collection);
    }
}
