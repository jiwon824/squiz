package com.ssafy.squiz.base

import android.app.Application
import android.content.Context
import android.util.Log
import com.kakao.sdk.common.KakaoSdk
import com.navercorp.nid.NaverIdLoginSDK
import com.ssafy.squiz.BuildConfig
import com.ssafy.squiz.data.local.AuthManager
import com.ssafy.squiz.data.local.SharedPreferencesUtil
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.repository.AuthRepository

class SquizApplication : Application() {

    companion object {
        private const val TAG = "SquizApplication"

        private lateinit var instance: SquizApplication

        fun getInstance(): SquizApplication = instance

        fun getContext(): Context = instance.applicationContext
    }

    lateinit var sharedPreferencesUtil: SharedPreferencesUtil
        private set

    lateinit var authManager: AuthManager
        private set

    lateinit var authRepository: AuthRepository
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

        // Kakao SDK 초기화
        initKakaoSdk()

        // Naver SDK 초기화
        initNaverSdk()

        Log.d(TAG, "SquizApplication initialized")
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
}
