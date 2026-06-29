package com.solra.spc.domain.model;

/**
 * SpaceAsset 值对象 — 空间资产（3D模型/纹理/音频）。
 */
public class SpaceAsset {
    private String assetId;
    private String assetType; // model / texture / audio / script
    private String url;
    private long sizeBytes;
    private AssetFormat format;
    private int lodLevel = 0;

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public AssetFormat getFormat() { return format; }
    public void setFormat(AssetFormat format) { this.format = format; }
    public int getLodLevel() { return lodLevel; }
    public void setLodLevel(int lodLevel) { this.lodLevel = lodLevel; }
}
