package com.ssafy.squiz.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ssafy.squiz.base.SquizApplication
import com.ssafy.squiz.ui.screens.auth.*
import com.ssafy.squiz.ui.screens.home.*
import com.ssafy.squiz.ui.screens.study.*
import com.ssafy.squiz.ui.screens.mystudy.*
import com.ssafy.squiz.ui.screens.attendance.*
import com.ssafy.squiz.ui.screens.schedule.*
import com.ssafy.squiz.ui.screens.quiz.*
import com.ssafy.squiz.ui.screens.mypage.*
import com.ssafy.squiz.ui.screens.meeting.*
import com.ssafy.squiz.ui.screens.dm.*
import com.ssafy.squiz.ui.screens.main.MainScreen
import java.net.URLDecoder

@Composable
fun SquizNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.Splash.route,
        modifier = modifier
    ) {
        // Auth
        composable(NavRoutes.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Login.route) {
            val loginViewModel = remember {
                val app = SquizApplication.getInstance()
                LoginViewModel(app.authRepository)
            }

            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToMain = {
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToAdditionalInfo = {
                    navController.navigate(NavRoutes.AdditionalInfo.route)
                }
            )
        }

        composable(NavRoutes.AdditionalInfo.route) {
            AdditionalInfoScreen(
                onNavigateToMain = {
                    navController.navigate(NavRoutes.Main.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Main (with Bottom Navigation)
        composable(NavRoutes.Main.route) {
            MainScreen(
                onNavigateToStudyDetail = { studyId ->
                    navController.navigate(NavRoutes.StudyDetail.createRoute(studyId))
                },
                onNavigateToStudySearch = {
                    navController.navigate(NavRoutes.StudySearch.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(NavRoutes.Notifications.route)
                },
                onNavigateToDmList = {
                    navController.navigate(NavRoutes.DmList.route)
                },
                onNavigateToStudyHome = { studyId ->
                    navController.navigate(NavRoutes.StudyHome.createRoute(studyId))
                },
                onNavigateToQuizSolve = { quizId ->
                    navController.navigate(NavRoutes.QuizSolve.createRoute(quizId))
                },
                onNavigateToWrongNotes = {
                    navController.navigate(NavRoutes.WrongNotes.route)
                },
                onNavigateToReviewSession = {
                    navController.navigate(NavRoutes.ReviewSession.route)
                },
                onNavigateToEditProfile = {
                    navController.navigate(NavRoutes.EditProfile.route)
                },
                onNavigateToMyActivity = {
                    navController.navigate(NavRoutes.MyActivity.route)
                },
                onNavigateToBookmarkedStudies = {
                    navController.navigate(NavRoutes.BookmarkedStudies.route)
                },
                onNavigateToMyApplications = {
                    navController.navigate(NavRoutes.MyApplications.route)
                },
                onNavigateToStudyTemplates = {
                    navController.navigate(NavRoutes.StudyTemplates.route)
                },
                onNavigateToScheduleList = {
                    navController.navigate(NavRoutes.ScheduleList.route)
                },
                onNavigateToStudyCreate = {
                    navController.navigate(NavRoutes.StudyCreate.route)
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Main.route) { inclusive = true }
                    }
                }
            )
        }

        // Home
        composable(NavRoutes.Notifications.route) {
            NotificationsScreen(onBackClick = { navController.popBackStack() })
        }

        // Study Search
        composable(NavRoutes.StudySearch.route) {
            StudySearchScreen(
                onBackClick = { navController.popBackStack() },
                onStudyClick = { studyId ->
                    navController.navigate(NavRoutes.StudyDetail.createRoute(studyId))
                }
            )
        }

        composable(
            route = NavRoutes.StudyDetail.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            StudyDetailScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onNavigateToComments = {
                    navController.navigate(NavRoutes.StudyComments.createRoute(studyId))
                }
            )
        }

        composable(
            route = NavRoutes.StudyComments.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            StudyCommentsScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.BookmarkedStudies.route) {
            BookmarkedStudiesScreen(
                onBackClick = { navController.popBackStack() },
                onStudyClick = { studyId ->
                    navController.navigate(NavRoutes.StudyDetail.createRoute(studyId))
                }
            )
        }

        composable(NavRoutes.MyApplications.route) {
            MyApplicationsScreen(
                onBackClick = { navController.popBackStack() },
                onStudyClick = { studyId ->
                    navController.navigate(NavRoutes.StudyDetail.createRoute(studyId))
                }
            )
        }

        composable(NavRoutes.StudyTemplates.route) {
            StudyTemplatesScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // 스터디 생성
        composable(NavRoutes.StudyCreate.route) {
            StudyCreateScreen(
                onBackClick = { navController.popBackStack() },
                onCreateSuccess = { studyId ->
                    navController.navigate(NavRoutes.StudyDetail.createRoute(studyId)) {
                        popUpTo(NavRoutes.StudyCreate.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.StudyCreateLightning.route) {
            StudyCreateScreen(
                onBackClick = { navController.popBackStack() },
                onCreateSuccess = { studyId ->
                    navController.navigate(NavRoutes.StudyDetail.createRoute(studyId)) {
                        popUpTo(NavRoutes.StudyCreateLightning.route) { inclusive = true }
                    }
                }
            )
        }

        // My Studies - Study Home
        composable(
            route = NavRoutes.StudyHome.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            StudyHomeScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onNavigateToChannelList = {
                    navController.navigate(NavRoutes.ChannelList.createRoute(studyId))
                },
                onNavigateToMaterials = {
                    navController.navigate(NavRoutes.Materials.createRoute(studyId))
                },
                onNavigateToCurriculum = {
                    navController.navigate(NavRoutes.Curriculum.createRoute(studyId))
                },
                onNavigateToProgressStatus = {
                    navController.navigate(NavRoutes.ProgressStatus.createRoute(studyId))
                },
                onNavigateToTeamDashboard = {
                    navController.navigate(NavRoutes.TeamDashboard.createRoute(studyId))
                },
                onNavigateToApplicationManagement = {
                    navController.navigate(NavRoutes.ApplicationManagement.createRoute(studyId))
                },
                onNavigateToAttendanceCalendar = {
                    navController.navigate(NavRoutes.AttendanceCalendar.createRoute(studyId))
                },
                onNavigateToExtendRecruitment = {
                    navController.navigate(NavRoutes.ExtendRecruitment.createRoute(studyId))
                },
                onNavigateToTempChannel = {
                    navController.navigate(NavRoutes.TempChannel.createRoute(studyId))
                },
                onNavigateToConvertToOfficial = {
                    navController.navigate(NavRoutes.ConvertToOfficial.createRoute(studyId))
                },
                onNavigateToMeetingList = {
                    navController.navigate(NavRoutes.MeetingList.createRoute(studyId))
                },
                onNavigateToAttendance = { sId, sessionId, isLeader ->
                    if (isLeader) {
                        navController.navigate(NavRoutes.AttendanceLeader.createRoute(sId, sessionId))
                    } else {
                        navController.navigate(NavRoutes.AttendanceMember.createRoute(sId, sessionId))
                    }
                }
            )
        }

        composable(
            route = NavRoutes.ChannelList.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            ChannelListScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onChannelClick = { channelId ->
                    navController.navigate(NavRoutes.Chat.createRoute(channelId))
                }
            )
        }

        composable(
            route = NavRoutes.Chat.route,
            arguments = listOf(navArgument("channelId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("channelId") ?: 0L
            ChatScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.Materials.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            MaterialsScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onMaterialClick = { materialId ->
                    navController.navigate(NavRoutes.MaterialDetail.createRoute(materialId))
                },
                onUploadClick = {
                    navController.navigate(NavRoutes.MaterialUpload.createRoute(studyId))
                }
            )
        }

        composable(
            route = NavRoutes.MaterialDetail.route,
            arguments = listOf(navArgument("materialId") { type = NavType.LongType })
        ) { backStackEntry ->
            val materialId = backStackEntry.arguments?.getLong("materialId") ?: 0L
            MaterialDetailScreen(
                materialId = materialId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.MaterialUpload.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            MaterialUploadScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onUploadSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.Curriculum.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            CurriculumScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ProgressStatus.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            ProgressStatusScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.TeamDashboard.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            TeamDashboardScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onMemberClick = { userId ->
                    navController.navigate(NavRoutes.UserProfile.createRoute(userId))
                }
            )
        }

        composable(
            route = NavRoutes.ApplicationManagement.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            ApplicationManagementScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ExtendRecruitment.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            ExtendRecruitmentScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.TempChannel.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            TempChannelScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.ConvertToOfficial.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            ConvertToOfficialScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        // BLE Attendance (모바일 전용 오프라인 출석)
        composable(
            route = NavRoutes.AttendanceMember.route,
            arguments = listOf(
                navArgument("studyId") { type = NavType.LongType },
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            AttendanceMemberScreen(
                studyId = studyId,
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(NavRoutes.AttendanceSuccess.route) {
                        popUpTo(NavRoutes.AttendanceMember.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.AttendanceLeader.route,
            arguments = listOf(
                navArgument("studyId") { type = NavType.LongType },
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            AttendanceLeaderScreen(
                studyId = studyId,
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.AttendanceSuccess.route) {
            AttendanceSuccessScreen(
                onConfirm = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.SelfAttendance.route,
            arguments = listOf(
                navArgument("studyId") { type = NavType.LongType },
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            SelfAttendanceScreen(
                studyId = studyId,
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.AttendanceCalendar.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            AttendanceCalendarScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate(NavRoutes.SessionMemo.createRoute(sessionId))
                }
            )
        }

        composable(
            route = NavRoutes.SessionMemo.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            SessionMemoScreen(
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 회의록 (모바일 전용 - 녹음 기반)
        composable(
            route = NavRoutes.MeetingList.route,
            arguments = listOf(navArgument("studyId") { type = NavType.LongType })
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            MeetingListScreen(
                studyId = studyId,
                onBackClick = { navController.popBackStack() },
                onMeetingClick = { meetingId ->
                    navController.navigate(NavRoutes.MeetingDetail.createRoute(meetingId))
                }
            )
        }

        composable(
            route = NavRoutes.MeetingDetail.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getLong("meetingId") ?: 0L
            MeetingDetailScreen(
                meetingId = meetingId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // Schedule
        composable(NavRoutes.ScheduleList.route) {
            ScheduleListScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { studyId, sessionId ->
                    navController.navigate(NavRoutes.ScheduleDetail.createRoute(studyId, sessionId))
                },
                onCalendarClick = {
                    navController.navigate(NavRoutes.ScheduleCalendar.route)
                },
                onGoogleSyncClick = {
                    navController.navigate(NavRoutes.GoogleCalendarSync.route)
                }
            )
        }

        composable(NavRoutes.ScheduleCalendar.route) {
            ScheduleCalendarScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { studyId, sessionId ->
                    navController.navigate(NavRoutes.ScheduleDetail.createRoute(studyId, sessionId))
                }
            )
        }

        composable(
            route = NavRoutes.ScheduleDetail.route,
            arguments = listOf(
                navArgument("studyId") { type = NavType.LongType },
                navArgument("sessionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val studyId = backStackEntry.arguments?.getLong("studyId") ?: 0L
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            ScheduleDetailScreen(
                studyId = studyId,
                sessionId = sessionId,
                onBackClick = { navController.popBackStack() },
                onAttendanceClick = { isLeader ->
                    if (isLeader) {
                        navController.navigate(NavRoutes.AttendanceLeader.createRoute(studyId, sessionId))
                    } else {
                        navController.navigate(NavRoutes.AttendanceMember.createRoute(studyId, sessionId))
                    }
                }
            )
        }

        composable(NavRoutes.GoogleCalendarSync.route) {
            GoogleCalendarSyncScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Quiz
        composable(
            route = NavRoutes.QuizSolve.route,
            arguments = listOf(navArgument("quizId") { type = NavType.LongType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getLong("quizId") ?: 0L
            QuizSolveScreen(
                quizId = quizId,
                onBackClick = { navController.popBackStack() },
                onComplete = { attemptId ->
                    navController.navigate(NavRoutes.QuizResult.createRoute(attemptId)) {
                        popUpTo(NavRoutes.QuizSolve.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.QuizResult.route,
            arguments = listOf(navArgument("attemptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val attemptId = backStackEntry.arguments?.getLong("attemptId") ?: 0L
            QuizResultScreen(
                attemptId = attemptId,
                onBackClick = { navController.popBackStack() },
                onRetry = { quizId ->
                    navController.navigate(NavRoutes.QuizSolve.createRoute(quizId)) {
                        popUpTo(NavRoutes.QuizResult.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.WrongNotes.route) {
            WrongNotesScreen(
                onBackClick = { navController.popBackStack() },
                onNoteClick = { noteId ->
                    navController.navigate(NavRoutes.WrongNoteReview.createRoute(noteId))
                }
            )
        }

        // FSRS 복습 세션
        composable(NavRoutes.ReviewSession.route) {
            ReviewSessionScreen(
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.WrongNoteReview.route,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
            WrongNoteReviewScreen(
                noteId = noteId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // My Page
        composable(NavRoutes.EditProfile.route) {
            EditProfileScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.NotificationSettings.route) {
            NotificationSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.MyActivity.route) {
            MyActivityScreen(
                onBackClick = { navController.popBackStack() },
                onGrassClick = {
                    navController.navigate(NavRoutes.GrassGraph.route)
                },
                onDateClick = { date ->
                    navController.navigate(NavRoutes.ActivityDetail.createRoute(date))
                }
            )
        }

        composable(NavRoutes.GrassGraph.route) {
            GrassGraphScreen(
                onBackClick = { navController.popBackStack() },
                onDateClick = { date ->
                    navController.navigate(NavRoutes.ActivityDetail.createRoute(date))
                }
            )
        }

        composable(
            route = NavRoutes.ActivityDetail.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            ActivityDetailScreen(
                date = date,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PrivacySettings.route) {
            PrivacySettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // User Profile (스터디 멤버 프로필 조회용)
        composable(
            route = NavRoutes.UserProfile.route,
            arguments = listOf(navArgument("userId") { type = NavType.LongType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
            UserProfileScreen(
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onSendMessage = { partnerId, partnerNickname ->
                    navController.navigate(NavRoutes.DmChat.createRoute(-1, partnerId, partnerNickname))
                }
            )
        }

        // DM (1:1 채팅)
        composable(NavRoutes.DmList.route) {
            DmListScreen(
                onBackClick = { navController.popBackStack() },
                onConversationClick = { conversationId, partnerId, partnerNickname ->
                    navController.navigate(NavRoutes.DmChat.createRoute(conversationId, partnerId, partnerNickname))
                }
            )
        }

        composable(
            route = NavRoutes.DmChat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType },
                navArgument("partnerId") { type = NavType.LongType },
                navArgument("partnerNickname") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            val partnerId = backStackEntry.arguments?.getLong("partnerId") ?: 0L
            val partnerNickname = try {
                URLDecoder.decode(
                    backStackEntry.arguments?.getString("partnerNickname") ?: "",
                    "UTF-8"
                )
            } catch (e: Exception) {
                backStackEntry.arguments?.getString("partnerNickname") ?: ""
            }
            DmChatScreen(
                conversationId = conversationId,
                partnerId = partnerId,
                partnerNickname = partnerNickname,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
