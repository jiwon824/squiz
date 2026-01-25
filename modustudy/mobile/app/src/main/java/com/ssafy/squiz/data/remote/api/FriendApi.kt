package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 친구 API
 * Base: /api/v1/friends
 */
interface FriendApi {

    /**
     * 친구 목록 조회
     */
    @GET("api/v1/friends")
    suspend fun getFriends(
        @Query("status") status: String? = null // online/offline/all
    ): Response<ApiResponse<FriendListResponse>>

    /**
     * 사용자 검색
     */
    @GET("api/v1/friends/search")
    suspend fun searchUsers(
        @Query("keyword") keyword: String
    ): Response<ApiResponse<List<UserSearchResult>>>

    /**
     * 친구 요청 보내기
     */
    @POST("api/v1/friends/request")
    suspend fun sendFriendRequest(
        @Body request: FriendRequestBody
    ): Response<ApiResponse<FriendRequestResponse>>

    /**
     * 받은 친구 요청 목록
     */
    @GET("api/v1/friends/requests/received")
    suspend fun getReceivedRequests(): Response<ApiResponse<List<ReceivedFriendRequest>>>

    /**
     * 보낸 친구 요청 목록
     */
    @GET("api/v1/friends/requests/sent")
    suspend fun getSentRequests(): Response<ApiResponse<List<SentFriendRequest>>>

    /**
     * 친구 요청 수락
     */
    @PUT("api/v1/friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(
        @Path("requestId") requestId: Long
    ): Response<ApiResponse<AcceptFriendResponse>>

    /**
     * 친구 요청 거절
     */
    @PUT("api/v1/friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(
        @Path("requestId") requestId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 친구 삭제
     */
    @DELETE("api/v1/friends/{friendId}")
    suspend fun deleteFriend(
        @Path("friendId") friendId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 사용자 차단
     */
    @POST("api/v1/friends/block/{userId}")
    suspend fun blockUser(
        @Path("userId") userId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 차단 해제
     */
    @DELETE("api/v1/friends/block/{userId}")
    suspend fun unblockUser(
        @Path("userId") userId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 차단 목록 조회
     */
    @GET("api/v1/friends/block")
    suspend fun getBlockedUsers(): Response<ApiResponse<List<BlockedUser>>>
}
