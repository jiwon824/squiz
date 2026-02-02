package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * DM (1:1 채팅) API
 */
interface DmApi {

    /**
     * DM 메시지 전송
     */
    @POST("api/v1/dm")
    suspend fun sendMessage(
        @Body request: DirectMessageRequest
    ): Response<ApiResponse<DirectMessageDTO>>

    /**
     * DM 대화방 목록 조회
     */
    @GET("api/v1/dm/conversations")
    suspend fun getConversations(): Response<ApiResponse<List<DmConversationDTO>>>

    /**
     * 대화방 메시지 목록 조회
     */
    @GET("api/v1/dm/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<ApiResponse<List<DirectMessageDTO>>>

    /**
     * 메시지 읽음 처리
     */
    @POST("api/v1/dm/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("conversationId") conversationId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 대화방 삭제
     */
    @DELETE("api/v1/dm/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 읽지 않은 메시지 총 개수
     */
    @GET("api/v1/dm/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountResponse>>
}
