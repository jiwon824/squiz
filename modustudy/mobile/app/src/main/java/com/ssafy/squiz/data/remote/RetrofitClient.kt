package com.ssafy.squiz.data.remote

import com.ssafy.squiz.BuildConfig
import com.ssafy.squiz.data.local.AuthManager
import com.ssafy.squiz.data.remote.api.*
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 클라이언트 싱글톤
 */
object RetrofitClient {

    // 서버 Base URL (프로덕션)
    private const val BASE_URL = "https://i14d106.p.ssafy.io/"

    // 개발용 (로컬 서버)
    // private const val BASE_URL = "http://10.0.2.2:8080/"  // Android Emulator에서 localhost

    private var authManager: AuthManager? = null

    fun init(authManager: AuthManager) {
        this.authManager = authManager
    }

    /**
     * Auth Interceptor - JWT 토큰 및 User-Id 헤더 자동 추가
     */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        // 토큰이 필요 없는 경로 (로그인, 토큰 갱신)
        val noAuthPaths = listOf("/api/v1/auth/oauth", "/api/v1/auth/token/refresh")
        val isAuthRequired = noAuthPaths.none { originalRequest.url.encodedPath.startsWith(it) }

        // accessToken을 한 번만 가져와서 사용 (두 번 호출 시 race condition 방지)
        val accessToken = authManager?.getAccessToken()
        val request = if (isAuthRequired && accessToken != null) {
            val userId = authManager?.getCurrentUserId() ?: 0L
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .header("User-Id", userId.toString())
                .build()
        } else {
            originalRequest
        }

        chain.proceed(request)
    }

    /**
     * Logging Interceptor (디버그용)
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * OkHttp 클라이언트
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit 인스턴스
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Auth API
     */
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    /**
     * Notification API
     */
    val notificationApi: NotificationApi by lazy {
        retrofit.create(NotificationApi::class.java)
    }

    /**
     * Study API
     */
    val studyApi: StudyApi by lazy {
        retrofit.create(StudyApi::class.java)
    }

    /**
     * Review API (FSRS)
     */
    val reviewApi: ReviewApi by lazy {
        retrofit.create(ReviewApi::class.java)
    }

    /**
     * Attendance API (BLE 출석)
     */
    val attendanceApi: AttendanceApi by lazy {
        retrofit.create(AttendanceApi::class.java)
    }

    /**
     * User API
     */
    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    /**
     * Schedule API
     */
    val scheduleApi: ScheduleApi by lazy {
        retrofit.create(ScheduleApi::class.java)
    }

    /**
     * Home API
     */
    val homeApi: HomeApi by lazy {
        retrofit.create(HomeApi::class.java)
    }
}
