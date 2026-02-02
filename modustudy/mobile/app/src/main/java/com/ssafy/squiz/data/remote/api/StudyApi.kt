package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 스터디 API - 백엔드 연동
 */
interface StudyApi {

    /**
     * 전체 스터디 목록 조회
     */
    @GET("api/v1/study")
    suspend fun getStudies(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<StudyDTO>>

    /**
     * 모집중인 스터디 목록 조회
     */
    @GET("api/v1/study/recruiting")
    suspend fun getRecruitingStudies(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<StudyDTO>>

    /**
     * 내 스터디 목록 조회
     */
    @GET("api/v1/study/my")
    suspend fun getMyStudies(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<StudyDTO>>

    /**
     * 스터디 상세 조회
     */
    @GET("api/v1/study/{studyId}")
    suspend fun getStudyDetail(
        @Path("studyId") studyId: Long
    ): Response<StudyDetailDTO>

    /**
     * 스터디 검색
     */
    @GET("api/v1/study/search")
    suspend fun searchStudies(
        @Query("keyword") keyword: String? = null,
        @Query("topicId") topicId: Long? = null,
        @Query("meetingType") meetingType: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<StudyDTO>>

    /**
     * 추천 스터디 조회
     */
    @GET("api/v1/study/recommend")
    suspend fun getRecommendedStudies(
        @Query("limit") limit: Int = 10
    ): Response<List<StudyDTO>>

    /**
     * 스터디 지원
     */
    @POST("api/v1/study/{studyId}/applications")
    suspend fun applyToStudy(
        @Path("studyId") studyId: Long,
        @Body request: StudyApplicationRequest
    ): Response<Unit>

    /**
     * 스터디 찜하기/취소
     */
    @POST("api/v1/study/{studyId}/bookmark")
    suspend fun toggleBookmark(
        @Path("studyId") studyId: Long
    ): Response<Boolean>

    /**
     * 찜한 스터디 목록
     */
    @GET("api/v1/study/bookmarked")
    suspend fun getBookmarkedStudies(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<StudyDTO>>

    /**
     * 내 지원서 목록
     */
    @GET("api/v1/study/applications/my")
    suspend fun getMyApplications(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<PageResponse<StudyApplicationDTO>>
}
