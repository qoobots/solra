package com.solra.not.infrastructure.push;

import com.solra.not.domain.model.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * FcmPushProvider — Firebase Cloud Messaging Mock 实现。
 */
@Component
public class FcmPushProvider implements PushProvider {

    private static final Logger log = LoggerFactory.getLogger(FcmPushProvider.class);

    @Override
    public boolean supports(Platform platform) {
        return platform == Platform.ANDROID;
    }

    @Override
    public PushResult send(String deviceToken, String title, String body) {
        String msgId = "fcm-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("[FCM Mock] Push to device={} title={}", deviceToken.substring(0, 8), title);
        return PushResult.success(msgId);
    }

    @Override
    public boolean validate(String deviceToken) {
        return deviceToken != null && deviceToken.length() >= 32;
    }
}
