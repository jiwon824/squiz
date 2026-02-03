package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 일정 API
 */
interface ScheduleApi {

    /**
     * 일정 목록 조회 (내 스터디 세션)
     * 백엔드 엔드포인트: GET /api/v1/users/me/study-sessions
     */
    @GET("api/v1/users/me/study-sessions")
    suspend fun getSchedules(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<StudySessionDTO>>

    /**
     * 오늘 일정 조회 (내 스터디 세션)
     */
    @GET("api/v1/users/me/study-sessions")
    suspend fun getTodaySchedules(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<StudySessionDTO>>

    /**
     * 월별 일정 조회 (내 스터디 세션 - 해당 월의 시작일~종료일)
     */
    @GET("api/v1/users/me/study-sessions")
    suspend fun getMonthSchedules(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<List<StudySessionDTO>>

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
