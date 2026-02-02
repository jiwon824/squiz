package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 홈 API
 */
interface HomeApi {

    /**
     * 홈 데이터 조회
     */
    @GET("api/v1/home")
    suspend fun getHomeData(): Response<ApiResponse<HomeData>>

    /**
     * 추천 스터디 조회
     */
    @GET("api/v1/home/recommended")
    suspend fun getRecommendedStudies(): Response<ApiResponse<List<StudyDTO>>>

    /**
     * 인기 스터디 조회
     */
    @GET("api/v1/home/popular")
    suspend fun getPopularStudies(): Response<ApiResponse<List<StudyDTO>>>
}
