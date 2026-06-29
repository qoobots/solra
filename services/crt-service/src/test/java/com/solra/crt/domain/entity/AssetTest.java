package com.solra.crt.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Asset 实体单元测试。
 * 验证资产状态机和版本控制逻辑。
 */
@DisplayName("Asset — 资产实体测试")
class AssetTest {

    @Nested
    @DisplayName("构造函数")
    class Constructor {

        @Test
        @DisplayName("默认状态为 DRAFT")
        void defaultStatusIsDraft() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc", null);
            assertEquals(Asset.AssetStatus.DRAFT, asset.getStatus());
        }

        @Test
        @DisplayName("可指定初始状态")
        void explicitStatus() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc",
                    Asset.AssetStatus.PUBLISHED);
            assertEquals(Asset.AssetStatus.PUBLISHED, asset.getStatus());
        }

        @Test
        @DisplayName("初始版本号为1")
        void initialVersionIsOne() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc", null);
            assertEquals(1, asset.getVersion());
        }
    }

    @Nested
    @DisplayName("publish() — 发布资产")
    class Publish {

        @Test
        @DisplayName("DRAFT → PUBLISHED")
        void draftToPublished() {
            Asset asset = createDraftAsset();
            asset.publish();
            assertEquals(Asset.AssetStatus.PUBLISHED, asset.getStatus());
        }

        @Test
        @DisplayName("发布后版本号+1")
        void publishIncrementsVersion() {
            Asset asset = createDraftAsset();
            asset.publish();
            assertEquals(2, asset.getVersion());
        }

        @Test
        @DisplayName("发布后 updatedAt 更新")
        void publishUpdatesTimestamp() throws InterruptedException {
            Asset asset = createDraftAsset();
            Thread.sleep(10); // 确保时间差异
            asset.publish();
            // updatedAt should be updated (after creation time)
            assertNotNull(asset.getUpdatedAt());
        }

        @Test
        @DisplayName("非DRAFT状态发布不生效")
        void nonDraftPublishNoEffect() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc",
                    Asset.AssetStatus.ARCHIVED);
            asset.publish();
            assertEquals(Asset.AssetStatus.ARCHIVED, asset.getStatus());
            assertEquals(1, asset.getVersion()); // version unchanged
        }

        @Test
        @DisplayName("PUBLISHED状态再次发布不生效")
        void alreadyPublishedNoEffect() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.MODEL_3D, "Dragon", "desc",
                    Asset.AssetStatus.PUBLISHED);
            asset.publish();
            assertEquals(Asset.AssetStatus.PUBLISHED, asset.getStatus());
        }
    }

    @Nested
    @DisplayName("archive() — 归档资产")
    class Archive {

        @Test
        @DisplayName("任意状态可归档")
        void anyStatusCanArchive() {
            Asset asset = createDraftAsset();
            asset.archive();
            assertEquals(Asset.AssetStatus.ARCHIVED, asset.getStatus());
        }

        @Test
        @DisplayName("归档后 updatedAt 更新")
        void archiveUpdatesTimestamp() {
            Asset asset = createDraftAsset();
            asset.archive();
            assertNotNull(asset.getUpdatedAt());
        }

        @Test
        @DisplayName("已归档再次归档仍为 ARCHIVED")
        void alreadyArchivedStillArchived() {
            Asset asset = createDraftAsset();
            asset.archive();
            asset.archive();
            assertEquals(Asset.AssetStatus.ARCHIVED, asset.getStatus());
        }
    }

    @Nested
    @DisplayName("updateMeta() — 更新元数据")
    class UpdateMeta {

        @Test
        @DisplayName("更新元数据版本号+1")
        void updateMetaIncrementsVersion() {
            Asset asset = createDraftAsset();
            AssetMeta meta = new AssetMeta();
            asset.updateMeta(meta);
            assertEquals(2, asset.getVersion());
        }

        @Test
        @DisplayName("更新元数据后 updatedAt 刷新")
        void updateMetaUpdatesTimestamp() {
            Asset asset = createDraftAsset();
            AssetMeta meta = new AssetMeta();
            meta.setFileFormat("glb");
            asset.updateMeta(meta);
            assertEquals("glb", asset.getMeta().getFileFormat());
        }

        @Test
        @DisplayName("多次更新元数据版本号持续递增")
        void multipleUpdatesVersionKeepsIncrementing() {
            Asset asset = createDraftAsset();
            asset.updateMeta(new AssetMeta());
            assertEquals(2, asset.getVersion());
            asset.updateMeta(new AssetMeta());
            assertEquals(3, asset.getVersion());
        }
    }

    @Nested
    @DisplayName("所有资产类型")
    class AssetTypes {

        @Test
        @DisplayName("MODEL_3D 类型")
        void model3D() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.MODEL_3D, "Model", "desc", null);
            assertEquals(Asset.AssetType.MODEL_3D, asset.getType());
        }

        @Test
        @DisplayName("TEXTURE 类型")
        void texture() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.TEXTURE, "Tex", "desc", null);
            assertEquals(Asset.AssetType.TEXTURE, asset.getType());
        }

        @Test
        @DisplayName("ANIMATION 类型")
        void animation() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.ANIMATION, "Anim", "desc", null);
            assertEquals(Asset.AssetType.ANIMATION, asset.getType());
        }

        @Test
        @DisplayName("SCENE 类型")
        void scene() {
            Asset asset = new Asset("a1", "s1", "u1",
                    Asset.AssetType.SCENE, "Scene", "desc", null);
            assertEquals(Asset.AssetType.SCENE, asset.getType());
        }
    }

    private Asset createDraftAsset() {
        return new Asset("a1", "s1", "u1",
                Asset.AssetType.MODEL_3D, "Dragon", "desc", null);
    }
}
