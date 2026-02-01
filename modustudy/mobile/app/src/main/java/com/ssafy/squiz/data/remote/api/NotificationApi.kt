package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 알림 API
 */
interface NotificationApi {

    /**
     * FCM 토큰 등록
     */
    @POST("api/v1/notifications/fcm-token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 알림 목록 조회
     */
    @GET("api/v1/notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<NotificationListResponse>>

    /**
     * 알림 읽음 처리
     */
    @PUT("api/v1/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 모든 알림 읽음 처리
     */
    @PUT("api/v1/notifications/read-all")
    suspend fun markAllAsRead(): Response<ApiResponse<Unit>>

    /**
     * 읽지 않은 알림 수 조회
     */
    @GET("api/v1/notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<Int>>
}
