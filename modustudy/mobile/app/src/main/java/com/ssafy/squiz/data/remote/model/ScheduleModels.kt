package com.ssafy.squiz.data.remote.model

import com.google.gson.annotations.SerializedName

// 일정 DTO (리스트용)
data class ScheduleDTO(
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("sessionId") val sessionId: Long,
    @SerializedName("studyName") val studyName: String,
    @SerializedName("date") val date: String,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String,
    @SerializedName("location") val location: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean = true,
    @SerializedName("meetingUrl") val meetingUrl: String? = null
)

// 세션 상세 DTO
data class SessionDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("studyName") val studyName: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("startTime") val startTime: String,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean = true,
    @SerializedName("meetingUrl") val meetingUrl: String? = null,
    @SerializedName("attendanceStatus") val attendanceStatus: String? = null,  // PRESENT, ABSENT, LATE, PENDING
    @SerializedName("isLeader") val isLeader: Boolean? = null
)

// 일정 목록 응답
data class ScheduleListResponse(
    @SerializedName("sessions") val sessions: List<ScheduleDTO>,
    @SerializedName("totalCount") val totalCount: Int
)

// Google 캘린더 동기화 상태
data class GoogleSyncStatus(
    @SerializedName("isConnected") val isConnected: Boolean,
    @SerializedName("email") val email: String? = null,
    @SerializedName("lastSyncTime") val lastSyncTime: String? = null,
    @SerializedName("autoSync") val autoSync: Boolean = false,
    @SerializedName("syncedCalendars") val syncedCalendars: List<CalendarInfo>? = null
)

data class CalendarInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String? = null
)

// 캘린더 데이터
data class CalendarData(
    @SerializedName("scheduledDays") val scheduledDays: List<Int>,
    @SerializedName("schedules") val schedules: List<ScheduleDTO>
)
