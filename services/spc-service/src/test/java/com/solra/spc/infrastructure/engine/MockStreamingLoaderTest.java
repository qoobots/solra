package com.solra.spc.infrastructure.engine;

import com.solra.spc.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MockStreamingLoader.
 */
@DisplayName("MockStreamingLoader")
class MockStreamingLoaderTest {

    private MockStreamingLoader loader;

    @BeforeEach
    void setUp() {
        loader = new MockStreamingLoader();
    }

    @Nested
    @DisplayName("estimateLoadTimeMs")
    class EstimateLoadTimeTests {

        @Test
        @DisplayName("should estimate <2s for 5G")
        void shouldEstimateFastFor5G() {
            long time = loader.estimateLoadTimeMs("spc-001", "5G");
            assertThat(time).isEqualTo(1800L);
            assertThat(time).isLessThan(2000L);
        }

        @Test
        @DisplayName("should estimate <5s for 4G")
        void shouldEstimateFor4G() {
            long time = loader.estimateLoadTimeMs("spc-001", "4G");
            assertThat(time).isEqualTo(4500L);
            assertThat(time).isLessThan(5000L);
        }

        @Test
        @DisplayName("should estimate for WIFI")
        void shouldEstimateForWifi() {
            long time = loader.estimateLoadTimeMs("spc-001", "WIFI");
            assertThat(time).isEqualTo(1200L);
        }

        @Test
        @DisplayName("should default for unknown network")
        void shouldDefaultForUnknown() {
            long time = loader.estimateLoadTimeMs("spc-001", "3G");
            assertThat(time).isEqualTo(3000L);
        }

        @Test
        @DisplayName("should be case insensitive")
        void shouldBeCaseInsensitive() {
            assertThat(loader.estimateLoadTimeMs("spc-001", "wifi")).isEqualTo(1200L);
            assertThat(loader.estimateLoadTimeMs("spc-001", "5g")).isEqualTo(1800L);
        }
    }

    @Nested
    @DisplayName("prioritizeAssets")
    class PrioritizeAssetsTests {

        @Test
        @DisplayName("should return empty for space without content")
        void shouldReturnEmptyForNoContent() {
            Space space = new Space("spc-001", "creator-1");

            List<String> result = loader.prioritizeAssets(space);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should sort assets by LOD level ascending")
        void shouldSortByLodLevel() {
            Space space = new Space("spc-001", "creator-1");
            SpaceContent content = new SpaceContent();

            SpaceAsset lod0 = new SpaceAsset();
            lod0.setAssetId("asset-lod0");
            lod0.setLodLevel(0);

            SpaceAsset lod2 = new SpaceAsset();
            lod2.setAssetId("asset-lod2");
            lod2.setLodLevel(2);

            SpaceAsset lod1 = new SpaceAsset();
            lod1.setAssetId("asset-lod1");
            lod1.setLodLevel(1);

            content.setAssets(List.of(lod2, lod0, lod1));
            space.setContent(content);

            List<String> result = loader.prioritizeAssets(space);

            assertThat(result).containsExactly("asset-lod0", "asset-lod1", "asset-lod2");
        }
    }

    @Nested
    @DisplayName("getInitialChunks")
    class GetInitialChunksTests {

        @Test
        @DisplayName("should return single chunk")
        void shouldReturnSingleChunk() {
            List<AssetChunk> chunks = loader.getInitialChunks("spc-001");

            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0).getAssetId()).isEqualTo("init-scene");
            assertThat(chunks.get(0).isFinal()).isTrue();
            assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
            assertThat(chunks.get(0).getTotalChunks()).isEqualTo(1);
        }

        @Test
        @DisplayName("should contain space ID in mock data")
        void shouldContainSpaceId() {
            List<AssetChunk> chunks = loader.getInitialChunks("custom-space");

            String data = new String(chunks.get(0).getData());
            assertThat(data).contains("custom-space");
        }
    }
}
