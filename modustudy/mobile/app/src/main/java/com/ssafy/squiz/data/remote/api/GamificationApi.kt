package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 게이미피케이션 API
 */
interface GamificationApi {

    /**
     * 내 활동 통계 조회
     */
    @GET("api/v1/gamification/stats")
    suspend fun getMyStats(): Response<ApiResponse<GamificationStats>>

    /**
     * 잔디 그래프 조회
     */
    @GET("api/v1/gamification/contributions")
    suspend fun getContributions(
        @Query("year") year: Int,
        @Query("month") month: Int?
    ): Response<ApiResponse<ContributionResponse>>
}

// 잔디 응답
data class ContributionResponse(
    val year: Int,
    val month: Int?,
    val contributions: List<ContributionDay>
)

data class ContributionDay(
    val date: String,
    val activityCount: Int
)
