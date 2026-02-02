package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 회의/녹음 API - 백엔드 연동
 */
interface MeetingApi {

    /**
     * 스터디의 회의 목록 조회
     */
    @GET("api/v1/meetings/study/{studyId}")
    suspend fun getMeetings(
        @Path("studyId") studyId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<MeetingDTO>>

    /**
     * 회의 상세 조회
     */
    @GET("api/v1/meetings/{meetingId}")
    suspend fun getMeetingDetail(
        @Path("meetingId") meetingId: Long
    ): Response<MeetingDetailDTO>

    /**
     * 녹음 시작 (회의 생성)
     */
    @POST("api/v1/meetings/start")
    suspend fun startRecording(
        @Body request: RecordingStartRequest
    ): Response<RecordingStartResponse>

    /**
     * 녹음 종료
     */
    @POST("api/v1/meetings/end")
    suspend fun endRecording(
        @Body request: RecordingEndRequest
    ): Response<MeetingDTO>

    /**
     * 오디오 파일 업로드
     */
    @Multipart
    @POST("api/v1/meetings/{meetingId}/audio")
    suspend fun uploadAudio(
        @Path("meetingId") meetingId: Long,
        @Part audio: MultipartBody.Part
    ): Response<AudioUploadResponse>

    /**
     * 회의 요약 조회
     */
    @GET("api/v1/meetings/{meetingId}/summary")
    suspend fun getMeetingSummary(
        @Path("meetingId") meetingId: Long
    ): Response<MeetingSummaryDTO>

    /**
     * 회의 전체 녹취록 조회
     */
    @GET("api/v1/meetings/{meetingId}/transcript")
    suspend fun getMeetingTranscript(
        @Path("meetingId") meetingId: Long
    ): Response<String>

    /**
     * 회의 삭제
     */
    @DELETE("api/v1/meetings/{meetingId}")
    suspend fun deleteMeeting(
        @Path("meetingId") meetingId: Long
    ): Response<Unit>

    /**
     * 회의 상태 확인 (처리 진행률)
     */
    @GET("api/v1/meetings/{meetingId}/status")
    suspend fun getMeetingStatus(
        @Path("meetingId") meetingId: Long
    ): Response<MeetingDTO>
}
