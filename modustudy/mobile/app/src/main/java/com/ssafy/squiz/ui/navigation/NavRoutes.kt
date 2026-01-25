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

    // BLE Attendance
    object AttendanceMember : NavRoutes("attendance_member/{sessionId}") {
        fun createRoute(sessionId: Long) = "attendance_member/$sessionId"
    }
    object AttendanceLeader : NavRoutes("attendance_leader/{sessionId}") {
        fun createRoute(sessionId: Long) = "attendance_leader/$sessionId"
    }
    object AttendanceSuccess : NavRoutes("attendance_success")
    object SelfAttendance : NavRoutes("self_attendance/{sessionId}") {
        fun createRoute(sessionId: Long) = "self_attendance/$sessionId"
    }
    object AttendanceCalendar : NavRoutes("attendance_calendar/{studyId}") {
        fun createRoute(studyId: Long) = "attendance_calendar/$studyId"
    }
    object SessionMemo : NavRoutes("session_memo/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_memo/$sessionId"
    }

    // Schedule
    object ScheduleList : NavRoutes("schedule_list")
    object ScheduleCalendar : NavRoutes("schedule_calendar")
    object ScheduleDetail : NavRoutes("schedule_detail/{sessionId}") {
        fun createRoute(sessionId: Long) = "schedule_detail/$sessionId"
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

    // Quiz Contest
    object ContestList : NavRoutes("contest_list")
    object ContestWaiting : NavRoutes("contest_waiting/{contestId}") {
        fun createRoute(contestId: Long) = "contest_waiting/$contestId"
    }
    object ContestPlay : NavRoutes("contest_play/{contestId}") {
        fun createRoute(contestId: Long) = "contest_play/$contestId"
    }
    object ContestResult : NavRoutes("contest_result/{contestId}") {
        fun createRoute(contestId: Long) = "contest_result/$contestId"
    }
    object ContestHistory : NavRoutes("contest_history")
    object ContestResultDetail : NavRoutes("contest_result_detail/{contestId}") {
        fun createRoute(contestId: Long) = "contest_result_detail/$contestId"
    }
    object MyContestRecords : NavRoutes("my_contest_records")

    // Quiz Course
    object CourseList : NavRoutes("course_list")
    object CourseDetail : NavRoutes("course_detail/{courseId}") {
        fun createRoute(courseId: Long) = "course_detail/$courseId"
    }
    object SectionSolve : NavRoutes("section_solve/{sectionId}") {
        fun createRoute(sectionId: Long) = "section_solve/$sectionId"
    }
    object SectionResult : NavRoutes("section_result/{attemptId}") {
        fun createRoute(attemptId: Long) = "section_result/$attemptId"
    }
    object MyCourseProgress : NavRoutes("my_course_progress")

    // Daily & Retrospective
    object DailyReport : NavRoutes("daily_report/{studyId}") {
        fun createRoute(studyId: Long) = "daily_report/$studyId"
    }
    object DailyHistory : NavRoutes("daily_history/{studyId}") {
        fun createRoute(studyId: Long) = "daily_history/$studyId"
    }
    object RetrospectiveList : NavRoutes("retrospective_list/{studyId}") {
        fun createRoute(studyId: Long) = "retrospective_list/$studyId"
    }
    object RetrospectiveWrite : NavRoutes("retrospective_write/{studyId}") {
        fun createRoute(studyId: Long) = "retrospective_write/$studyId"
    }

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

    // Friends
    object FriendList : NavRoutes("friend_list")
    object FriendSearch : NavRoutes("friend_search")
    object FriendRequests : NavRoutes("friend_requests")
    object UserProfile : NavRoutes("user_profile/{userId}") {
        fun createRoute(userId: Long) = "user_profile/$userId"
    }
    object BlockedUsers : NavRoutes("blocked_users")

    // DM
    object DMList : NavRoutes("dm_list")
    object DMChat : NavRoutes("dm_chat/{chatId}") {
        fun createRoute(chatId: Long) = "dm_chat/$chatId"
    }

    // AI
    object AIChatbot : NavRoutes("ai_chatbot/{studyId}") {
        fun createRoute(studyId: Long) = "ai_chatbot/$studyId"
    }
    object AIRecommendation : NavRoutes("ai_recommendation")
}
