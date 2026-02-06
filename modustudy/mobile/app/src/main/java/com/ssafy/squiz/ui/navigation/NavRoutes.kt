package com.ssafy.squiz.ui.navigation

sealed class NavRoutes(val route: String) {
    // Auth
    object Splash : NavRoutes("splash")
    object Login : NavRoutes("login")
    object AdditionalInfo : NavRoutes("additional_info")

    // Main (Bottom Navigation)
    object Main : NavRoutes("main")

    // Home
    object Home : NavRoutes("home")
    object Notifications : NavRoutes("notifications")

    // Study Search
    object StudySearch : NavRoutes("study_search")
    object StudyDetail : NavRoutes("study_detail/{studyId}") {
        fun createRoute(studyId: Long) = "study_detail/$studyId"
    }
    object StudyComments : NavRoutes("study_comments/{studyId}") {
        fun createRoute(studyId: Long) = "study_comments/$studyId"
    }
    object BookmarkedStudies : NavRoutes("bookmarked_studies")
    object MyApplications : NavRoutes("my_applications")
    object StudyTemplates : NavRoutes("study_templates")
    object StudyCreate : NavRoutes("study_create")
    object StudyCreateLightning : NavRoutes("study_create_lightning")

    // My Studies
    object MyStudies : NavRoutes("my_studies")
    object StudyHome : NavRoutes("study_home/{studyId}") {
        fun createRoute(studyId: Long) = "study_home/$studyId"
    }
    object ChannelList : NavRoutes("channel_list/{studyId}") {
        fun createRoute(studyId: Long) = "channel_list/$studyId"
    }
    object Chat : NavRoutes("chat/{channelId}") {
        fun createRoute(channelId: Long) = "chat/$channelId"
    }
    object Materials : NavRoutes("materials/{studyId}") {
        fun createRoute(studyId: Long) = "materials/$studyId"
    }
    object MaterialDetail : NavRoutes("material_detail/{materialId}") {
        fun createRoute(materialId: Long) = "material_detail/$materialId"
    }
    object MaterialUpload : NavRoutes("material_upload/{studyId}") {
        fun createRoute(studyId: Long) = "material_upload/$studyId"
    }
    object Curriculum : NavRoutes("curriculum/{studyId}") {
        fun createRoute(studyId: Long) = "curriculum/$studyId"
    }
    object ProgressStatus : NavRoutes("progress_status/{studyId}") {
        fun createRoute(studyId: Long) = "progress_status/$studyId"
    }
    object TeamDashboard : NavRoutes("team_dashboard/{studyId}") {
        fun createRoute(studyId: Long) = "team_dashboard/$studyId"
    }
    object ApplicationManagement : NavRoutes("application_management/{studyId}") {
        fun createRoute(studyId: Long) = "application_management/$studyId"
    }
    object ExtendRecruitment : NavRoutes("extend_recruitment/{studyId}") {
        fun createRoute(studyId: Long) = "extend_recruitment/$studyId"
    }
    object TempChannel : NavRoutes("temp_channel/{studyId}") {
        fun createRoute(studyId: Long) = "temp_channel/$studyId"
    }
    object ConvertToOfficial : NavRoutes("convert_to_official/{studyId}") {
        fun createRoute(studyId: Long) = "convert_to_official/$studyId"
    }

    // BLE Attendance (모바일 전용)
    object AttendanceMember : NavRoutes("attendance_member/{studyId}/{sessionId}") {
        fun createRoute(studyId: Long, sessionId: Long) = "attendance_member/$studyId/$sessionId"
    }
    object AttendanceLeader : NavRoutes("attendance_leader/{studyId}/{sessionId}") {
        fun createRoute(studyId: Long, sessionId: Long) = "attendance_leader/$studyId/$sessionId"
    }
    object AttendanceSuccess : NavRoutes("attendance_success")
    object SelfAttendance : NavRoutes("self_attendance/{studyId}/{sessionId}") {
        fun createRoute(studyId: Long, sessionId: Long) = "self_attendance/$studyId/$sessionId"
    }
    object AttendanceCalendar : NavRoutes("attendance_calendar/{studyId}") {
        fun createRoute(studyId: Long) = "attendance_calendar/$studyId"
    }
    object SessionMemo : NavRoutes("session_memo/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_memo/$sessionId"
    }

    // 회의록 (모바일 전용 - 녹음 기반)
    object MeetingList : NavRoutes("meeting_list/{studyId}?sessionId={sessionId}&isLeader={isLeader}") {
        fun createRoute(studyId: Long, sessionId: Long? = null, isLeader: Boolean = false): String {
            val base = "meeting_list/$studyId"
            val params = mutableListOf<String>()
            if (sessionId != null) params.add("sessionId=$sessionId")
            params.add("isLeader=$isLeader")
            return if (params.isNotEmpty()) "$base?${params.joinToString("&")}" else base
        }
    }
    object MeetingDetail : NavRoutes("meeting_detail/{studyId}/{meetingId}") {
        fun createRoute(studyId: Long, meetingId: Long) = "meeting_detail/$studyId/$meetingId"
    }

    // Schedule
    object ScheduleList : NavRoutes("schedule_list")
    object ScheduleCalendar : NavRoutes("schedule_calendar")
    object ScheduleDetail : NavRoutes("schedule_detail/{studyId}/{sessionId}") {
        fun createRoute(studyId: Long, sessionId: Long) = "schedule_detail/$studyId/$sessionId"
    }
    object GoogleCalendarSync : NavRoutes("google_calendar_sync")

    // Quiz
    object QuizHome : NavRoutes("quiz_home")
    object QuizSolve : NavRoutes("quiz_solve/{quizId}") {
        fun createRoute(quizId: Long) = "quiz_solve/$quizId"
    }
    object QuizResult : NavRoutes("quiz_result/{attemptId}") {
        fun createRoute(attemptId: Long) = "quiz_result/$attemptId"
    }
    object WrongNotes : NavRoutes("wrong_notes")
    object WrongNoteReview : NavRoutes("wrong_note_review/{noteId}") {
        fun createRoute(noteId: Long) = "wrong_note_review/$noteId"
    }
    object ReviewSession : NavRoutes("review_session")

    // My Page
    object MyPage : NavRoutes("my_page")
    object EditProfile : NavRoutes("edit_profile")
    object NotificationSettings : NavRoutes("notification_settings")
    object MyActivity : NavRoutes("my_activity")
    object GrassGraph : NavRoutes("grass_graph")
    object ActivityDetail : NavRoutes("activity_detail/{date}") {
        fun createRoute(date: String) = "activity_detail/$date"
    }
    object PrivacySettings : NavRoutes("privacy_settings")

    // User Profile (스터디 멤버 프로필 조회용)
    object UserProfile : NavRoutes("user_profile/{userId}") {
        fun createRoute(userId: Long) = "user_profile/$userId"
    }

    // DM (1:1 채팅)
    object DmList : NavRoutes("dm_list")
    object DmChat : NavRoutes("dm_chat/{conversationId}/{partnerId}/{partnerNickname}") {
        fun createRoute(conversationId: Long, partnerId: Long, partnerNickname: String) =
            "dm_chat/$conversationId/$partnerId/${java.net.URLEncoder.encode(partnerNickname, "UTF-8")}"
    }
}
