package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// 사용자 프로필 (백엔드 /api/v1/users/me 응답)
data class UserProfileDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("loginProvider") val loginProvider: String? = null,
    @SerializedName("role") val role: String? = null
)

// 게이미피케이션 통계 (백엔드 /api/v1/gamification/stats 응답)
data class GamificationStats(
    @SerializedName("level") val level: Int = 1,
    @SerializedName("levelName") val levelName: String? = null,
    @SerializedName("levelProgress") val levelProgress: LevelProgress? = null,
    @SerializedName("totalActivityDays") val totalActivityDays: Int = 0,
    @SerializedName("currentStreak") val currentStreak: Int = 0,
    @SerializedName("maxStreak") val maxStreak: Int = 0,
    @SerializedName("totalStudiesJoined") val totalStudiesJoined: Int = 0,
    @SerializedName("totalStudiesLed") val totalStudiesLed: Int = 0,
    @SerializedName("totalAttendance") val totalAttendance: Int = 0,
    @SerializedName("totalQuizCount") val totalQuizCount: Int = 0
)

data class LevelProgress(
    @SerializedName("current") val current: Int = 0,
    @SerializedName("required") val required: Int = 100,
    @SerializedName("percentage") val percentage: Double = 0.0
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

// 활동 통계 (백엔드 UserStatsResponse와 매칭)
data class ActivityStats(
    @SerializedName("totalActivityDays") val totalStudyDays: Int = 0,
    @SerializedName("currentStreak") val currentStreak: Int = 0,
    @SerializedName("totalAttendance") val attendanceRate: Int = 0,  // 출석 횟수를 사용
    @SerializedName("maxStreak") val totalStudyTime: Int = 0  // maxStreak를 표시
)

// 잔디 데이터 (백엔드 ContributionResponse와 매칭)
data class GrassData(
    @SerializedName("year") val year: Int,
    @SerializedName("month") val month: Int? = null,
    @SerializedName("contributions") val contributions: List<ContributionDay> = emptyList()
) {
    // UI에서 사용하는 days 형태로 변환
    val days: List<GrassDay>
        get() = contributions.mapIndexed { _, contribution ->
            val dayOfMonth = try {
                contribution.date?.substring(8, 10)?.toIntOrNull() ?: 0
            } catch (e: Exception) {
                0
            }
            // activityCount를 0~4 레벨로 변환
            val level = when {
                contribution.activityCount == 0 -> 0
                contribution.activityCount <= 2 -> 1
                contribution.activityCount <= 4 -> 2
                contribution.activityCount <= 6 -> 3
                else -> 4
            }
            GrassDay(day = dayOfMonth, level = level, count = contribution.activityCount)
        }
}

data class ContributionDay(
    @SerializedName("date") val date: String? = null,
    @SerializedName("hasActivity") val hasActivity: Boolean = false,
    @SerializedName("activityCount") val activityCount: Int = 0
)

data class GrassDay(
    val day: Int,
    val level: Int,  // 0~4
    val count: Int = 0
)

// 활동 상세 (백엔드 ContributionDetailResponse와 매칭)
data class ActivityDetail(
    @SerializedName("date") val date: String,
    @SerializedName("hasActivity") val hasActivity: Boolean = false,
    @SerializedName("activities") val activities: List<ActivityItem>
) {
    // UI에서 표시할 학습 시간 (활동 개수 기반)
    val studyTime: String
        get() = if (activities.isEmpty()) "0분" else "${activities.size}개 활동"
}

data class ActivityItem(
    @SerializedName("referenceId") val id: Long = 0,
    @SerializedName("type") val type: String,
    @SerializedName("referenceName") val title: String = "",
    @SerializedName("createdAt") val timestamp: String? = null
) {
    // 활동 타입에 따른 설명 생성
    val description: String?
        get() = when (type) {
            "ATTENDANCE" -> "스터디 출석"
            "QUIZ" -> "퀴즈 풀이"
            "CHAT" -> "채팅 참여"
            "MATERIAL_UPLOAD" -> "자료 업로드"
            "RETROSPECTIVE" -> "회고 작성"
            else -> null
        }
}

// 프로필 업데이트 요청
data class UpdateProfileRequest(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("bio") val bio: String? = null
)
