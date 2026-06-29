package com.solra.soc.application.dto;

/**
 * 追踪点击命令 — 可变 JavaBean。
 */
public class TrackClickCommand {

    private String shareCode;
    private String visitorUserId;
    private String ipAddress;
    private String userAgent;
    private String platform;

    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }

    public String getVisitorUserId() { return visitorUserId; }
    public void setVisitorUserId(String visitorUserId) { this.visitorUserId = visitorUserId; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
}
