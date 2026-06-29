package com.solra.soc.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ShareSession 聚合根 — 跟踪一次分享链接的完整生命周期。
 * <p>
 * 管理分享链接的创建、点击追踪、转化追踪和病毒传播链路。
 * 支持三种分享类型：SPACE（空间分享）、PROFILE（资料分享）、INVITE（邀请）。
 */
public class ShareSession {

    private String shareId;
    private String spaceId;
    private String sharerUserId;
    private ShareType shareType;
    private String shareCode;
    private long clickCount;
    private long conversionCount;
    private Instant createdAt;
    private Instant expiresAt;
    private List<String> viralChain; // 传播链：userId 列表，从分享者到转化者
    private ShareStatus status;

    public ShareSession() {
        this.viralChain = new ArrayList<>();
    }

    /**
     * 创建一个新的分享会话。
     *
     * @param shareId      分享唯一标识
     * @param spaceId      关联的空间ID
     * @param sharerUserId 分享者用户ID
     * @param shareType    分享类型
     * @param shareCode    生成的分享码
     * @param expiresAt    过期时间（可为 null 表示永不过期）
     */
    public ShareSession(String shareId, String spaceId, String sharerUserId,
                        ShareType shareType, String shareCode, Instant expiresAt) {
        this.shareId = shareId;
        this.spaceId = spaceId;
        this.sharerUserId = sharerUserId;
        this.shareType = shareType;
        this.shareCode = shareCode;
        this.clickCount = 0;
        this.conversionCount = 0;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.viralChain = new ArrayList<>();
        this.viralChain.add(sharerUserId);
        this.status = ShareStatus.ACTIVE;
    }

    // ---- business methods ----

    /** 记录一次点击，传播链追加访问者。 */
    public void recordClick(String visitorUserId) {
        this.clickCount++;
        if (visitorUserId != null && !visitorUserId.isBlank()
                && !this.viralChain.contains(visitorUserId)) {
            this.viralChain.add(visitorUserId);
        }
        checkExpiration();
    }

    /** 记录一次转化（用户注册/加入空间等）。 */
    public void recordConversion(String userId) {
        this.conversionCount++;
        if (userId != null && !userId.isBlank()
                && !this.viralChain.contains(userId)) {
            this.viralChain.add(userId);
        }
    }

    /** 标记分享链接为已过期。 */
    public void expire() {
        if (this.status == ShareStatus.ACTIVE) {
            this.status = ShareStatus.EXPIRED;
        }
    }

    /** 标记分享链接为已消费。 */
    public void consume() {
        if (this.status == ShareStatus.ACTIVE) {
            this.status = ShareStatus.CONSUMED;
        }
    }

    /** 检查是否已过期，若过期自动更新状态。 */
    public boolean isExpired() {
        if (this.expiresAt != null && Instant.now().isAfter(this.expiresAt)) {
            this.status = ShareStatus.EXPIRED;
            return true;
        }
        return this.status == ShareStatus.EXPIRED;
    }

    private void checkExpiration() {
        if (this.expiresAt != null && Instant.now().isAfter(this.expiresAt)) {
            this.status = ShareStatus.EXPIRED;
        }
    }

    // ---- getters / setters ----

    public String getShareId() { return shareId; }
    public void setShareId(String shareId) { this.shareId = shareId; }

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getSharerUserId() { return sharerUserId; }
    public void setSharerUserId(String sharerUserId) { this.sharerUserId = sharerUserId; }

    public ShareType getShareType() { return shareType; }
    public void setShareType(ShareType shareType) { this.shareType = shareType; }

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

    public List<String> getViralChain() { return Collections.unmodifiableList(viralChain); }
    public void setViralChain(List<String> viralChain) { this.viralChain = viralChain != null ? new ArrayList<>(viralChain) : new ArrayList<>(); }

    public ShareStatus getStatus() { return status; }
    public void setStatus(ShareStatus status) { this.status = status; }
}
