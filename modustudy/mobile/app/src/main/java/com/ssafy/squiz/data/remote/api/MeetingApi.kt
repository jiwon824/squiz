package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 회의/녹음 API - 백엔드 연동
 * 백엔드 경로: /api/v1/studies/{studyId}/meetings/...
 */
interface MeetingApi {

    /**
     * 스터디의 회의 목록 조회
     */
    @GET("api/v1/studies/{studyId}/meetings")
    suspend fun getMeetings(
        @Path("studyId") studyId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<MeetingDTO>>>

    /**
     * 회의 상세 조회
     */
    @GET("api/v1/studies/{studyId}/meetings/{meetingId}")
    suspend fun getMeetingDetail(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long
    ): Response<ApiResponse<MeetingDetailDTO>>

    /**
     * 녹음 시작 (회의 생성)
     * POST /api/v1/studies/{studyId}/meetings
     */
    @POST("api/v1/studies/{studyId}/meetings")
    suspend fun startRecording(
        @Path("studyId") studyId: Long,
        @Body request: RecordingStartRequest
    ): Response<ApiResponse<MeetingDTO>>

    /**
     * 녹음 종료 (회의 종료)
     * PUT /api/v1/studies/{studyId}/meetings/{meetingId}/end
     */
    @PUT("api/v1/studies/{studyId}/meetings/{meetingId}/end")
    suspend fun endRecording(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long
    ): Response<ApiResponse<MeetingEndDTO>>

    /**
     * 오디오 파일 업로드
     * POST /api/v1/studies/{studyId}/meetings/{meetingId}/recording/audio
     * trackType: MIXED (전체 녹음) 또는 INDIVIDUAL (개인 녹음)
     */
    @Multipart
    @POST("api/v1/studies/{studyId}/meetings/{meetingId}/recording/audio")
    suspend fun uploadAudio(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long,
        @Part audio: MultipartBody.Part,
        @Query("trackType") trackType: String = "MIXED"
    ): Response<ApiResponse<AudioUploadResponse>>

    /**
     * 회의 요약 조회 (STT Summary)
     */
    @GET("api/v1/studies/{studyId}/meetings/{meetingId}/summary")
    suspend fun getMeetingSummary(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long
    ): Response<ApiResponse<MeetingSummaryDTO>>

    /**
     * 회의 전체 녹취록 조회
     */
    @GET("api/v1/studies/{studyId}/meetings/{meetingId}/transcripts")
    suspend fun getMeetingTranscripts(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long
    ): Response<ApiResponse<List<TranscriptItemDTO>>>

    /**
     * 회의 삭제
     */
    @DELETE("api/v1/studies/{studyId}/meetings/{meetingId}")
    suspend fun deleteMeeting(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 회의 퀴즈 목록 조회
     */
    @GET("api/v1/studies/{studyId}/meetings/{meetingId}/quizzes")
    suspend fun getMeetingQuizzes(
        @Path("studyId") studyId: Long,
        @Path("meetingId") meetingId: Long
    ): Response<ApiResponse<List<QuizDTO>>>
}
