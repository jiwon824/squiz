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
    @SerializedName("isLeader") val isLeader: Boolean? = null,
    // 출석 시간 체크용 필드
    @SerializedName("scheduledAt") val scheduledAt: String? = null,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null
)

// 일정 목록 응답
data class ScheduleListResponse(
    @SerializedName("sessions") val sessions: List<ScheduleDTO>,
    @SerializedName("totalCount") val totalCount: Int
)

// Google 캘린더 동기화 상태 (백엔드 CalendarStatusResponse와 매핑)
data class GoogleSyncStatus(
    @SerializedName("connected") val isConnected: Boolean = false,
    @SerializedName("email") val email: String? = null,
    @SerializedName("hasValidToken") val hasValidToken: Boolean = false,
    @SerializedName("calendarId") val calendarId: String? = null,
    // 하위 호환성
    @SerializedName("autoSync") val autoSync: Boolean = false
)

data class CalendarInfo(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String? = null
)

// 캘린더 데이터
data class CalendarData(
    @SerializedName("scheduledDays") val scheduledDays: List<Int>,
    @SerializedName("schedules") val schedules: List<ScheduleDTO>,
    @SerializedName("googleEvents") val googleEvents: List<GoogleCalendarEvent> = emptyList()
)

// Google 캘린더 이벤트
data class GoogleCalendarEvent(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String? = null,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String? = null,
    @SerializedName("location") val location: String? = null
)

// 모든 일정 통합 응답 (개인 + 스터디 + Google Calendar)
data class AllSchedulesResponse(
    @SerializedName("personal") val personal: List<PersonalSchedule> = emptyList(),
    @SerializedName("studySessions") val studySessions: List<StudySessionCalendar> = emptyList(),
    @SerializedName("googleEvents") val googleEvents: List<GoogleCalendarEvent> = emptyList()
)

// 개인 일정
data class PersonalSchedule(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("isAllDay") val isAllDay: Boolean = false
)

// 스터디 세션 (캘린더용)
data class StudySessionCalendar(
    @SerializedName("id") val id: Long,
    @SerializedName("studyId") val studyId: Long,
    @SerializedName("sessionNumber") val sessionNumber: Int? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("scheduledAt") val scheduledAt: String?,
    @SerializedName("durationMinutes") val durationMinutes: Int? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("isOnline") val isOnline: Boolean = true,
    @SerializedName("status") val status: String? = null
)
