package com.solra.spc.domain.model;

import java.util.List;

/**
 * AssetChunk 值对象 — 流式加载的资产数据块。
 */
public class AssetChunk {
    private String assetId;
    private int chunkIndex;
    private int totalChunks;
    private byte[] data;
    private boolean isFinal;
    private int compressionLevel;

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public int getTotalChunks() { return totalChunks; }
    public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }
    public boolean isFinal() { return isFinal; }
    public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
    public int getCompressionLevel() { return compressionLevel; }
    public void setCompressionLevel(int compressionLevel) { this.compressionLevel = compressionLevel; }
}
