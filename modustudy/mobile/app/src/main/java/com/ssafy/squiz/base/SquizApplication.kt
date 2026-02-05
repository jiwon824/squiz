package com.ssafy.squiz.base

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NaverIdLoginSDK
import com.ssafy.squiz.BuildConfig
import com.ssafy.squiz.data.local.AuthManager
import com.ssafy.squiz.data.local.NotificationStorage
import com.ssafy.squiz.data.local.SharedPreferencesUtil
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.FcmTokenRequest
import com.ssafy.squiz.data.repository.AuthRepository
import com.ssafy.squiz.service.SquizFirebaseMessagingService
import com.ssafy.squiz.worker.RecordingCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SquizApplication : Application() {

    companion object {
        private const val TAG = "SquizApplication"
        private const val CHANNEL_ID = "squiz_notifications"
        private const val CHANNEL_NAME = "Squiz 알림"

        private lateinit var instance: SquizApplication

        // FCM 토큰
        var fcmToken: String? = null

        fun getInstance(): SquizApplication = instance

        fun getContext(): Context = instance.applicationContext
    }

    lateinit var sharedPreferencesUtil: SharedPreferencesUtil
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var notificationStorage: NotificationStorage
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // SharedPreferences 초기화
        sharedPreferencesUtil = SharedPreferencesUtil(applicationContext)

        // AuthManager 초기화
        authManager = AuthManager.getInstance(applicationContext)

        // RetrofitClient 초기화 (AuthManager 전달)
        RetrofitClient.init(authManager)

        // AuthRepository 초기화
        authRepository = AuthRepository(applicationContext, authManager)

        // NotificationStorage 초기화
        notificationStorage = NotificationStorage(applicationContext)

        // Kakao SDK 초기화
        initKakaoSdk()

        // Naver SDK 초기화
        initNaverSdk()

        // 알림 채널 생성
        createNotificationChannel()

        // FCM 토큰 초기화
        initFcmToken()

        // 녹음 파일 자동 정리 스케줄링 (7일 후 자동 삭제)
        scheduleRecordingCleanup()

        Log.d(TAG, "SquizApplication initialized")
    }

    /**
     * 만료된 녹음 파일 자동 정리 스케줄링
     */
    private fun scheduleRecordingCleanup() {
        RecordingCleanupWorker.schedule(applicationContext)
    }

    private fun initKakaoSdk() {
        try {
            val kakaoAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY
            if (kakaoAppKey.isNotEmpty() && kakaoAppKey != "your_kakao_native_app_key") {
                KakaoSdk.init(this, kakaoAppKey)
                Log.d(TAG, "Kakao SDK initialized")
            } else {
                Log.w(TAG, "Kakao SDK not initialized: API key not configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Kakao SDK: ${e.message}")
        }
    }

    private fun initNaverSdk() {
        try {
            val clientId = BuildConfig.NAVER_CLIENT_ID
            val clientSecret = BuildConfig.NAVER_CLIENT_SECRET
            val clientName = BuildConfig.NAVER_CLIENT_NAME

            if (clientId.isNotEmpty() && clientId != "your_naver_client_id") {
                NaverIdLoginSDK.initialize(
                    this,
                    clientId,
                    clientSecret,
                    clientName
                )
                Log.d(TAG, "Naver SDK initialized")
            } else {
                Log.w(TAG, "Naver SDK not initialized: API credentials not configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Naver SDK: ${e.message}")
        }
    }

    /**
     * 알림 채널 생성 (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Squiz 앱 알림"
                enableVibration(true)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    /**
     * FCM 토큰 초기화 및 서버 등록
     */
    private fun initFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "FCM 토큰 가져오기 실패: ${task.exception}")
                return@addOnCompleteListener
            }

            val token = task.result
            fcmToken = token
            SquizFirebaseMessagingService.fcmToken = token
            Log.d(TAG, "FCM Token: $token")

            // 로그인 상태이면 서버에 토큰 등록
            if (authManager.isLoggedIn()) {
                registerFcmTokenToServer(token)
            }
        }
    }

    /**
     * FCM 토큰 서버 등록
     * accessToken이 있어야 User-Id 헤더가 전송됨
     */
    fun registerFcmTokenToServer(token: String? = fcmToken) {
        if (token == null) {
            Log.w(TAG, "FCM 토큰이 없습니다")
            return
        }

        // accessToken이 없으면 등록 불가 (User-Id 헤더 전송 안됨)
        val accessToken = authManager.getAccessToken()
        if (accessToken == null) {
            Log.w(TAG, "FCM 토큰 서버 등록 스킵: accessToken이 없습니다")
            return
        }

        Log.d(TAG, "FCM 토큰 서버 등록 시도: userId=${authManager.getCurrentUserId()}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.notificationApi.registerFcmToken(
                    FcmTokenRequest(token = token)
                )
                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "FCM 토큰 서버 등록 성공")
                } else {
                    Log.e(TAG, "FCM 토큰 서버 등록 실패: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM 토큰 서버 등록 오류: ${e.message}")
            }
        }
    }
}
