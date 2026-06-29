package com.solra.not.infrastructure.push;

import com.solra.not.domain.model.Platform;

/**
 * PushProvider — 推送提供者接口。
 * 定义各平台推送的统一抽象。
 */
public interface PushProvider {

    /** 判断是否支持该平台 */
    boolean supports(Platform platform);

    /** 发送推送消息 */
    PushResult send(String deviceToken, String title, String body);

    /** 验证设备令牌有效性 */
    boolean validate(String deviceToken);

    /** 推送结果 */
    record PushResult(boolean success, String providerMessageId, String error) {
        public static PushResult success(String msgId) { return new PushResult(true, msgId, null); }
        public static PushResult failure(String error) { return new PushResult(false, null, error); }
    }
}
