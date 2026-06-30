package com.solra.app

import android.app.Application
import com.solra.android.features.notifications.PushNotificationManager

/// Solra Android 应用入口。
class SolraApplication : Application() {

    lateinit var pushManager: PushNotificationManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 初始化推送通知管理
        pushManager = PushNotificationManager(this)

        // 请求通知权限后初始化 FCM
        if (pushManager.isNotificationPermissionGranted()) {
            pushManager.initialize()
        }

        // TODO: P1 — 初始化 SolraCore JNI 桥接
    }
}
