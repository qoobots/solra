package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.Asset;
import com.solra.crt.domain.entity.AssetMeta;
import com.solra.crt.domain.repository.AssetRepository;

/**
 * 资产领域服务。
 * 封装资产上传、发布、归档等核心业务逻辑。
 */
public class AssetDomainService {

    private final AssetRepository assetRepository;

    public AssetDomainService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * 上传资产。
     */
    public Asset uploadAsset(String assetId, String spaceId, String ownerId,
                              String name, Asset.AssetType type, String description) {
        Asset asset = new Asset(assetId, spaceId, ownerId, type, name, description, null);
        return assetRepository.save(asset);
    }

    /**
     * 更新资产元数据（文件上传完成后）。
     */
    public Asset updateAssetMeta(String assetId, AssetMeta meta) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        asset.updateMeta(meta);
        return assetRepository.save(asset);
    }

    /**
     * 发布资产。
     */
    public Asset publishAsset(String assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        asset.publish();
        return assetRepository.save(asset);
    }

    /**
     * 归档资产。
     */
    public Asset archiveAsset(String assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        asset.archive();
        return assetRepository.save(asset);
    }
}
