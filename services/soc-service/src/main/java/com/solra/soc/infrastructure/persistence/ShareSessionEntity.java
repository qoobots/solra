package com.solra.soc.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * ShareSession JPA 实体 — 分享会话持久化映射。
 */
@Entity
@Table(name = "share_sessions")
public class ShareSessionEntity {

    @Id
    @Column(name = "share_id", length = 64)
    private String shareId;

    @Column(name = "space_id", length = 64, nullable = false)
    private String spaceId;

    @Column(name = "sharer_user_id", length = 64, nullable = false)
    private String sharerUserId;

    @Column(name = "share_type", length = 20, nullable = false)
    private String shareType;

    @Column(name = "share_code", length = 32, unique = true, nullable = false)
    private String shareCode;

    @Column(name = "click_count")
    private long clickCount;

    @Column(name = "conversion_count")
    private long conversionCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "viral_chain", columnDefinition = "TEXT")
    private String viralChain; // JSON array stored as string

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    public ShareSessionEntity() {}

    // getters / setters

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getSharerUserId() { return sharerUserId; }
    public void setSharerUserId(String sharerUserId) { this.sharerUserId = sharerUserId; }

    public String getShareType() { return shareType; }
    public void setShareType(String shareType) { this.shareType = shareType; }

    public String getShareCode() { return shareCode; }
    public void setShareCode(String shareCode) { this.shareCode = shareCode; }

    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }

    public long getConversionCount() { return conversionCount; }
    public void setConversionCount(long conversionCount) { this.conversionCount = conversionCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getViralChain() { return viralChain; }
    public void setViralChain(String viralChain) { this.viralChain = viralChain; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
