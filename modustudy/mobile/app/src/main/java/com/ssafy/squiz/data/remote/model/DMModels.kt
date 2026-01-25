package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * DM 대화 목록 아이템
 */
data class ConversationResponse(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("partner") val partner: DMPartner,
    @SerializedName("lastMessage") val lastMessageData: LastMessage?,
    @SerializedName("unreadCount") val unreadCount: Int,
    @SerializedName("updatedAt") val updatedAt: String
) {
    // 편의 프로퍼티
    val lastMessage: String? get() = lastMessageData?.content
    val lastMessageAt: String? get() = lastMessageData?.createdAt
}

/**
 * DM 상대방 정보
 */
data class DMPartner(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("lastSeenAt") val lastSeenAt: String? = null
) {
    // 편의 프로퍼티
    val profileImageUrl: String? get() = profileImage
}

/**
 * 마지막 메시지
 */
data class LastMessage(
    @SerializedName("id") val id: Long,
    @SerializedName("content") val content: String,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * 대화 상세 응답
 */
data class ConversationDetailResponse(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("partner") val partner: DMPartner,
    @SerializedName("messages") val messages: List<DMMessage>,
    @SerializedName("hasMore") val hasMore: Boolean,
    @SerializedName("nextCursor") val nextCursor: Long?
)

/**
 * DM 메시지
 */
data class DMMessage(
    @SerializedName("id") val id: Long,
    @SerializedName("content") val content: String,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * 새 대화 시작 요청
 */
data class StartConversationRequest(
    @SerializedName("partnerId") val partnerId: Long,
    @SerializedName("message") val message: String
)

/**
 * 새 대화 시작 응답
 */
data class StartConversationResponse(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("messageId") val messageId: Long,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * 메시지 전송 요청
 */
data class SendMessageRequest(
    @SerializedName("content") val content: String
)

/**
 * 메시지 전송 응답
 */
data class DMMessageResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("content") val content: String,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * 읽음 처리 응답
 */
data class ReadMessageResponse(
    @SerializedName("readCount") val readCount: Int,
    @SerializedName("lastReadMessageId") val lastReadMessageId: Long
)

/**
 * WebSocket 이벤트 타입
 */
enum class DMWebSocketEventType {
    NEW_MESSAGE,
    MESSAGE_READ,
    TYPING
}

/**
 * WebSocket 새 메시지 이벤트
 */
data class DMNewMessageEvent(
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: DMNewMessageData
)

data class DMNewMessageData(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("message") val message: DMMessage
)

/**
 * WebSocket 읽음 이벤트
 */
data class DMReadEvent(
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: DMReadData
)

data class DMReadData(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("readerId") val readerId: Long,
    @SerializedName("lastReadMessageId") val lastReadMessageId: Long
)

/**
 * WebSocket 타이핑 이벤트
 */
data class DMTypingEvent(
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: DMTypingData
)

data class DMTypingData(
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("userId") val userId: Long,
    @SerializedName("isTyping") val isTyping: Boolean
)

/**
 * 타이핑 전송 요청
 */
data class DMTypingRequest(
    @SerializedName("type") val type: String = "TYPING",
    @SerializedName("conversationId") val conversationId: String,
    @SerializedName("isTyping") val isTyping: Boolean
)
