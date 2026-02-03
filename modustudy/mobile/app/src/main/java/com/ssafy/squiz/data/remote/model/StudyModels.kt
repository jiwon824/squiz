package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// 스터디 목록 응답
data class StudyDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("topic") val topic: TopicInfo? = null,
    @SerializedName("format") val format: FormatInfo? = null,
    @SerializedName("leader") val leader: LeaderInfo? = null,
    @SerializedName("currentMembers") val currentMembers: Int? = null,
    @SerializedName("maxMembers") val maxMembers: Int? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("recruitEndDate") val recruitEndDate: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("isBookmarked") val isBookmarked: Boolean? = null,
    @SerializedName("isLeader") val isLeader: Boolean? = null,
    @SerializedName("progress") val progress: Int? = null,
    @SerializedName("nextMeeting") val nextMeeting: String? = null
) {
    // 편의 프로퍼티
    val topicName: String? get() = topic?.name
    val meetingType: String? get() = format?.name
    val leaderId: Long? get() = leader?.id
    val leaderNickname: String? get() = leader?.nickname
}

// 스터디 상세 응답
data class StudyDetailDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("topic") val topic: TopicInfo? = null,
    @SerializedName("format") val format: FormatInfo? = null,
    @SerializedName("leader") val leader: LeaderInfo? = null,
    @SerializedName("currentMembers") val currentMembers: Int? = null,
    @SerializedName("maxMembers") val maxMembers: Int? = null,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("recruitEndDate") val recruitEndDate: String? = null,
    @SerializedName("tags") val tags: List<String>? = null,
    @SerializedName("rules") val rules: String? = null,
    @SerializedName("schedule") val schedule: String? = null,
    @SerializedName("scheduleSummary") val scheduleSummary: String? = null,
    @SerializedName("processDetail") val processDetail: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("members") val members: List<StudyMemberDTO>? = null,
    @SerializedName("isBookmarked") val isBookmarked: Boolean? = null,
    @SerializedName("isMember") val isMember: Boolean? = null,
    @SerializedName("isLeader") val isLeader: Boolean? = null
) {
    val topicName: String? get() = topic?.name
    val meetingType: String? get() = format?.name
}

// 토픽 정보
data class TopicInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("parent") val parent: ParentTopicInfo? = null
)

data class ParentTopicInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String
)

// 포맷 정보 (온라인/오프라인/혼합)
data class FormatInfo(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("name") val name: String
)

// 스터디장 정보
data class LeaderInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("role") val role: String? = null
)

// 스터디 멤버
data class StudyMemberDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("joinedAt") val joinedAt: String? = null
)

// 스터디 지원
data class StudyApplicationRequest(
    @SerializedName("message") val message: String
)

data class StudyApplicationDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)

// 페이징 응답 (Spring Page 형식)
data class PageResponse<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("size") val size: Int,
    @SerializedName("first") val first: Boolean,
    @SerializedName("last") val last: Boolean
)

// 토픽 목록
data class TopicDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("children") val children: List<TopicDTO>? = null
)

// ========== 스터디 생성 요청 ==========

/**
 * 스터디 생성 요청 DTO
 * 백엔드 StudyCreateRequest에 맞춤
 */
