package com.solra.crt.domain.entity;

/**
 * 资产元数据值对象。
 * 包含文件格式、大小、哈希、CDN 地址等描述信息。
 */
public class AssetMeta {

    private String fileFormat;
    private long fileSizeBytes;
    private String fileHash;
    private String cdnUrl;
    private String thumbnailUrl;
    private int triangleCount;
    private int vertexCount;

    public AssetMeta() {}

    public AssetMeta(String fileFormat, long fileSizeBytes, String fileHash,
                     String cdnUrl, String thumbnailUrl) {
        this.fileFormat = fileFormat;
        this.fileSizeBytes = fileSizeBytes;
        this.fileHash = fileHash;
        this.cdnUrl = cdnUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public int getTriangleCount() { return triangleCount; }
    public void setTriangleCount(int triangleCount) { this.triangleCount = triangleCount; }
    public int getVertexCount() { return vertexCount; }
    public void setVertexCount(int vertexCount) { this.vertexCount = vertexCount; }
}
