package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * API 공통 응답 래퍼
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ErrorResponse?
)

data class ErrorResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?
)

/**
 * OAuth 콜백 요청
 */
data class OAuth2CallbackRequest(
    @SerializedName("code") val code: String
)

/**
 * 토큰 갱신 요청
 */
data class TokenRefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

/**
 * 인증 응답 (로그인 성공 시)
 */
data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Int?,
    @SerializedName("isNewUser") val isNewUser: Boolean?,
    @SerializedName("user") val user: UserDTO?,
    @SerializedName("loginProvider") val loginProvider: String?
)

/**
 * 사용자 정보
 */
data class UserDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String?,
    @SerializedName("nickname") val nickname: String?,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("loginProvider") val loginProvider: String?
)

/**
 * 토큰 갱신 응답
 */
data class TokenRefreshResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("expiresIn") val expiresIn: Int?
)
