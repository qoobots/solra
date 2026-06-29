package com.solra.soc.application.dto;

import com.solra.soc.domain.model.ShareSession;

import java.time.Instant;

/**
 * 分享结果 DTO — 不可变 record。
 */
public record ShareResultDTO(String shareId, String shareCode, String shareUrl, Instant expiresAt) {

    /**
     * 从领域模型 ShareSession 构造 DTO。
     *
     * @param session 分享会话聚合根
     * @param baseUrl 分享链接基础 URL（如 "https://solra.app/s/"）
     */
    public static ShareResultDTO from(ShareSession session, String baseUrl) {
        String shareUrl = baseUrl != null
                ? baseUrl + session.getShareCode()
                : session.getShareCode();
        return new ShareResultDTO(
                session.getShareId(),
                session.getShareCode(),
                shareUrl,
                session.getExpiresAt()
        );
    }
}
