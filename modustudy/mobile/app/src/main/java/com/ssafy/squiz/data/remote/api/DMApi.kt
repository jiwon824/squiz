package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * DM (Direct Message) API
 * Base: /api/v1/dm
 */
interface DMApi {

    /**
     * DM 대화 목록 조회
     */
    @GET("api/v1/dm/conversations")
    suspend fun getConversations(): Response<ApiResponse<List<ConversationResponse>>>

    /**
     * 대화 메시지 조회
     */
    @GET("api/v1/dm/conversations/{conversationId}")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String,
        @Query("cursor") cursor: Long? = null,
        @Query("limit") limit: Int? = 50
    ): Response<ApiResponse<ConversationDetailResponse>>

    /**
     * 새 대화 시작
     */
    @POST("api/v1/dm/conversations")
    suspend fun startConversation(
        @Body request: StartConversationRequest
    ): Response<ApiResponse<StartConversationResponse>>

    /**
     * 메시지 전송
     */
    @POST("api/v1/dm/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequest
    ): Response<ApiResponse<DMMessageResponse>>

    /**
     * 메시지 읽음 처리
     */
    @PUT("api/v1/dm/conversations/{conversationId}/read")
    suspend fun markAsRead(
        @Path("conversationId") conversationId: String
    ): Response<ApiResponse<ReadMessageResponse>>

    /**
     * 대화 삭제
     */
    @DELETE("api/v1/dm/conversations/{conversationId}")
    suspend fun deleteConversation(
        @Path("conversationId") conversationId: String
    ): Response<ApiResponse<Unit>>

    /**
     * 메시지 삭제
     */
    @DELETE("api/v1/dm/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("messageId") messageId: Long
    ): Response<ApiResponse<Unit>>
}
