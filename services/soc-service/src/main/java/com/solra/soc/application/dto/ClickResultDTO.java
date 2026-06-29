package com.solra.soc.application.dto;

import com.solra.soc.domain.model.ShareClick;

/**
 * 点击结果 DTO — 不可变 record。
 */
public record ClickResultDTO(String clickId, boolean isNewUser, String redirectUrl) {

    /**
     * 从领域模型 ShareClick 构造 DTO。
     *
     * @param click       分享点击值对象
     * @param isNewUser   是否为新用户（分享链接新访客）
     * @param redirectUrl 点击后的重定向地址
     */
    public static ClickResultDTO from(ShareClick click, boolean isNewUser, String redirectUrl) {
        return new ClickResultDTO(click.getClickId(), isNewUser, redirectUrl);
    }
}
