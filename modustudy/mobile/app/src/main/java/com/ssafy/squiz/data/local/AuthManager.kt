package com.ssafy.squiz.data.local

import android.content.Context

/**
 * 인증 관리 클래스
 * - 자동로그인 체크
 * - 토큰 관리
 * - 로그인/로그아웃 처리
 */
class AuthManager(context: Context) {

    private val prefs = SharedPreferencesUtil(context)

    /**
     * 자동로그인 가능 여부 확인
     * 배포 환경에서는 실제 토큰 유효성 검사
     */
    fun isAutoLoginAvailable(): Boolean {
        // 개발 모드일 때는 로그인 화면으로 이동 (OAuth 테스트)
        if (devModeAutoLogin) {
            return false // 개발 중에는 항상 로그인 화면 보여주기
        }

        // 실제 로직:
        // 1. isLoggedIn 체크
        // 2. accessToken 존재 여부 체크
        // 3. accessToken 만료 여부 체크 (JWT 디코딩) - TODO: 구현 필요
        // 4. 만료 시 refreshToken으로 갱신 시도 - TODO: 구현 필요

        return prefs.isLoggedIn && prefs.accessToken != null
    }

    /**
     * 개발 모드 설정
     * true: 항상 로그인 화면 표시 (개발용 - OAuth 테스트)
     * false: 실제 로그인 상태 체크 (배포용)
     */
    var devModeAutoLogin: Boolean = true

    /**
     * 실제 로그인 상태 확인 (배포용)
     */
    fun isLoggedIn(): Boolean {
        return prefs.isLoggedIn && prefs.accessToken != null
    }

    /**
     * Mattermost 인증번호 검증 후 로그인 처리
     * TODO: 실제 API 연동 시 구현
     */
    fun loginWithVerificationCode(
        email: String,
        code: String,
        onSuccess: (accessToken: String, refreshToken: String) -> Unit,
        onError: (String) -> Unit
    ) {
        // TODO: API 호출
        // 1. 서버에 email + code 전송
        // 2. 서버에서 Mattermost로 전송된 코드와 비교
        // 3. 검증 성공 시 JWT 토큰 발급받음
        // 4. 토큰 저장 및 콜백 호출
    }

    /**
     * 토큰 저장 (로그인 성공 시 호출)
     */
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.accessToken = accessToken
        prefs.refreshToken = refreshToken
        prefs.isLoggedIn = true
    }

    /**
     * 사용자 정보 저장
     */
    fun saveUserInfo(userId: Long, email: String, nickname: String, profileImage: String?) {
        prefs.userId = userId
        prefs.userEmail = email
        prefs.userNickname = nickname
        prefs.userProfileImage = profileImage
    }

    /**
     * 로그아웃
     */
    fun logout() {
        // TODO: 서버에 토큰 무효화 요청
        prefs.clearUserData()
    }

    /**
     * Access Token 가져오기 (API 호출 시 사용)
     */
    fun getAccessToken(): String? = prefs.accessToken

    /**
     * Refresh Token 가져오기
     */
    fun getRefreshToken(): String? = prefs.refreshToken

    /**
     * Access Token만 저장 (토큰 갱신 시)
     */
    fun saveAccessToken(accessToken: String) {
        prefs.accessToken = accessToken
    }

    /**
     * 현재 로그인된 사용자 ID
     */
    fun getCurrentUserId(): Long = prefs.userId

    /**
     * 현재 로그인된 사용자 닉네임
     */
    fun getCurrentUserNickname(): String? = prefs.userNickname

    /**
     * Access Token 갱신
     * TODO: 실제 구현 시 API 호출
     */
    suspend fun refreshAccessToken(): Boolean {
        val refreshToken = prefs.refreshToken ?: return false

        // TODO: API 호출로 새 accessToken 받아오기
        // val response = authApi.refreshToken(refreshToken)
        // prefs.accessToken = response.accessToken
        // return true

        return false
    }

    companion object {
        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
