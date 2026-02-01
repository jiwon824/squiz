package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// 사용자 프로필
data class UserProfileDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("studyCount") val studyCount: Int = 0,
    @SerializedName("attendanceRate") val attendanceRate: Int = 0,
    @SerializedName("quizScore") val quizScore: Int = 0,
    @SerializedName("createdAt") val createdAt: String? = null
)

// 알림 설정
data class NotificationSettings(
    @SerializedName("pushEnabled") val pushEnabled: Boolean = true,
    @SerializedName("studyAlertEnabled") val studyAlertEnabled: Boolean = true,
    @SerializedName("chatAlertEnabled") val chatAlertEnabled: Boolean = true,
    @SerializedName("friendAlertEnabled") val friendAlertEnabled: Boolean = true
)

// 개인정보 설정
data class PrivacySettings(
    @SerializedName("profilePublic") val profilePublic: Boolean = true,
    @SerializedName("activityPublic") val activityPublic: Boolean = true
)

// 활동 통계
data class ActivityStats(
    @SerializedName("totalStudyDays") val totalStudyDays: Int = 0,
    @SerializedName("currentStreak") val currentStreak: Int = 0,
    @SerializedName("attendanceRate") val attendanceRate: Int = 0,
    @SerializedName("totalStudyTime") val totalStudyTime: Int = 0
)

// 잔디 데이터
data class GrassData(
    @SerializedName("year") val year: Int,
    @SerializedName("month") val month: Int,
    @SerializedName("days") val days: List<GrassDay>
)

data class GrassDay(
    @SerializedName("day") val day: Int,
    @SerializedName("level") val level: Int,  // 0~4
    @SerializedName("count") val count: Int = 0
)

// 활동 상세
data class ActivityDetail(
    @SerializedName("date") val date: String,
    @SerializedName("studyTime") val studyTime: String,
    @SerializedName("activities") val activities: List<ActivityItem>
)

data class ActivityItem(
    @SerializedName("id") val id: Long,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

// 프로필 업데이트 요청
data class UpdateProfileRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("bio") val bio: String? = null
)