data class StudyCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("intro") val intro: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("topicId") val topicId: Long,
    @SerializedName("formatId") val formatId: Long? = null,
    @SerializedName("studyType") val studyType: String = "PLANNED", // PLANNED, LIGHTNING
    @SerializedName("meetingType") val meetingType: String = "ONLINE", // ONLINE, OFFLINE, HYBRID
    @SerializedName("regionId") val regionId: Long? = null,
    @SerializedName("locationDetail") val locationDetail: String? = null,
    @SerializedName("scheduleSummary") val scheduleSummary: String? = null,
    @SerializedName("scheduleDays") val scheduleDays: String? = null, // 쉼표 구분 문자열
    @SerializedName("scheduleTime") val scheduleTime: String? = null, // HH:mm
    @SerializedName("maxMembers") val maxMembers: Int = 4,
    @SerializedName("isPublic") val isPublic: Boolean = true,
    @SerializedName("penaltyPolicy") val penaltyPolicy: String = "NORMAL",
    @SerializedName("startDate") val startDate: String, // yyyy-MM-dd
    @SerializedName("endDate") val endDate: String, // yyyy-MM-dd
    @SerializedName("totalSessions") val totalSessions: Int? = null,
    @SerializedName("recruitStartDate") val recruitStartDate: String? = null,
    @SerializedName("recruitEndDate") val recruitEndDate: String? = null,
    @SerializedName("textbook") val textbook: String? = null,
    @SerializedName("goal") val goal: String? = null,
    @SerializedName("difficulty") val difficulty: String = "BEGINNER", // BEGINNER, INTERMEDIATE, ADVANCED
    @SerializedName("prerequisites") val prerequisites: String? = null,
    @SerializedName("processDetail") val processDetail: String? = null,
    @SerializedName("status") val status: String? = null // RECRUITING, PENDING 등
)

// 난이도 열거형
enum class StudyDifficulty(val label: String) {
    BEGINNER("입문"),
    INTERMEDIATE("중급"),
    ADVANCED("고급")
}

// 진행 방식 열거형
enum class MeetingTypeEnum(val label: String) {
    ONLINE("온라인"),
    OFFLINE("오프라인"),
    HYBRID("혼합")
}

// 스터디 타입 열거형
enum class StudyTypeEnum(val label: String) {
    PLANNED("일반 스터디"),
    LIGHTNING("번개 스터디")
}

// 스터디 세션 DTO
data class StudySessionDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("sessionNumber") val sessionNumber: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("scheduledAt") val scheduledAt: String? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean? = true,
    @SerializedName("status") val status: String? = null, // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("studyName") val studyName: String? = null
) {
    // ScheduleDTO로 변환 (UI 호환성)
    fun toScheduleDTO(): ScheduleDTO {
        val dateTime = scheduledAt?.let {
            try {
                java.time.LocalDateTime.parse(it)
            } catch (e: Exception) {
                null
            }
        }
        val date = dateTime?.toLocalDate()?.toString() ?: ""
        val startTime = dateTime?.toLocalTime()?.toString()?.take(5) ?: ""
        val endTime = durationMinutes?.let { minutes ->
            dateTime?.plusMinutes(minutes.toLong())?.toLocalTime()?.toString()?.take(5)
        } ?: ""

        return ScheduleDTO(
            studyId = studyId,
            sessionId = id,
            studyName = studyName ?: "스터디 #$studyId",
            date = date,
            startTime = startTime,
            endTime = endTime,
            location = location,
            isOnline = isOnline ?: true,
            meetingUrl = null
        )
    }

    // SessionDTO로 변환 (세션 상세 화면용)
    fun toSessionDTO(studyName: String? = null, isLeader: Boolean = false): SessionDTO {
        val dateTime = scheduledAt?.let {
            try {
                java.time.LocalDateTime.parse(it)
            } catch (e: Exception) {
                // 다른 형식 시도 (예: "2025-02-14T09:00:00.000")
                try {
                    java.time.LocalDateTime.parse(it.take(19))
                } catch (e2: Exception) {
                    null
                }
            }
        }
        val startTimeStr = dateTime?.toLocalTime()?.toString()?.take(5) ?: ""
        val endTimeStr = durationMinutes?.let { minutes ->
            dateTime?.plusMinutes(minutes.toLong())?.toLocalTime()?.toString()?.take(5)
        } ?: ""

        return SessionDTO(
            id = id,
            studyId = studyId,
            studyName = studyName ?: this.studyName ?: "스터디 #$studyId",
            title = title,
            description = description,
            startTime = startTimeStr,
            endTime = endTimeStr,
            location = location,
            isOnline = isOnline ?: true,
            meetingUrl = null,
            attendanceStatus = null,
            isLeader = isLeader
        )
    }
}
