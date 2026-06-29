package com.solra.avt.domain.model;

/**
 * MessageAttachment 值对象 — 对话消息附件。
 */
public class MessageAttachment {
    private String attachmentType;
    private String url;
    private String mimeType;
    private long sizeBytes;

    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
}
