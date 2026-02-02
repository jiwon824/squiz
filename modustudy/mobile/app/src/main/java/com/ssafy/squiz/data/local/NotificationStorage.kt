package com.ssafy.squiz.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ssafy.squiz.data.remote.model.NotificationDTO

/**
 * 알림 로컬 저장소
 * SharedPreferences + Gson을 사용하여 알림을 로컬에 저장
 */
class NotificationStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "squiz_notifications"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val MAX_NOTIFICATIONS = 100  // 최대 저장 개수
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * 알림 저장
     */
    fun saveNotification(notification: NotificationDTO) {
        val notifications = getNotifications().toMutableList()

        // 중복 체크
        if (notifications.none { it.id == notification.id }) {
            notifications.add(0, notification)  // 최신 알림을 앞에 추가

            // 최대 개수 제한
            val trimmedList = notifications.take(MAX_NOTIFICATIONS)
            saveNotifications(trimmedList)
        }
    }

    /**
     * 모든 알림 조회
     */
    fun getNotifications(): List<NotificationDTO> {
        val json = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<NotificationDTO>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 알림 목록 저장
     */
    private fun saveNotifications(notifications: List<NotificationDTO>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    /**
     * 알림 읽음 처리
     */
    fun markAsRead(notificationId: Long) {
        val notifications = getNotifications().map { notification ->
            if (notification.id == notificationId) {
                notification.copy(isRead = true)
            } else {
                notification
            }
        }
        saveNotifications(notifications)
    }

    /**
     * 모든 알림 읽음 처리
     */
    fun markAllAsRead() {
        val notifications = getNotifications().map { it.copy(isRead = true) }
        saveNotifications(notifications)
    }

    /**
     * 읽지 않은 알림 수
     */
    fun getUnreadCount(): Int {
        return getNotifications().count { !it.isRead }
    }

    /**
     * 모든 알림 삭제
     */
    fun clearAll() {
        prefs.edit().remove(KEY_NOTIFICATIONS).apply()
    }

    /**
     * 특정 알림 삭제
     */
    fun deleteNotification(notificationId: Long) {
        val notifications = getNotifications().filter { it.id != notificationId }
        saveNotifications(notifications)
    }
}
