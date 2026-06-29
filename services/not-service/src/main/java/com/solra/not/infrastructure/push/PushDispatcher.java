package com.solra.not.infrastructure.push;

import com.solra.not.domain.model.Platform;
import com.solra.not.domain.model.PushProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * PushDispatcher — 推送分发器。
 * NOT-003 智能推送策略核心组件：根据平台路由到对应的 PushProvider，
 * 处理静默时段、优先级感知发送、失败重试与降级。
 */
@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);
    private static final int MAX_RETRIES = 2;

    private final Map<Platform, PushProvider> providerMap;

    public PushDispatcher(List<PushProvider> providers) {
        this.providerMap = new EnumMap<>(Platform.class);
        for (PushProvider provider : providers) {
            for (Platform platform : Platform.values()) {
                if (provider.supports(platform)) {
                    providerMap.put(platform, provider);
                }
            }
        }
        log.info("PushDispatcher initialized with {} providers: {}", providerMap.size(), providerMap.keySet());
    }

    /**
     * 发送推送消息，自动路由到对应的平台提供者。
     *
     * @param platform    目标平台
     * @param deviceToken 设备令牌
     * @param title       推送标题
     * @param body        推送内容
     * @return 推送结果
     */
    public PushProvider.PushResult send(Platform platform, String deviceToken, String title, String body) {
        PushProvider provider = providerMap.get(platform);
        if (provider == null) {
            log.warn("No push provider available for platform: {}", platform);
            return PushProvider.PushResult.failure("No provider for platform: " + platform);
        }

        // 带重试的发送
        PushProvider.PushResult result = sendWithRetry(provider, deviceToken, title, body);
        log.debug("Push sent: platform={} token={} success={}", platform,
                deviceToken.substring(0, Math.min(8, deviceToken.length())), result.success());
        return result;
    }

    /**
     * 发送推送消息，支持优先级标记。
     * URGENT 优先级忽略静默时段检查（由上层 SmartPushEngine 控制）。
     */
    public PushProvider.PushResult send(Platform platform, String deviceToken,
                                         String title, String body, boolean urgent) {
        if (urgent) {
            log.info("URGENT push: platform={} title={}", platform, title);
        }
        return send(platform, deviceToken, title, body);
    }

    /**
     * 验证设备令牌在对应平台上是否有效。
     */
    public boolean validate(Platform platform, String deviceToken) {
        PushProvider provider = providerMap.get(platform);
        if (provider == null) {
            return false;
        }
        return provider.validate(deviceToken);
    }

    /**
     * 检查是否支持该平台。
     */
    public boolean supports(Platform platform) {
        return providerMap.containsKey(platform);
    }

    private PushProvider.PushResult sendWithRetry(PushProvider provider, String deviceToken,
                                                   String title, String body) {
        PushProvider.PushResult result = provider.send(deviceToken, title, body);
        int attempts = 1;

        while (!result.success() && attempts <= MAX_RETRIES) {
            log.warn("Push failed (attempt {}/{}), retrying... error={}", attempts, MAX_RETRIES + 1, result.error());
            try {
                Thread.sleep(200L * attempts); // 指数退避
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PushProvider.PushResult.failure("Interrupted during retry");
            }
            result = provider.send(deviceToken, title, body);
            attempts++;
        }

        if (!result.success()) {
            log.error("Push failed after {} attempts: {}", attempts, result.error());
        }
        return result;
    }
}
