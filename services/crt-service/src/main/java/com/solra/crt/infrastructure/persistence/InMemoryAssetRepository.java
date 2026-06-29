package com.solra.crt.infrastructure.persistence;

import com.solra.crt.domain.entity.Asset;
import com.solra.crt.domain.repository.AssetRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 资产仓储内存实现（开发阶段，后续替换为 JPA/PostgreSQL）。
 */
public class InMemoryAssetRepository implements AssetRepository {

    private final Map<String, Asset> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Asset> findById(String assetId) {
        return Optional.ofNullable(store.get(assetId));
    }

    @Override
    public List<Asset> findBySpaceId(String spaceId) {
        return store.values().stream()
                .filter(a -> spaceId.equals(a.getSpaceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Asset> findBySpaceIdAndType(String spaceId, Asset.AssetType type,
                                             int offset, int limit) {
        return store.values().stream()
                .filter(a -> spaceId.equals(a.getSpaceId()))
                .filter(a -> type == null || a.getType() == type)
                .sorted(Comparator.comparing(Asset::getUpdatedAt).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long countBySpaceIdAndType(String spaceId, Asset.AssetType type) {
        return store.values().stream()
                .filter(a -> spaceId.equals(a.getSpaceId()))
                .filter(a -> type == null || a.getType() == type)
                .count();
    }

    @Override
    public Asset save(Asset asset) {
        store.put(asset.getAssetId(), asset);
        return asset;
    }

    @Override
    public void deleteById(String assetId) {
        store.remove(assetId);
    }
}
