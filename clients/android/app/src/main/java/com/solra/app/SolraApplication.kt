package com.solra.app

import android.app.Application

/// Solra Android 应用入口。
class SolraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: P1 — 初始化 SolraCore JNI 桥接
    }
}
