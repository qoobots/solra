package com.solra.android.features.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

/**
 * 推送通知管理器 — 提供统一的推送通知 API。
 *
 * 职责：
 * - 检查通知权限状态
 * - 请求通知权限 (Android 13+)
 * - FCM Token 获取与刷新
 * - 本地通知调度 (测试/离线场景)
 *
 * @since 0.1.0
 */
class PushNotificationManager(private val context: Context) {

    companion object {
        const val TAG = "PushNotificationMgr"
    }

    /**
     * 检查通知权限是否已授权
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 及以下不需要运行时权限
        }
    }

    /**
     * 初始化推送服务
     * - 请求 FCM Token
     * - 订阅默认主题
     */
    fun initialize() {
        if (!isNotificationPermissionGranted()) {
            android.util.Log.w(TAG, "Notification permission not granted")
            return
        }

        FirebaseMessaging.getInstance().apply {
            // 获取当前 Token
            token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    android.util.Log.d(TAG, "FCM Token: $token")
                } else {
                    android.util.Log.e(TAG, "FCM Token fetch failed", task.exception)
                }
            }

            // 订阅全局广播主题
            subscribeToTopic("global_broadcast")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        android.util.Log.d(TAG, "Subscribed to global_broadcast")
                    }
                }
        }
    }

    /**
     * 按设备平台上报 Token
     */
    fun uploadDeviceToken(token: String) {
        // TODO: P1 — 调用 not-service gRPC DeviceRegistration API
        // val request = DeviceRegistrationRequest.newBuilder()
        //     .setDeviceToken(token)
        //     .setPlatform(Platform.ANDROID)
        //     .setPushEnabled(true)
        //     .build()
        // notClient.registerDevice(request)
    }
}
