package com.ssafy.squiz.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.ssafy.squiz.BuildConfig
import com.ssafy.squiz.data.local.AuthManager
import com.ssafy.squiz.base.SquizApplication
import com.ssafy.squiz.data.remote.RetrofitClient
import com.ssafy.squiz.data.remote.model.OAuth2CallbackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 인증 Repository
 * OAuth 로그인 및 토큰 관리 담당
 */
class AuthRepository(
    private val context: Context,
    private val authManager: AuthManager
) {
    companion object {
        private const val TAG = "AuthRepository"
    }

    /**
     * 카카오 로그인
     * 카카오톡 앱이 설치되어 있으면 앱으로, 아니면 웹으로 로그인
     */
    suspend fun loginWithKakao(activity: Activity): Result<AuthResult> = withContext(Dispatchers.Main) {
        try {
            // 카카오톡 설치 여부 확인
            val isKakaoTalkAvailable = UserApiClient.instance.isKakaoTalkLoginAvailable(context)

            val oAuthToken = if (isKakaoTalkAvailable) {
                try {
                    loginWithKakaoTalk(activity)
                } catch (e: Exception) {
                    // 카카오톡 로그인 실패 시 웹으로 fallback
                    if (e is ClientError && e.reason == ClientErrorCause.Cancelled) {
                        throw e // 사용자 취소는 그대로 throw
                    }
                    Log.w(TAG, "카카오톡 로그인 실패, 웹 로그인으로 전환: ${e.message}")
                    loginWithKakaoAccount(activity)
                }
            } else {
                loginWithKakaoAccount(activity)
            }

            Log.d(TAG, "카카오 로그인 성공, 인가코드로 백엔드 인증 시작")

            // 백엔드에 인증 코드 전송 (카카오는 accessToken을 code로 사용)
            val authResult = authenticateWithBackend("kakao", oAuthToken.accessToken)
            Result.success(authResult)

        } catch (e: Exception) {
            Log.e(TAG, "카카오 로그인 실패: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun loginWithKakaoTalk(activity: Activity): OAuthToken = suspendCancellableCoroutine { cont ->
        UserApiClient.instance.loginWithKakaoTalk(activity) { token, error ->
            if (error != null) {
                cont.resumeWithException(error)
            } else if (token != null) {
                cont.resume(token)
            } else {
                cont.resumeWithException(Exception("Unknown error"))
            }
        }
    }

    private suspend fun loginWithKakaoAccount(activity: Activity): OAuthToken = suspendCancellableCoroutine { cont ->
        UserApiClient.instance.loginWithKakaoAccount(activity) { token, error ->
            if (error != null) {
                cont.resumeWithException(error)
            } else if (token != null) {
                cont.resume(token)
            } else {
                cont.resumeWithException(Exception("Unknown error"))
            }
        }
    }

    /**
     * 네이버 로그인
     */
    suspend fun loginWithNaver(activity: Activity): Result<AuthResult> = withContext(Dispatchers.Main) {
        try {
            val accessToken = naverLogin(activity)
            Log.d(TAG, "네이버 로그인 성공, 백엔드 인증 시작")

            // 백엔드에 토큰 전송
            val state = NaverIdLoginSDK.getState()?.toString() ?: ""
            val authResult = authenticateWithBackend("naver", accessToken, state)
            Result.success(authResult)

        } catch (e: Exception) {
            Log.e(TAG, "네이버 로그인 실패: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun naverLogin(activity: Activity): String = suspendCancellableCoroutine { cont ->
        val callback = object : OAuthLoginCallback {
            override fun onSuccess() {
                val accessToken = NaverIdLoginSDK.getAccessToken()
                if (accessToken != null) {
                    cont.resume(accessToken)
                } else {
                    cont.resumeWithException(Exception("Access token is null"))
                }
            }

            override fun onFailure(httpStatus: Int, message: String) {
                cont.resumeWithException(Exception("Naver login failed: $message (status: $httpStatus)"))
            }

            override fun onError(errorCode: Int, message: String) {
                cont.resumeWithException(Exception("Naver login error: $message (code: $errorCode)"))
            }
        }

        NaverIdLoginSDK.authenticate(activity, callback)
    }

    /**
     * 구글 로그인을 위한 Intent 생성
     * Activity에서 startActivityForResult로 호출해야 함
     */
    fun getGoogleSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .build()

        return GoogleSignIn.getClient(activity, gso)
    }

    /**
     * 구글 캘린더 연동을 위한 Google Sign-In 클라이언트
     * Calendar API 스코프 포함
     */
    fun getGoogleCalendarSignInClient(activity: Activity): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestServerAuthCode(BuildConfig.GOOGLE_CLIENT_ID)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar.readonly"))
            .build()

        return GoogleSignIn.getClient(activity, gso)
    }

    /**
     * 구글 로그인 결과 처리
     */
    suspend fun handleGoogleSignInResult(serverAuthCode: String): Result<AuthResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "구글 로그인 성공, 백엔드 인증 시작")
            val authResult = authenticateWithBackend("google", serverAuthCode)
            Result.success(authResult)
        } catch (e: Exception) {
            Log.e(TAG, "구글 로그인 처리 실패: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 백엔드 인증 API 호출
     * OAuth 제공자로부터 받은 코드/토큰을 백엔드에 전송하여 JWT 토큰 발급
     */
    private suspend fun authenticateWithBackend(
        provider: String,
        code: String,
        state: String? = null
    ): AuthResult = withContext(Dispatchers.IO) {
        val request = OAuth2CallbackRequest(code)
        val response = RetrofitClient.authApi.oauthCallback(provider, request, state)

        if (response.isSuccessful && response.body()?.success == true) {
            val authResponse = response.body()!!.data!!

            // 토큰 저장
            authManager.saveTokens(authResponse.accessToken, authResponse.refreshToken)

            // 사용자 정보 저장
            authResponse.user?.let { user ->
                authManager.saveUserInfo(
                    userId = user.id,
                    email = user.email ?: "",
                    nickname = user.nickname ?: "",
                    profileImage = user.profileImage
                )
            }

            // 로그인 성공 후 FCM 토큰 서버에 등록
            try {
                SquizApplication.getInstance().registerFcmTokenToServer()
            } catch (e: Exception) {
                Log.w(TAG, "FCM 토큰 등록 실패 (무시): ${e.message}")
            }

            AuthResult(
                isNewUser = authResponse.isNewUser ?: false,
                accessToken = authResponse.accessToken,
                refreshToken = authResponse.refreshToken,
                user = authResponse.user
            )
        } else {
            val errorMsg = response.body()?.error?.message ?: response.message() ?: "Unknown error"
            throw Exception("Backend authentication failed: $errorMsg")
        }
    }

    /**
     * Access Token 갱신
     */
    suspend fun refreshAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = authManager.getRefreshToken()
                ?: return@withContext Result.failure(Exception("No refresh token"))

            val request = com.ssafy.squiz.data.remote.model.TokenRefreshRequest(refreshToken)
            val response = RetrofitClient.authApi.refreshToken(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val newAccessToken = response.body()!!.data!!.accessToken
                authManager.saveAccessToken(newAccessToken)
                Result.success(newAccessToken)
            } else {
                Result.failure(Exception("Token refresh failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 로그아웃
     */
    fun logout() {
        // 카카오 로그아웃
        UserApiClient.instance.logout { error ->
            if (error != null) {
                Log.w(TAG, "카카오 로그아웃 실패: ${error.message}")
            }
        }

        // 네이버 로그아웃
        NaverIdLoginSDK.logout()

        // 로컬 데이터 삭제
        authManager.logout()
    }
}

/**
 * 인증 결과 데이터 클래스
 */
data class AuthResult(
    val isNewUser: Boolean,
    val accessToken: String,
    val refreshToken: String,
    val user: com.ssafy.squiz.data.remote.model.UserDTO?
)
