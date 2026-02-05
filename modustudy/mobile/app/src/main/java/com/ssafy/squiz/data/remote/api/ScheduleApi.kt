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
     * 백엔드는 StudySessionResponse를 직접 반환 (ApiResponse 래핑 없음)
     */
    @GET("api/v1/studies/{studyId}/sessions/{sessionId}")
    suspend fun getSessionDetail(
        @Path("studyId") studyId: Long,
        @Path("sessionId") sessionId: Long
    ): Response<StudySessionDTO>

    /**
     * Google 캘린더 동기화 상태 조회
     */
    @GET("api/v1/calendar/status")
    suspend fun getGoogleSyncStatus(): Response<ApiResponse<GoogleSyncStatus>>

    /**
     * Google 캘린더 연결 (Authorization Code로) - 모바일용
     * 모바일은 redirect_uri 없이 토큰 교환하므로 별도 엔드포인트 사용
     */
    @POST("api/v1/calendar/connect/mobile")
    suspend fun connectGoogleCalendar(
        @Body request: Map<String, String>
    ): Response<ApiResponse<GoogleSyncStatus>>

    /**
     * Google 캘린더 연결 해제
     */
    @POST("api/v1/calendar/disconnect")
    suspend fun disconnectGoogleCalendar(): Response<ApiResponse<Unit>>

    /**
     * 일정 동기화
     */
    @POST("api/v1/calendar/sync")
    suspend fun syncSchedules(): Response<ApiResponse<Unit>>

    /**
     * 모든 일정 통합 조회 (개인 + 스터디 + Google Calendar)
     * Google 캘린더 연동 시 Google 일정도 포함됨
     */
    @GET("api/v1/calendar/all")
    suspend fun getAllSchedules(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<ApiResponse<AllSchedulesResponse>>
}
