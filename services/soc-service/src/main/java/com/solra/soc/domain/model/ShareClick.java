package com.solra.soc.domain.model;

import java.time.Instant;

/**
 * ShareClick 值对象 — 表示一次分享点击事件。
 * 跟踪访问者信息、平台来源以及是否发生转化。
 */
public class ShareClick {

    private String clickId;
    private String shareId;
    private String visitorUserId;
    private String ipAddress;
    private String userAgent;
    private String platform;
    private Instant timestamp;
    private boolean converted;

    public ShareClick() {}

    /**
     * 创建一个新的分享点击记录。
     *
     * @param clickId       点击唯一标识
     * @param shareId       关联的分享会话ID
     * @param visitorUserId 访客用户ID（可为空，表示未登录访客）
     * @param ipAddress     访客IP地址
     * @param userAgent     浏览器/客户端 User-Agent
     * @param platform      平台标识（如 WEB/IOS/ANDROID）
     */
    public ShareClick(String clickId, String shareId, String visitorUserId,
                      String ipAddress, String userAgent, String platform) {
        this.clickId = clickId;
        this.shareId = shareId;
        this.visitorUserId = visitorUserId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.platform = platform;
        this.timestamp = Instant.now();
        this.converted = false;
    }

    /** 标记此点击已发生转化（注册/加入空间等）。 */
    public void markConverted() {
        this.converted = true;
    }

    // ---- getters / setters ----

    public String getClickId() { return clickId; }
    public void setClickId(String clickId) { this.clickId = clickId; }

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    public String getVisitorUserId() { return visitorUserId; }
    public void setVisitorUserId(String visitorUserId) { this.visitorUserId = visitorUserId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public boolean isConverted() { return converted; }
    public void setConverted(boolean converted) { this.converted = converted; }
}
