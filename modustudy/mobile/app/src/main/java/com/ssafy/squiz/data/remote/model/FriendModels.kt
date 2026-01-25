package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * 친구 목록 응답
 */
data class FriendListResponse(
    @SerializedName("online") val online: List<FriendItem>,
    @SerializedName("offline") val offline: List<FriendItem>,
    @SerializedName("totalCount") val totalCount: Int
) {
    // 편의 프로퍼티: 전체 친구 목록
    val friends: List<FriendItem>
        get() = online + offline
}

/**
 * 친구 아이템
 */
data class FriendItem(
    @SerializedName("friendshipId") val friendshipId: Long,
    @SerializedName("user") val user: FriendUser,
    @SerializedName("createdAt") val createdAt: String
) {
    // 편의 프로퍼티
    val friendId: Long get() = friendshipId
}

/**
 * 친구 사용자 정보
 */
data class FriendUser(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("statusMessage") val statusMessage: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("lastSeenAt") val lastSeenAt: String?
) {
    // 편의 프로퍼티
    val profileImageUrl: String? get() = profileImage
}

/**
 * 사용자 검색 결과
 */
data class UserSearchResult(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("statusMessage") val statusMessage: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean,
    @SerializedName("friendStatus") val friendStatus: FriendStatus
) {
    // 편의 프로퍼티
    val profileImageUrl: String? get() = profileImage
}

/**
 * 친구 관계 상태
 */
enum class FriendStatus {
    @SerializedName("NONE") NONE,
    @SerializedName("PENDING_SENT") PENDING_SENT,
    @SerializedName("PENDING_RECEIVED") PENDING_RECEIVED,
    @SerializedName("FRIEND") FRIEND,
    @SerializedName("BLOCKED") BLOCKED,
    @SerializedName("BLOCKED_BY") BLOCKED_BY
}

/**
 * 친구 요청 Body
 */
data class FriendRequestBody(
    @SerializedName("userId") val userId: Long
)

/**
 * 친구 요청 응답
 */
data class FriendRequestResponse(
    @SerializedName("requestId") val requestId: Long,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String
)

/**
 * 받은 친구 요청
 */
data class ReceivedFriendRequest(
    @SerializedName("requestId") val requestId: Long,
    @SerializedName("requester") val requester: FriendUser,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String
) {
    // 편의 프로퍼티
    val fromUser: FriendUser get() = requester
}

/**
 * 보낸 친구 요청
 */
data class SentFriendRequest(
    @SerializedName("requestId") val requestId: Long,
    @SerializedName("addressee") val addressee: FriendUser,
    @SerializedName("status") val status: String,
    @SerializedName("createdAt") val createdAt: String
) {
    // 편의 프로퍼티
    val toUser: FriendUser get() = addressee
}

/**
 * 친구 요청 수락 응답
 */
data class AcceptFriendResponse(
    @SerializedName("friendshipId") val friendshipId: Long
)

/**
 * 차단된 사용자
 */
data class BlockedUser(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String?,
    @SerializedName("blockedAt") val blockedAt: String
) {
    // 편의 프로퍼티
    val userId: Long get() = id
    val profileImageUrl: String? get() = profileImage
}
