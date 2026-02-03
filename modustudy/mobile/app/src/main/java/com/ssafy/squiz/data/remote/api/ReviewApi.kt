package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 복습 (FSRS) API
 */
interface ReviewApi {

    /**
     * 오늘 복습 예정 카드 조회
     */
    @GET("api/v1/reviews/today")
    suspend fun getTodayReviews(): Response<ApiResponse<TodayReviewResponse>>

    /**
     * 틀린 문제 조회
     * @param sortType MOST_WRONG (많이 틀린 순) 또는 FSRS_RECOMMENDED (복습 우선순위)
     */
    @GET("api/v1/reviews/wrong-answers")
    suspend fun getWrongAnswers(
        @Query("sortType") sortType: String? = null
    ): Response<ApiResponse<TodayReviewResponse>>

    /**
     * 복습 결과 제출
     */
    @POST("api/v1/reviews")
    suspend fun submitReview(
        @Body request: ReviewSubmitRequest
    ): Response<ApiResponse<Unit>>

    /**
     * 복습 통계 조회
     */
    @GET("api/v1/reviews/stats")
    suspend fun getReviewStats(): Response<ApiResponse<ReviewStatsResponse>>

    /**
     * 퀴즈 목록 조회
     */
    @GET("api/v1/quizzes")
    suspend fun getQuizzes(
        @Query("studyId") studyId: Long? = null
    ): Response<ApiResponse<List<QuizDTO>>>

    /**
     * 퀴즈 상세 조회
     */
    @GET("api/v1/quizzes/{quizId}")
    suspend fun getQuizDetail(
        @Path("quizId") quizId: Long
    ): Response<ApiResponse<QuizDetailDTO>>

    /**
     * 퀴즈 결과 제출
     */
    @POST("api/v1/quizzes/{quizId}/submit")
    suspend fun submitQuizResult(
        @Path("quizId") quizId: Long,
        @Body answers: List<Int>
    ): Response<ApiResponse<QuizResultDTO>>
}
