package com.ssafy.squiz.data.local

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesUtil(context: Context) {

    companion object {
        private const val SHARED_PREFERENCES_NAME = "squiz_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NICKNAME = "user_nickname"
        private const val KEY_USER_PROFILE_IMAGE = "user_profile_image"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    // Token
    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    // User Info
    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, -1)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userNickname: String?
        get() = prefs.getString(KEY_USER_NICKNAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NICKNAME, value).apply()

    var userProfileImage: String?
        get() = prefs.getString(KEY_USER_PROFILE_IMAGE, null)
        set(value) = prefs.edit().putString(KEY_USER_PROFILE_IMAGE, value).apply()

    // Login State
    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_FIRST_LAUNCH, value).apply()

    // FCM
    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN, value).apply()

    // Notification
    var notificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun clearUserData() {
        accessToken = null
        refreshToken = null
        userId = -1
        userEmail = null
        userNickname = null
        userProfileImage = null
        isLoggedIn = false
    }
}
