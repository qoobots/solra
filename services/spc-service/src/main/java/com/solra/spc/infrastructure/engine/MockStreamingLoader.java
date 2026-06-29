package com.solra.spc.infrastructure.engine;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.service.StreamingLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * MockStreamingLoader — 模拟流式加载引擎。
 * 在 core/ C++ 渲染/流式引擎就绪前提供基础功能。
 */
@Component
public class MockStreamingLoader implements StreamingLoader {

    private static final Logger log = LoggerFactory.getLogger(MockStreamingLoader.class);

    @Override
    public long estimateLoadTimeMs(String spaceId, String networkType) {
        return switch (networkType.toUpperCase()) {
            case "5G" -> 1800L;  // <2s
            case "4G" -> 4500L;  // <5s
            case "WIFI" -> 1200L;
            default -> 3000L;
        };
    }

    @Override
    public List<String> prioritizeAssets(Space space) {
        if (space.getContent() == null || space.getContent().getAssets() == null) return List.of();
        return space.getContent().getAssets().stream()
                .sorted(Comparator.comparingInt(SpaceAsset::getLodLevel))
                .map(SpaceAsset::getAssetId)
                .toList();
    }

    @Override
    public Flow.Publisher<AssetChunk> streamAssets(String spaceId, List<String> assetIds, StreamConfig config) {
        SubmissionPublisher<AssetChunk> publisher = new SubmissionPublisher<>();
        List<String> ids = assetIds != null ? assetIds : List.of("mock-asset-1", "mock-asset-2");

        Thread.ofVirtual().start(() -> {
            try {
                for (String assetId : ids) {
                    int totalChunks = 3;
                    for (int i = 0; i < totalChunks; i++) {
                        boolean isFinal = (i == totalChunks - 1);
                        AssetChunk chunk = new AssetChunk();
                        chunk.setAssetId(assetId);
                        chunk.setChunkIndex(i);
                        chunk.setTotalChunks(totalChunks);
                        chunk.setData(("MOCK_CHUNK_" + assetId + "_" + i).getBytes());
                        chunk.setFinal(isFinal);
                        chunk.setCompressionLevel(config.enableCompression() ? 3 : 0);
                        publisher.submit(chunk);
                        Thread.sleep(30); // 模拟网络延迟
                    }
                }
                publisher.close();
            } catch (Exception e) {
                publisher.closeExceptionally(e);
            }
        });

        log.info("MockStreamingLoader: started streaming space={} assets={}", spaceId, ids.size());
        return publisher;
    }

    @Override
    public List<AssetChunk> getInitialChunks(String spaceId) {
        AssetChunk chunk = new AssetChunk();
        chunk.setAssetId("init-scene");
        chunk.setChunkIndex(0);
        chunk.setTotalChunks(1);
        chunk.setData(("MOCK_INIT_" + spaceId).getBytes());
        chunk.setFinal(true);
        chunk.setCompressionLevel(0);
        return List.of(chunk);
    }
}
