package com.solra.not.infrastructure.push;

import com.solra.not.domain.model.Platform;
import com.solra.not.domain.model.PushProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PushDispatcher 单元测试。
 * NOT-003: 推送分发器。
 */
@DisplayName("PushDispatcher — 推送分发器测试")
class PushDispatcherTest {

    private PushDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new PushDispatcher(List.of(
                new ApnsPushProvider(),
                new FcmPushProvider(),
                new WebPushProvider()
        ));
    }

    @Nested
    @DisplayName("send — 发送推送")
    class Send {

        @Test
        @DisplayName("iOS 平台路由到 APNS")
        void routesToApns() {
            PushProvider.PushResult result = dispatcher.send(
                    Platform.IOS, "device-token-32chars-minimum-length",
                    "标题", "内容");

            assertTrue(result.success());
            assertNotNull(result.providerMessageId());
            assertTrue(result.providerMessageId().startsWith("apns-"));
        }

        @Test
        @DisplayName("Android 平台路由到 FCM")
        void routesToFcm() {
            PushProvider.PushResult result = dispatcher.send(
                    Platform.ANDROID, "device-token-32chars-minimum-length",
                    "标题", "内容");

            assertTrue(result.success());
            assertTrue(result.providerMessageId().startsWith("fcm-"));
        }

        @Test
        @DisplayName("Web 平台路由到 WebPush")
        void routesToWebPush() {
            PushProvider.PushResult result = dispatcher.send(
                    Platform.WEB, "device-token-32chars-minimum-length",
                    "标题", "内容");

            assertTrue(result.success());
            assertTrue(result.providerMessageId().startsWith("web-"));
        }

        @Test
        @DisplayName("不支持 HarmonyOS 返回失败")
        void unsupportedPlatformReturnsFailure() {
            PushProvider.PushResult result = dispatcher.send(
                    Platform.HARMONYOS, "device-token-32chars-minimum-length",
                    "标题", "内容");

            assertFalse(result.success());
            assertTrue(result.error().contains("HARMONYOS"));
        }

        @Test
        @DisplayName("URGENT 推送也应正常发送")
        void urgentPushSendsNormally() {
            PushProvider.PushResult result = dispatcher.send(
                    Platform.IOS, "device-token-32chars-minimum-length",
                    "紧急通知", "内容", true);

            assertTrue(result.success());
        }
    }

    @Nested
    @DisplayName("validate — 验证令牌")
    class Validate {

        @Test
        @DisplayName("有效令牌返回 true")
        void validTokenReturnsTrue() {
            assertTrue(dispatcher.validate(Platform.IOS,
                    "device-token-32chars-minimum-length-for-validation"));
        }

        @Test
        @DisplayName("无效令牌返回 false")
        void invalidTokenReturnsFalse() {
            assertFalse(dispatcher.validate(Platform.IOS, "short"));
        }
    }

    @Nested
    @DisplayName("supports — 平台支持检查")
    class Supports {

        @Test
        @DisplayName("支持 iOS")
        void supportsIos() {
            assertTrue(dispatcher.supports(Platform.IOS));
        }

        @Test
        @DisplayName("支持 Android")
        void supportsAndroid() {
            assertTrue(dispatcher.supports(Platform.ANDROID));
        }

        @Test
        @DisplayName("不支持 HarmonyOS")
        void notSupportsHarmonyOS() {
            assertFalse(dispatcher.supports(Platform.HARMONYOS));
        }
    }
}
