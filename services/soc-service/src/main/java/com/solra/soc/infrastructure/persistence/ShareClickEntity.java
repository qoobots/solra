package com.solra.soc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * ShareClick JPA 实体 — 分享点击记录持久化映射。
 */
@Entity
@Table(name = "share_clicks")
public class ShareClickEntity {

    @Id
    @Column(name = "click_id", length = 64)
    private String clickId;

    @Column(name = "share_id", length = 64, nullable = false)
    private String shareId;

    @Column(name = "visitor_user_id", length = 64)
    private String visitorUserId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "platform", length = 20)
    private String platform;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "converted")
    private boolean converted;

    public ShareClickEntity() {}

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
