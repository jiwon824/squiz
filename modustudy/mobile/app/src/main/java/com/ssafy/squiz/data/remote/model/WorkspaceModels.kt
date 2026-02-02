package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// ========== Workspace (스터디 채팅) 관련 모델 ==========

/**
 * 워크스페이스 DTO
 */
data class WorkspaceDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("createdAt") val createdAt: String? = null
)

/**
 * 메시지 DTO (백엔드 MessageDTO에 맞춤)
 */
data class MessageDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("workspaceId") val workspaceId: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("messageType") val messageType: String = "TEXT", // TEXT, IMAGE, FILE, SYSTEM
    @SerializedName("fileUrl") val fileUrl: String? = null,
    @SerializedName("isDeleted") val isDeleted: Boolean = false,
    @SerializedName("isPinned") val isPinned: Boolean = false,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

/**
 * 메시지 생성 요청
 */
data class MessageCreateRequest(
    @SerializedName("workspaceId") val workspaceId: Long,
    @SerializedName("content") val content: String,
    @SerializedName("messageType") val messageType: String = "TEXT",
    @SerializedName("fileUrl") val fileUrl: String? = null
)

/**
 * 메시지 수정 요청
 */
data class MessageUpdateRequest(
    @SerializedName("content") val content: String
)

/**
 * 메시지 페이지 응답
 */
data class MessagePageResponse(
    @SerializedName("content") val content: List<MessageDTO>,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("first") val first: Boolean,
    @SerializedName("last") val last: Boolean
)

/**
 * 접속 중인 사용자 정보
 */
data class PresenceDTO(
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null,
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null
)

/**
 * 메시지 타입 열거형
 */
enum class MessageType(val value: String) {
    TEXT("TEXT"),
    IMAGE("IMAGE"),
    FILE("FILE"),
    SYSTEM("SYSTEM")
}
