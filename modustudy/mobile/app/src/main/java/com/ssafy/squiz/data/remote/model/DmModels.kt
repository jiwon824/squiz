package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// ========== DM (1:1 채팅) 관련 모델 ==========

/**
 * DM 대화방 목록 DTO
 */
data class DmConversationDTO(
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("partnerId") val partnerId: Long,
    @SerializedName("partnerNickname") val partnerNickname: String,
    @SerializedName("partnerProfileImage") val partnerProfileImage: String? = null,
    @SerializedName("partnerIsOnline") val partnerIsOnline: Boolean = false,
    @SerializedName("lastMessage") val lastMessage: String? = null,
    @SerializedName("lastMessageIsMine") val lastMessageIsMine: Boolean = false,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("lastMessageAt") val lastMessageAt: String? = null
)

/**
 * DM 메시지 DTO
 */
data class DirectMessageDTO(
    @SerializedName("messageId") val messageId: Long,
    @SerializedName("conversationId") val conversationId: Long,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("senderNickname") val senderNickname: String,
    @SerializedName("senderProfileImage") val senderProfileImage: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("isMine") val isMine: Boolean = false,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * DM 전송 요청
 */
data class DirectMessageRequest(
    @SerializedName("receiverId") val receiverId: Long,
    @SerializedName("content") val content: String
)

/**
 * DM 메시지 페이지 응답
 */
data class DmMessagePageResponse(
    @SerializedName("content") val content: List<DirectMessageDTO>,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("first") val first: Boolean,
    @SerializedName("last") val last: Boolean
)

/**
 * 읽지 않은 메시지 수 응답
 */
data class UnreadCountResponse(
    @SerializedName("count") val count: Int
)
