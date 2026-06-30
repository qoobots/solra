package com.solra.android.features.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.solra.app.MainActivity
import com.solra.app.R

/**
 * Firebase Cloud Messaging 服务 — 处理远程推送消息。
 *
 * 职责：
 * 1. 获取 FCM Token 并上报到 Solra not-service
 * 2. 前台消息展示通知横幅
 * 3. 数据消息处理（静默同步 / 深度链接）
 *
 * @since 0.1.0
 */
class SolraFirebaseMessagingService : com.google.firebase.messaging.FirebaseMessagingService() {

    companion object {
        const val TAG = "SolraFCM"
        const val CHANNEL_ID_MESSAGES = "solra_messages"
        const val CHANNEL_ID_SYSTEM = "solra_system"
        const val CHANNEL_NAME_MESSAGES = "新消息"
        const val CHANNEL_NAME_SYSTEM = "系统通知"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    /**
     * 获取新的 FCM Token — 上报到后端
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d(TAG, "FCM Token refreshed: $token")

        // TODO: P1 — 调用 not-service gRPC DeviceRegistration API
        // uploadDeviceToken(token)

        // 本地持久化（可选）
        getSharedPreferences("solra_push", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    /**
     * 收到远程消息
     */
    override fun onMessageReceived(message: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(message)
        android.util.Log.d(TAG, "Message from: ${message.from}")

        when {
            // 通知消息 — 展示系统通知
            message.notification != null -> {
                handleNotificationMessage(message)
            }
            // 数据消息 — 静默处理
            message.data.isNotEmpty() -> {
                handleDataMessage(message.data)
            }
        }
    }

    /**
     * 展示通知横幅
     */
    private fun handleNotificationMessage(message: com.google.firebase.messaging.RemoteMessage) {
        val notification = message.notification ?: return
        val title = notification.title ?: "Solra"
        val body = notification.body ?: ""

        showNotification(
            channelId = CHANNEL_ID_MESSAGES,
            title = title,
            body = body,
            data = message.data
        )
    }

    /**
     * 处理数据消息（静默推送）
     */
    private fun handleDataMessage(data: Map<String, String>) {
        // 根据消息类型处理
        when (data["type"]) {
            "space_invite" -> {
                // 空间邀请通知
                val spaceId = data["space_id"] ?: return
                val inviterName = data["inviter_name"] ?: "好友"
                showNotification(
                    channelId = CHANNEL_ID_MESSAGES,
                    title = "空间邀请",
                    body = "$inviterName 邀请你加入空间",
                    data = mapOf("space_id" to spaceId, "action" to "open_space")
                )
            }
            "avatar_message" -> {
                // 虚拟人新消息
                val avatarName = data["avatar_name"] ?: "虚拟人"
                val messagePreview = data["message_preview"] ?: "新消息"
                showNotification(
                    channelId = CHANNEL_ID_MESSAGES,
                    title = avatarName,
                    body = messagePreview,
                    data = data
                )
            }
            "sync" -> {
                // 静默数据同步
                android.util.Log.d(TAG, "Silent sync: ${data["entity"]}")
            }
        }
    }

    /**
     * 显示本地通知
     */
    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // 将通知数据附加到 Intent 以支持深度链接
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * 创建通知渠道（Android 8.0+ 必需）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_ID_MESSAGES,
                CHANNEL_NAME_MESSAGES,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "空间消息与虚拟人对话通知"
                enableVibration(true)
            },
            NotificationChannel(
                CHANNEL_ID_SYSTEM,
                CHANNEL_NAME_SYSTEM,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "系统与服务通知"
            }
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(channels)
    }
}
