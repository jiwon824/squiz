package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// BLE 출석 시작 요청
data class BleAttendanceStartRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("major") val major: Int,  // studyId
    @SerializedName("minor") val minor: Int   // sessionId
)

// BLE 출석 체크 요청
data class BleAttendanceCheckRequest(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("major") val major: Int,
    @SerializedName("minor") val minor: Int
)

// 출석 응답
data class AttendanceResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("status") val status: String,  // PRESENT, ABSENT, LATE, PENDING
    @SerializedName("checkedAt") val checkedAt: String? = null
)

// 출석 현황 DTO
data class AttendanceStatusDTO(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("sessionTitle") val sessionTitle: String,
    @SerializedName("sessionDate") val sessionDate: String,
    @SerializedName("status") val status: String,  // PRESENT, ABSENT, LATE, PENDING
    @SerializedName("checkedAt") val checkedAt: String? = null
)

// 출석 통계
data class AttendanceStatsDTO(
    @SerializedName("totalSessions") val totalSessions: Int = 0,
    @SerializedName("presentCount") val presentCount: Int = 0,
    @SerializedName("lateCount") val lateCount: Int = 0,
    @SerializedName("absentCount") val absentCount: Int = 0,
    @SerializedName("attendanceRate") val attendanceRate: Float = 0f
)

// 세션 멤버 출석 현황 (스터디장용)
data class SessionAttendanceDTO(
    @SerializedName("userId") val userId: Long,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("status") val status: String,  // PRESENT, ABSENT, LATE, PENDING
    @SerializedName("checkedAt") val checkedAt: String? = null
)

// 세션 출석 정보 (스터디장용)
data class SessionAttendanceInfoDTO(
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("sessionTitle") val sessionTitle: String,
    @SerializedName("totalMembers") val totalMembers: Int,
    @SerializedName("presentCount") val presentCount: Int,
    @SerializedName("members") val members: List<SessionAttendanceDTO>
)

// 수동 출석 상태 변경 요청 (스터디장용)
data class AttendanceManualUpdateRequest(
    @SerializedName("status") val status: String,  // PRESENT, ABSENT, LATE
    @SerializedName("reason") val reason: String? = null  // 사유 (선택)
)
