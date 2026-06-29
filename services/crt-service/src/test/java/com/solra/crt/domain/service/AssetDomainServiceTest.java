package com.solra.crt.domain.service;

import com.solra.crt.domain.entity.Asset;
import com.solra.crt.domain.entity.AssetMeta;
import com.solra.crt.domain.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AssetDomainService 单元测试。
 * CRT-001 资产上传/发布/归档业务逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AssetDomainService — 资产领域服务测试")
class AssetDomainServiceTest {

    @Mock private AssetRepository assetRepository;

    private AssetDomainService service;

    @BeforeEach
    void setUp() {
        service = new AssetDomainService(assetRepository);
    }

    @Nested
    @DisplayName("uploadAsset — 上传资产")
    class UploadAsset {

        @Test
        @DisplayName("正常上传3D模型资产")
        void uploadModel3D() {
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset asset = service.uploadAsset("asset-1", "space-1", "user-1",
                    "Dragon Model", Asset.AssetType.MODEL_3D, "A dragon 3D model");

            assertNotNull(asset);
            assertEquals("asset-1", asset.getAssetId());
            assertEquals("space-1", asset.getSpaceId());
            assertEquals("user-1", asset.getOwnerId());
            assertEquals(Asset.AssetType.MODEL_3D, asset.getType());
            assertEquals("Dragon Model", asset.getName());
            assertEquals("A dragon 3D model", asset.getDescription());
            assertEquals(Asset.AssetStatus.DRAFT, asset.getStatus());
            assertEquals(1, asset.getVersion());
        }

        @Test
        @DisplayName("上传纹理资产")
        void uploadTexture() {
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset asset = service.uploadAsset("asset-2", "space-1", "user-1",
                    "Wood Texture", Asset.AssetType.TEXTURE, "Oak wood texture");

            assertEquals(Asset.AssetType.TEXTURE, asset.getType());
        }

        @Test
        @DisplayName("上传音频资产")
        void uploadAudio() {
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset asset = service.uploadAsset("asset-3", "space-1", "user-1",
                    "BGM Track", Asset.AssetType.AUDIO, "Background music");

            assertEquals(Asset.AssetType.AUDIO, asset.getType());
        }

        @Test
        @DisplayName("上传后调用 repository.save")
        void callsRepositorySave() {
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            service.uploadAsset("asset-1", "space-1", "user-1",
                    "Test", Asset.AssetType.SCENE, "desc");

            verify(assetRepository, times(1)).save(any(Asset.class));
        }
    }

    @Nested
    @DisplayName("updateAssetMeta — 更新元数据")
    class UpdateAssetMeta {

        @Test
        @DisplayName("正常更新元数据")
        void updateMetaSuccess() {
            Asset asset = new Asset("asset-1", "space-1", "user-1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc", null);
            AssetMeta meta = new AssetMeta();
            meta.setFileFormat("glb");
            meta.setFileSizeBytes(1024000L);
            meta.setFileHash("sha256:abc123");

            when(assetRepository.findById("asset-1")).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset updated = service.updateAssetMeta("asset-1", meta);

            assertNotNull(updated.getMeta());
            assertEquals("glb", updated.getMeta().getFileFormat());
            assertEquals(1024000L, updated.getMeta().getFileSizeBytes());
            assertEquals(2, updated.getVersion()); // version should increment
        }

        @Test
        @DisplayName("资产不存在时抛 IllegalArgumentException")
        void assetNotFoundThrows() {
            AssetMeta meta = new AssetMeta();
            when(assetRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.updateAssetMeta("non-existent", meta));
            verify(assetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("publishAsset — 发布资产")
    class PublishAsset {

        @Test
        @DisplayName("DRAFT 状态可发布")
        void publishDraftAsset() {
            Asset asset = new Asset("asset-1", "space-1", "user-1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc", Asset.AssetStatus.DRAFT);

            when(assetRepository.findById("asset-1")).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset published = service.publishAsset("asset-1");

            assertEquals(Asset.AssetStatus.PUBLISHED, published.getStatus());
        }

        @Test
        @DisplayName("非DRAFT状态发布不生效")
        void publishNonDraftNoEffect() {
            Asset asset = new Asset("asset-1", "space-1", "user-1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc", Asset.AssetStatus.ARCHIVED);

            when(assetRepository.findById("asset-1")).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset result = service.publishAsset("asset-1");

            // publish() 守卫: 仅 DRAFT 可变 PUBLISHED
            assertEquals(Asset.AssetStatus.ARCHIVED, result.getStatus());
        }

        @Test
        @DisplayName("资产不存在时抛异常")
        void assetNotFoundThrows() {
            when(assetRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.publishAsset("non-existent"));
        }
    }

    @Nested
    @DisplayName("archiveAsset — 归档资产")
    class ArchiveAsset {

        @Test
        @DisplayName("任意状态可归档")
        void anyStatusCanArchive() {
            Asset asset = new Asset("asset-1", "space-1", "user-1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc", Asset.AssetStatus.PUBLISHED);

            when(assetRepository.findById("asset-1")).thenReturn(Optional.of(asset));
            when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

            Asset archived = service.archiveAsset("asset-1");

            assertEquals(Asset.AssetStatus.ARCHIVED, archived.getStatus());
        }

        @Test
        @DisplayName("资产不存在时抛异常")
        void assetNotFoundThrows() {
            when(assetRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.archiveAsset("non-existent"));
        }
    }
}
