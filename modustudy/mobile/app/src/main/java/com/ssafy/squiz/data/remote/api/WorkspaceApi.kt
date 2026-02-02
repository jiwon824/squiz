package com.ssafy.squiz.data.remote.api

import com.ssafy.squiz.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Workspace (스터디 채팅) API
 */
interface WorkspaceApi {

    /**
     * 스터디 워크스페이스 조회
     */
    @GET("api/v1/workspaces/study/{studyId}")
    suspend fun getWorkspaceByStudy(
        @Path("studyId") studyId: Long
    ): Response<ApiResponse<WorkspaceDTO>>

    /**
     * 메시지 생성
     */
    @POST("api/v1/workspaces/{workspaceId}/messages")
    suspend fun createMessage(
        @Path("workspaceId") workspaceId: Long,
        @Body request: MessageCreateRequest
    ): Response<ApiResponse<MessageDTO>>

    /**
     * 메시지 목록 조회 (페이징)
     */
    @GET("api/v1/workspaces/{workspaceId}/messages")
    suspend fun getMessages(
        @Path("workspaceId") workspaceId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<ApiResponse<MessagePageResponse>>

    /**
     * 최근 메시지 조회
     */
    @GET("api/v1/workspaces/{workspaceId}/messages/recent")
    suspend fun getRecentMessages(
        @Path("workspaceId") workspaceId: Long,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<List<MessageDTO>>>

    /**
     * 특정 시간 이후 메시지 조회 (폴링용)
     */
    @GET("api/v1/workspaces/{workspaceId}/messages/after")
    suspend fun getMessagesAfter(
        @Path("workspaceId") workspaceId: Long,
        @Query("after") after: String
    ): Response<ApiResponse<List<MessageDTO>>>

    /**
     * 메시지 검색
     */
    @GET("api/v1/workspaces/{workspaceId}/messages/search")
    suspend fun searchMessages(
        @Path("workspaceId") workspaceId: Long,
        @Query("keyword") keyword: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<MessagePageResponse>>

    /**
     * 메시지 수정
     */
    @PUT("api/v1/workspaces/{workspaceId}/messages/{messageId}")
    suspend fun updateMessage(
        @Path("workspaceId") workspaceId: Long,
        @Path("messageId") messageId: Long,
        @Body request: MessageUpdateRequest
    ): Response<ApiResponse<MessageDTO>>

    /**
     * 메시지 삭제
     */
    @DELETE("api/v1/workspaces/{workspaceId}/messages/{messageId}")
    suspend fun deleteMessage(
        @Path("workspaceId") workspaceId: Long,
        @Path("messageId") messageId: Long
    ): Response<ApiResponse<Unit>>

    /**
     * 메시지 고정/해제
     */
    @PATCH("api/v1/workspaces/{workspaceId}/messages/{messageId}/pin")
    suspend fun togglePinMessage(
        @Path("workspaceId") workspaceId: Long,
        @Path("messageId") messageId: Long
    ): Response<ApiResponse<MessageDTO>>

    /**
     * 고정된 메시지 목록
     */
    @GET("api/v1/workspaces/{workspaceId}/messages/pinned")
    suspend fun getPinnedMessages(
        @Path("workspaceId") workspaceId: Long
    ): Response<ApiResponse<List<MessageDTO>>>

    /**
     * 접속 중인 사용자 목록
     */
    @GET("api/v1/workspaces/{workspaceId}/presence")
    suspend fun getOnlineUsers(
        @Path("workspaceId") workspaceId: Long
    ): Response<ApiResponse<List<PresenceDTO>>>
}
