package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 인증 API 인터페이스
 */
interface AuthApi {

    /**
     * OAuth 콜백 처리 (소셜 로그인)
     * POST /api/v1/auth/oauth/{provider}/callback
     *
     * @param provider kakao, naver, google
     * @param request 인증 코드
     * @param state 네이버 로그인 시 필요한 state 파라미터
     */
    @POST("/api/v1/auth/oauth/{provider}/callback")
    suspend fun oauthCallback(
        @Path("provider") provider: String,
        @Body request: OAuth2CallbackRequest,
        @Query("state") state: String? = null
    ): Response<ApiResponse<AuthResponse>>

    /**
     * Access Token 갱신
     * POST /api/v1/auth/token/refresh
     */
    @POST("/api/v1/auth/token/refresh")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest
    ): Response<ApiResponse<TokenRefreshResponse>>
}
