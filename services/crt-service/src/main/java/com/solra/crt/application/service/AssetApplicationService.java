package com.solra.crt.application.service;

import com.solra.crt.application.dto.AssetDTO;
import com.solra.crt.application.dto.PageResult;
import com.solra.crt.domain.entity.Asset;
import com.solra.crt.domain.entity.AssetMeta;
import com.solra.crt.domain.repository.AssetRepository;
import com.solra.crt.domain.service.AssetDomainService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 资产应用服务。
 */
public class AssetApplicationService {

    private final AssetRepository assetRepository;
    private final AssetDomainService assetDomainService;

    public AssetApplicationService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
        this.assetDomainService = new AssetDomainService(assetRepository);
    }

    public AssetDTO uploadAsset(String spaceId, String ownerId, String name,
                                 String type, String description) {
        Asset.AssetType assetType = Asset.AssetType.valueOf(type);
        Asset asset = assetDomainService.uploadAsset(
                UUID.randomUUID().toString(), spaceId, ownerId, name, assetType, description);
        return AssetDTO.from(asset);
    }

    public AssetDTO getAsset(String assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        return AssetDTO.from(asset);
    }

    public PageResult<AssetDTO> listAssets(String spaceId, String typeFilter,
                                            int page, int pageSize) {
        Asset.AssetType type = typeFilter != null ? Asset.AssetType.valueOf(typeFilter) : null;
        int offset = (page - 1) * pageSize;

        List<Asset> assets = assetRepository.findBySpaceIdAndType(spaceId, type, offset, pageSize);
        long total = assetRepository.countBySpaceIdAndType(spaceId, type);

        List<AssetDTO> dtos = assets.stream()
                .map(AssetDTO::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtos, page, pageSize, total);
    }

    public void deleteAsset(String assetId) {
        assetRepository.deleteById(assetId);
    }

    public AssetDTO publishAsset(String assetId) {
        Asset asset = assetDomainService.publishAsset(assetId);
        return AssetDTO.from(asset);
    }
}
