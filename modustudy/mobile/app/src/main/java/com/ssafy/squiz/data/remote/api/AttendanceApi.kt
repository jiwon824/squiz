package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 출석 (BLE) API
 */
interface AttendanceApi {

    /**
     * BLE 출석 시작 (스터디장)
     */
    @POST("api/v1/studies/{studyId}/sessions/{sessionId}/attendance/ble/start")
    suspend fun startBleAttendance(
        @Path("studyId") studyId: Long,
        @Path("sessionId") sessionId: Long,
        @Body request: BleAttendanceStartRequest
    ): Response<ApiResponse<Unit>>

    /**
     * BLE 출석 체크 (멤버)
     */
    @POST("api/v1/studies/{studyId}/sessions/{sessionId}/attendance/ble/check")
    suspend fun checkBleAttendance(
        @Path("studyId") studyId: Long,
        @Path("sessionId") sessionId: Long,
        @Body request: BleAttendanceCheckRequest
    ): Response<ApiResponse<AttendanceResponse>>

    /**
     * 출석 현황 조회
     */
    @GET("api/v1/studies/{studyId}/attendance")
    suspend fun getAttendanceStatus(
        @Path("studyId") studyId: Long
    ): Response<ApiResponse<List<AttendanceStatusDTO>>>

    /**
     * 출석 통계 조회
     */
    @GET("api/v1/studies/{studyId}/attendance/stats")
    suspend fun getAttendanceStats(
        @Path("studyId") studyId: Long
    ): Response<ApiResponse<AttendanceStatsDTO>>

    /**
     * 세션 출석 정보 조회 (스터디장용)
     */
    @GET("api/v1/studies/{studyId}/sessions/{sessionId}/attendance")
    suspend fun getSessionAttendance(
        @Path("studyId") studyId: Long,
        @Path("sessionId") sessionId: Long
    ): Response<ApiResponse<SessionAttendanceInfoDTO>>
}
