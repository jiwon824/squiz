package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// 알림 DTO
data class NotificationDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,  // STUDY_APPLICATION, SCHEDULE, CHAT, QUIZ, FRIEND 등
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("isRead") val isRead: Boolean = false,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("data") val data: NotificationData? = null
)

// 알림 추가 데이터
data class NotificationData(
    @SerializedName("studyId") val studyId: Long? = null,
    @SerializedName("sessionId") val sessionId: Long? = null,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("link") val link: String? = null
)

// 알림 목록 응답
data class NotificationListResponse(
    @SerializedName("notifications") val notifications: List<NotificationDTO>,
    @SerializedName("unreadCount") val unreadCount: Int,
    @SerializedName("totalCount") val totalCount: Int
)

// FCM 토큰 등록 요청
data class FcmTokenRequest(
    @SerializedName("token") val token: String,
    @SerializedName("deviceType") val deviceType: String = "ANDROID"
)
