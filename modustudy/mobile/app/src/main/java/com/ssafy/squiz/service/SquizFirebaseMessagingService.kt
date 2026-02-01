package com.ssafy.squiz.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.squiz.MainActivity
import com.ssafy.squiz.R
import com.ssafy.squiz.base.SquizApplication
import com.ssafy.squiz.data.remote.model.NotificationDTO

/**
 * Firebase Cloud Messaging 서비스
 */
class SquizFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SquizFCMService"
        private const val CHANNEL_ID = "squiz_notifications"

        // FCM 토큰 (앱에서 접근 가능)
        var fcmToken: String? = null
    }

    /**
     * FCM 토큰 갱신 시 호출
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token 갱신: $token")
        fcmToken = token
        SquizApplication.fcmToken = token

        // 서버에 새 토큰 등록
        try {
            SquizApplication.getInstance().registerFcmTokenToServer(token)
        } catch (e: Exception) {
            Log.e(TAG, "토큰 등록 실패: ${e.message}")
        }
    }

    /**
     * 메시지 수신 시 호출
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "메시지 수신: ${remoteMessage.data}")

        // 데이터 메시지 처리
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            handleDataMessage(data)
        }

        // 알림 메시지 처리 (앱이 포그라운드일 때만)
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Squiz",
                body = notification.body ?: ""
            )
        }
    }

    /**
     * 데이터 메시지 처리
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Squiz"
        val body = data["body"] ?: ""
        val type = data["type"] ?: "GENERAL"
        val notificationId = data["notificationId"]?.toLongOrNull() ?: System.currentTimeMillis()

        // 로컬 저장소에 알림 저장
        try {
            val notification = NotificationDTO(
                id = notificationId,
                type = type,
                title = title,
                content = body,
                isRead = false,
                createdAt = java.time.Instant.now().toString()
            )
            SquizApplication.getInstance().notificationStorage.saveNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "알림 저장 실패: ${e.message}")
        }

        // 알림 표시
        showNotification(title, body, type)
    }

    /**
     * 알림 표시
     */
    private fun showNotification(title: String, body: String, type: String = "GENERAL") {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
