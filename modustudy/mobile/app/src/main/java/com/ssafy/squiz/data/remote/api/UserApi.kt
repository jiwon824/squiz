package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 사용자 API
 */
interface UserApi {

    /**
     * 내 프로필 조회
     */
    @GET("api/v1/users/me")
    suspend fun getMyProfile(): Response<ApiResponse<UserProfileDTO>>

    /**
     * 프로필 업데이트
     */
    @PUT("api/v1/users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserProfileDTO>>

    /**
     * 알림 설정 조회
     */
    @GET("api/v1/users/me/notification-settings")
    suspend fun getNotificationSettings(): Response<ApiResponse<NotificationSettings>>

    /**
     * 알림 설정 업데이트
     */
    @PUT("api/v1/users/me/notification-settings")
    suspend fun updateNotificationSettings(
        @Body settings: NotificationSettings
    ): Response<ApiResponse<NotificationSettings>>

    /**
     * 개인정보 설정 조회
     */
    @GET("api/v1/users/me/privacy-settings")
    suspend fun getPrivacySettings(): Response<ApiResponse<PrivacySettings>>

    /**
     * 개인정보 설정 업데이트
     */
    @PUT("api/v1/users/me/privacy-settings")
    suspend fun updatePrivacySettings(
        @Body settings: PrivacySettings
    ): Response<ApiResponse<PrivacySettings>>

    /**
     * 활동 통계 조회
     */
    @GET("api/v1/users/me/activity-stats")
    suspend fun getActivityStats(): Response<ApiResponse<ActivityStats>>

    /**
     * 잔디 데이터 조회
     */
    @GET("api/v1/users/me/grass")
    suspend fun getGrassData(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<GrassData>>

    /**
     * 활동 상세 조회
     */
    @GET("api/v1/users/me/activity/{date}")
    suspend fun getActivityDetail(
        @Path("date") date: String
    ): Response<ApiResponse<ActivityDetail>>
}
