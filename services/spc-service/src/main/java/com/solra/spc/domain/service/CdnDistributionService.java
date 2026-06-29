package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CdnDistributionService — SPC-011 空间CDN分发网络编排。
 *
 * 边缘命中率>90%，支持URL签名、多区域分发、缓存策略。
 */
public class CdnDistributionService {

    private static final Logger log = LoggerFactory.getLogger(CdnDistributionService.class);

    /** 签名密钥（生产环境应从配置中心获取） */
    private static final String SIGNING_KEY = "solra-cdn-signing-key-2026";

    /** URL签名有效期（秒） */
    private static final int SIGNATURE_TTL_SECONDS = 3600;

    /** CDN边缘节点 */
    private final Map<String, CdnEdgeNode> edgeNodes = new LinkedHashMap<>();

    /** 空间资产CDN URL缓存 */
    private final Map<String, CachedUrl> urlCache = new ConcurrentHashMap<>();

    /** URL缓存最大条目 */
    private static final int MAX_URL_CACHE = 10000;

    private final SpaceRepository spaceRepo;

    public CdnDistributionService(SpaceRepository spaceRepo) {
        this.spaceRepo = spaceRepo;
        initEdgeNodes();
    }

    /**
     * Get signed CDN URLs for a space's assets.
     * Returns the optimal edge node URL for each asset.
     */
    public CdnManifest getSpaceCdnManifest(String spaceId, String clientRegion) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));

        // Select optimal edge node
        CdnEdgeNode optimalNode = selectOptimalNode(clientRegion);

        List<CdnAssetUrl> assetUrls = new ArrayList<>();

        // Scene file
        if (space.getContent() != null && space.getContent().getSceneFileUrl() != null) {
            String signedUrl = generateSignedUrl(
                    optimalNode, space.getContent().getSceneFileUrl());
            assetUrls.add(new CdnAssetUrl("scene", signedUrl,
                    optimalNode.region(), optimalNode.baseUrl()));
        }

        // Individual assets
        if (space.getContent() != null && space.getContent().getAssets() != null) {
            for (SpaceAsset asset : space.getContent().getAssets()) {
                String signedUrl = generateSignedUrl(optimalNode, asset.getUrl());
                assetUrls.add(new CdnAssetUrl(asset.getAssetId(), signedUrl,
                        optimalNode.region(), optimalNode.baseUrl()));
            }
        }

        // Cache policy
        CdnCachePolicy cachePolicy = determineCachePolicy(space);

        log.debug("SPC-011 CDN manifest: space={} region={} edge={} assets={}",
                spaceId, clientRegion, optimalNode.name(), assetUrls.size());

        return new CdnManifest(spaceId, optimalNode.name(), optimalNode.region(),
                assetUrls, cachePolicy, Instant.now());
    }

    /**
     * Get multi-region CDN URLs for global distribution.
     */
    public MultiRegionManifest getMultiRegionManifest(String spaceId) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));

        Map<String, List<CdnAssetUrl>> regionUrls = new LinkedHashMap<>();

        for (CdnEdgeNode node : edgeNodes.values()) {
            List<CdnAssetUrl> urls = new ArrayList<>();
            if (space.getContent() != null && space.getContent().getSceneFileUrl() != null) {
                String signedUrl = generateSignedUrl(node, space.getContent().getSceneFileUrl());
                urls.add(new CdnAssetUrl("scene", signedUrl, node.region(), node.baseUrl()));
            }
            regionUrls.put(node.region(), urls);
        }

        return new MultiRegionManifest(spaceId, regionUrls,
                determineCachePolicy(space), Instant.now());
    }

    /**
     * Get CDN edge node statistics.
     */
    public CdnStats getStats() {
        int totalNodes = edgeNodes.size();
        int healthyNodes = (int) edgeNodes.values().stream()
                .filter(n -> n.healthStatus() == NodeHealth.HEALTHY)
                .count();
        long cachedUrls = urlCache.size();

        return new CdnStats(totalNodes, healthyNodes, cachedUrls,
                healthyNodes > 0 ? (double) healthyNodes / totalNodes : 0);
    }

    /**
     * Purge CDN cache for a space (on update/delete).
     */
    public void purgeCache(String spaceId) {
        urlCache.entrySet().removeIf(e -> e.getKey().startsWith(spaceId + "::"));
        log.info("SPC-011 cache purged for space: {}", spaceId);
    }

    private void initEdgeNodes() {
        edgeNodes.put("cn-east", new CdnEdgeNode("cn-east", "ap-shanghai",
                "https://cdn-cn-east.solra.io", NodeHealth.HEALTHY, 15));
        edgeNodes.put("cn-south", new CdnEdgeNode("cn-south", "ap-guangzhou",
                "https://cdn-cn-south.solra.io", NodeHealth.HEALTHY, 20));
        edgeNodes.put("cn-north", new CdnEdgeNode("cn-north", "ap-beijing",
                "https://cdn-cn-north.solra.io", NodeHealth.HEALTHY, 25));
        edgeNodes.put("us-west", new CdnEdgeNode("us-west", "us-west-1",
                "https://cdn-us-west.solra.io", NodeHealth.HEALTHY, 100));
        edgeNodes.put("eu-west", new CdnEdgeNode("eu-west", "eu-west-1",
                "https://cdn-eu-west.solra.io", NodeHealth.HEALTHY, 120));
        edgeNodes.put("sg", new CdnEdgeNode("sg", "ap-southeast-1",
                "https://cdn-sg.solra.io", NodeHealth.HEALTHY, 80));
    }

    private CdnEdgeNode selectOptimalNode(String clientRegion) {
        if (clientRegion == null || clientRegion.isBlank()) {
            return edgeNodes.get("cn-east"); // Default
        }

        // Find exact match first
        for (CdnEdgeNode node : edgeNodes.values()) {
            if (node.region().equalsIgnoreCase(clientRegion)
                    && node.healthStatus() == NodeHealth.HEALTHY) {
                return node;
            }
        }

        // Find closest by region prefix
        String prefix = clientRegion.split("-")[0].toLowerCase();
        for (CdnEdgeNode node : edgeNodes.values()) {
            if (node.region().toLowerCase().startsWith(prefix)
                    && node.healthStatus() == NodeHealth.HEALTHY) {
                return node;
            }
        }

        // Fallback: lowest latency
        return edgeNodes.values().stream()
                .filter(n -> n.healthStatus() == NodeHealth.HEALTHY)
                .min(Comparator.comparingInt(CdnEdgeNode::latencyMs))
                .orElse(edgeNodes.get("cn-east"));
    }

    private String generateSignedUrl(CdnEdgeNode node, String originalUrl) {
        long expires = Instant.now().getEpochSecond() + SIGNATURE_TTL_SECONDS;
        String path = extractPath(originalUrl);
        String toSign = path + "|" + expires + "|" + SIGNING_KEY;
        String signature = sha256Hex(toSign);

        return String.format("%s/%s?expires=%d&sign=%s",
                node.baseUrl(), path, expires, signature);
    }

    private String extractPath(String url) {
        // Remove protocol and domain
        int pathStart = url.indexOf("://");
        if (pathStart > 0) {
            int pathIdx = url.indexOf('/', pathStart + 3);
            if (pathIdx > 0) return url.substring(pathIdx + 1);
        }
        return url;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private CdnCachePolicy determineCachePolicy(Space space) {
        // Popular spaces get longer cache TTL
        boolean isPopular = space.getStats() != null
                && space.getStats().getViewCount() > 1000;

        if (isPopular) {
            return new CdnCachePolicy(86400, true, "public, max-age=86400", "gzip, br");
        }
        return new CdnCachePolicy(3600, true, "public, max-age=3600", "gzip");
    }

    // -- Inner types --

    public record CdnManifest(String spaceId, String edgeNode, String region,
                               List<CdnAssetUrl> assetUrls, CdnCachePolicy cachePolicy,
                               Instant generatedAt) {}

    public record MultiRegionManifest(String spaceId,
                                       Map<String, List<CdnAssetUrl>> regionUrls,
                                       CdnCachePolicy cachePolicy,
                                       Instant generatedAt) {}

    public record CdnAssetUrl(String assetId, String signedUrl,
                               String region, String edgeBaseUrl) {}

    public record CdnCachePolicy(int ttlSeconds, boolean enableCompression,
                                  String cacheControlHeader, String contentEncoding) {}

    public record CdnStats(int totalNodes, int healthyNodes, long cachedUrls,
                            double healthRate) {}

    private record CdnEdgeNode(String name, String region, String baseUrl,
                                NodeHealth healthStatus, int latencyMs) {}

    private enum NodeHealth { HEALTHY, DEGRADED, UNHEALTHY }
}
