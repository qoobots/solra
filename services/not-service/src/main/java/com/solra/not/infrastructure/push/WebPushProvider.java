package com.solra.not.infrastructure.push;

import com.solra.not.domain.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * WebPushProvider — Web Push API Mock 实现。
 */
@Component
public class WebPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(WebPushProvider.class);

    @Override
    public boolean supports(Platform platform) {
        return platform == Platform.WEB;
    }

    @Override
    public PushResult send(String deviceToken, String title, String body) {
        String msgId = "web-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[WebPush Mock] Push to subscription={} title={}", deviceToken.substring(0, 8), title);
        return PushResult.success(msgId);
    }

    @Override
    public boolean validate(String deviceToken) {
        return deviceToken != null && deviceToken.startsWith("http");
    }
}
