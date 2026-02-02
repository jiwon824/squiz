package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 일정 API
 */
interface ScheduleApi {

    /**
     * 일정 목록 조회
     */
    @GET("api/v1/schedules")
    suspend fun getSchedules(
        @Query("date") date: String? = null
    ): Response<ApiResponse<ScheduleListResponse>>

    /**
     * 오늘 일정 조회
     */
    @GET("api/v1/schedules/today")
    suspend fun getTodaySchedules(): Response<ApiResponse<List<ScheduleDTO>>>

    /**
     * 월별 일정 조회
     */
    @GET("api/v1/schedules/month")
    suspend fun getMonthSchedules(
        @Query("year") year: Int,
        @Query("month") month: Int
    ): Response<ApiResponse<CalendarData>>

    /**
     * 세션 상세 조회
     */
    @GET("api/v1/studies/{studyId}/sessions/{sessionId}")
    suspend fun getSessionDetail(
        @Path("studyId") studyId: Long,
        @Path("sessionId") sessionId: Long
    ): Response<ApiResponse<SessionDTO>>

    /**
     * Google 캘린더 동기화 상태 조회
     */
    @GET("api/v1/schedules/google-sync")
    suspend fun getGoogleSyncStatus(): Response<ApiResponse<GoogleSyncStatus>>

    /**
     * Google 캘린더 연결
     */
    @POST("api/v1/schedules/google-sync/connect")
    suspend fun connectGoogleCalendar(
        @Body token: Map<String, String>
    ): Response<ApiResponse<GoogleSyncStatus>>

    /**
     * Google 캘린더 연결 해제
     */
    @DELETE("api/v1/schedules/google-sync/disconnect")
    suspend fun disconnectGoogleCalendar(): Response<ApiResponse<Unit>>

    /**
     * 일정 동기화
     */
    @POST("api/v1/schedules/google-sync/sync")
    suspend fun syncSchedules(): Response<ApiResponse<Unit>>
}
