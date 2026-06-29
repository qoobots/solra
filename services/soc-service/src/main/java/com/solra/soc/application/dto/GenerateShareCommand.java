package com.solra.soc.application.dto;

/**
 * 生成分享链接命令 — 可变 JavaBean。
 */
public class GenerateShareCommand {

    private String spaceId;
    private String sharerUserId;
    private String shareType;

    public String getSpaceId() { return spaceId; }
    public void setSpaceId(String spaceId) { this.spaceId = spaceId; }

    public String getSharerUserId() { return sharerUserId; }
    public void setSharerUserId(String sharerUserId) { this.sharerUserId = sharerUserId; }

    public String getShareType() { return shareType; }
    public void setShareType(String shareType) { this.shareType = shareType; }
}
