package com.solra.spc.domain.service;

import com.solra.spc.domain.model.AssetChunk;
import com.solra.spc.domain.model.Space;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * StreamingLoader — 空间流式加载引擎抽象（SPC-001）。
 * 负责将 200MB 3D 空间在 2 秒内分块流式传输给客户端。
 * 具体实现由 core/ C++ StreamingEngine 提供。
 */
public interface StreamingLoader {

    /** 计算预估加载时间（毫秒） */
    long estimateLoadTimeMs(String spaceId, String networkType);

    /** 按优先级排序资产加载顺序（入口场景→LOD0→LOD1→...） */
    List<String> prioritizeAssets(Space space);

    /** 流式加载空间资产 */
    Flow.Publisher<AssetChunk> streamAssets(String spaceId, List<String> assetIds, StreamConfig config);

    /** 获取初始必要资产（首屏渲染最小集） */
    List<AssetChunk> getInitialChunks(String spaceId);

    record StreamConfig(int maxConcurrentChunks, int chunkSizeBytes,
                        List<String> priorityAssets, boolean enableCompression, int lodThreshold) {
        public static StreamConfig defaults() {
            return new StreamConfig(4, 65536, List.of(), true, 50);
        }
    }
}
