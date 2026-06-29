package com.solra.crt.application.service;

import com.solra.crt.application.dto.AssetDTO;
import com.solra.crt.application.dto.PageResult;
import com.solra.crt.domain.entity.Asset;
import com.solra.crt.domain.entity.AssetMeta;
import com.solra.crt.domain.repository.AssetRepository;
import com.solra.crt.domain.service.AssetDomainService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产应用服务。
 * 包含 CRT-005 空间资产库管理功能。
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

    // ── CRT-005: 空间资产库管理 ──

    /**
     * 更新资产元数据。
     */
    public AssetDTO updateAssetMeta(String assetId, AssetMeta meta) {
        Asset asset = assetDomainService.updateAssetMeta(assetId, meta);
        return AssetDTO.from(asset);
    }

    /**
     * 归档资产。
     */
    public AssetDTO archiveAsset(String assetId) {
        Asset asset = assetDomainService.archiveAsset(assetId);
        return AssetDTO.from(asset);
    }

    /**
     * 为资产添加标签。
     */
    public AssetDTO addTags(String assetId, List<String> tags) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        if (asset.getTags() == null) {
            asset.setTags(new ArrayList<>());
        }
        for (String tag : tags) {
            if (!asset.getTags().contains(tag)) {
                asset.getTags().add(tag);
            }
        }
        asset = assetRepository.save(asset);
        return AssetDTO.from(asset);
    }

    /**
     * 移除资产标签。
     */
    public AssetDTO removeTags(String assetId, List<String> tags) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
        if (asset.getTags() != null) {
            asset.getTags().removeAll(tags);
        }
        asset = assetRepository.save(asset);
        return AssetDTO.from(asset);
    }

    /**
     * 按标签搜索资产。
     */
    public PageResult<AssetDTO> searchByTags(String spaceId, List<String> tags,
                                              int page, int pageSize) {
        List<Asset> allAssets = assetRepository.findBySpaceId(spaceId);
        List<Asset> filtered = allAssets.stream()
                .filter(a -> a.getTags() != null && !Collections.disjoint(a.getTags(), tags))
                .collect(Collectors.toList());

        int offset = (page - 1) * pageSize;
        long total = filtered.size();
        List<Asset> paged = filtered.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(Collectors.toList());

        List<AssetDTO> dtos = paged.stream()
                .map(AssetDTO::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtos, page, pageSize, total);
    }

    /**
     * 获取资产库统计信息。
     */
    public Map<String, Object> getAssetStats(String spaceId) {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<Asset> allAssets = assetRepository.findBySpaceId(spaceId);

        stats.put("totalAssets", allAssets.size());

        // 按类型统计
        Map<String, Long> byType = allAssets.stream()
                .collect(Collectors.groupingBy(a -> a.getType().name(), Collectors.counting()));
        stats.put("byType", byType);

        // 按状态统计
        Map<String, Long> byStatus = allAssets.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
        stats.put("byStatus", byStatus);

        // 总文件大小
        long totalSize = allAssets.stream()
                .filter(a -> a.getMeta() != null)
                .mapToLong(a -> a.getMeta().getFileSizeBytes())
                .sum();
        stats.put("totalSizeBytes", totalSize);

        return stats;
    }

    /**
     * 批量删除资产。
     */
    public int batchDelete(List<String> assetIds) {
        int deleted = 0;
        for (String id : assetIds) {
            try {
                assetRepository.deleteById(id);
                deleted++;
            } catch (Exception ignored) {
                // 跳过不存在的资产
            }
        }
        return deleted;
    }

    /**
     * 批量发布资产。
     */
    public int batchPublish(List<String> assetIds) {
        int published = 0;
        for (String id : assetIds) {
            try {
                assetDomainService.publishAsset(id);
                published++;
            } catch (Exception ignored) {
                // 跳过无法发布的资产
            }
        }
        return published;
    }
}
