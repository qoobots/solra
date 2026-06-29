package com.solra.crt.domain.repository;

import com.solra.crt.domain.entity.Asset;
import java.util.List;
import java.util.Optional;

/**
 * 资产仓储接口（领域层端口）。
 */
public interface AssetRepository {

    Optional<Asset> findById(String assetId);

    List<Asset> findBySpaceId(String spaceId);

    List<Asset> findBySpaceIdAndType(String spaceId, Asset.AssetType type,
                                      int offset, int limit);

    long countBySpaceIdAndType(String spaceId, Asset.AssetType type);

    Asset save(Asset asset);

    void deleteById(String assetId);
}
