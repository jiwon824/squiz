package com.ssafy.squiz.ui.screens.mystudy

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.StudySessionDTO
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHomeScreen(
    studyId: Long,
    onBackClick: () -> Unit,
    onNavigateToChannelList: () -> Unit,
    onNavigateToMaterials: () -> Unit,
    onNavigateToCurriculum: () -> Unit,
    onNavigateToProgressStatus: () -> Unit,
    onNavigateToTeamDashboard: () -> Unit,
    onNavigateToApplicationManagement: () -> Unit,
    onNavigateToAttendanceCalendar: () -> Unit,
    onNavigateToExtendRecruitment: () -> Unit,
    onNavigateToTempChannel: () -> Unit,
    onNavigateToConvertToOfficial: () -> Unit,
    onNavigateToMeetingList: (isLeader: Boolean) -> Unit,
    onNavigateToAttendance: (studyId: Long, sessionId: Long, isLeader: Boolean) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: StudyHomeViewModel = viewModel()
    val studyDetail by viewModel.studyDetail.collectAsState()
    val nextSession by viewModel.nextSession.collectAsState()
    val isLeader by viewModel.isLeader.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 스터디 홈 데이터 로드
    LaunchedEffect(studyId) {
        viewModel.loadStudyHome(studyId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(studyDetail?.name ?: "스터디") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "설정")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 로딩 상태 처리
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Next Session Card (핵심: 출석체크 버튼 포함)
                item {
                    NextSessionCard(
                        session = nextSession,
                        studyName = studyDetail?.name ?: "스터디",
                        onAttendanceClick = {
                            val session = nextSession
                            if (session != null) {
                                // 세션 시작 30분 전 ~ 세션 종료 시간까지 출석 가능
                                val isWithinAttendanceWindow = try {
                                    val sessionStart = LocalDateTime.parse(session.scheduledAt?.take(19))
                                    val durationMinutes = session.durationMinutes ?: 60
                                    val sessionEnd = sessionStart.plusMinutes(durationMinutes.toLong())
                                    val windowStart = sessionStart.minusMinutes(30)
                                    val now = LocalDateTime.now()
                                    android.util.Log.d("AttendanceCheck", "sessionStart=$sessionStart, durationMinutes=$durationMinutes, sessionEnd=$sessionEnd, windowStart=$windowStart, now=$now")
                                    val result = now.isAfter(windowStart) && now.isBefore(sessionEnd)
                                    android.util.Log.d("AttendanceCheck", "isWithinWindow=$result")
                                    result
                                } catch (e: Exception) {
                                    android.util.Log.e("AttendanceCheck", "Exception: ${e.message}")
                                    false
                                }

                                if (isWithinAttendanceWindow) {
                                    onNavigateToAttendance(studyId, session.id, isLeader)
                                } else {
                                    Toast.makeText(context, "출석체크 가능한 시간이 아닙니다!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "예정된 세션이 없습니다", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Activity Section (핵심 기능: 출석 캘린더, 미팅 녹음)
                item {
                    SectionHeader(title = "활동")
                }

                item {
                    ActivityMenus(
                        onAttendanceCalendarClick = onNavigateToAttendanceCalendar,
                        onRecordingClick = { onNavigateToMeetingList(isLeader) }
                    )
                }
            }
        }
    }
}

// 세션 정보 파싱 헬퍼 함수
private data class SessionDisplayInfo(
    val dateTimeText: String,
    val locationText: String,
    val dDayText: String?,
    val hasSession: Boolean
)

private fun parseSessionInfo(session: StudySessionDTO?): SessionDisplayInfo {
    if (session == null) {
        return SessionDisplayInfo(
            dateTimeText = "예정된 세션 없음",
            locationText = "",
            dDayText = null,
            hasSession = false
        )
    }

    // 날짜/시간 포맷팅
    val dateTimeText = try {
        val dateTime = LocalDateTime.parse(session.scheduledAt?.take(19))
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        dateTime.format(formatter)
    } catch (e: Exception) {
        session.scheduledAt?.replace("T", " ")?.take(16) ?: "시간 미정"
    }

    // 위치 정보
    val locationText = if (session.isOnline == true) "온라인" else session.location ?: "오프라인"

    // D-Day 계산
    val dDayText = try {
        val sessionDate = LocalDateTime.parse(session.scheduledAt?.take(19)).toLocalDate()
        val today = LocalDate.now()
        val daysUntil = ChronoUnit.DAYS.between(today, sessionDate)
        when {
            daysUntil == 0L -> "D-Day"
            daysUntil > 0 -> "D-$daysUntil"
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    return SessionDisplayInfo(
        dateTimeText = dateTimeText,
        locationText = locationText,
        dDayText = dDayText,
        hasSession = true
    )
}

@Composable
private fun NextSessionCard(
    session: StudySessionDTO?,
    studyName: String,
    onAttendanceClick: () -> Unit
) {
    val sessionInfo = remember(session) { parseSessionInfo(session) }

    // 출석 버튼 펄스 애니메이션 (D-Day일 때)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val isDDay = sessionInfo.dDayText == "D-Day"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Primary.copy(alpha = 0.3f),
                spotColor = Primary.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // D-Day 뱃지
                    if (sessionInfo.dDayText != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = sessionInfo.dDayText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "다음 모임",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sessionInfo.dateTimeText,
                        fontSize = if (sessionInfo.hasSession) 20.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (sessionInfo.locationText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (sessionInfo.locationText == "온라인") Icons.Outlined.Videocam else Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sessionInfo.locationText,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // 세션이 있을 때만 출석 버튼 활성화
                if (sessionInfo.hasSession) {
                    Surface(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = if (isDDay) pulseScale else 1f,
                                scaleY = if (isDDay) pulseScale else 1f
                            )
                            .clickable { onAttendanceClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDDay) Color.White else Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = if (isDDay) Primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "출석",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDDay) Primary else Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onChatClick: () -> Unit,
    onMaterialsClick: () -> Unit,
    onCurriculumClick: () -> Unit,
    onProgressClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Outlined.Chat,
            label = "채팅",
            onClick = onChatClick
        )
        QuickActionButton(
            icon = Icons.Outlined.Folder,
            label = "자료실",
            onClick = onMaterialsClick
        )
        QuickActionButton(
            icon = Icons.Outlined.ListAlt,
            label = "커리큘럼",
            onClick = onCurriculumClick
        )
        QuickActionButton(
            icon = Icons.Outlined.TrendingUp,
            label = "진행현황",
            onClick = onProgressClick
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TeamQuickView(
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewAllClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "팀원 5명",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Member avatars
            Row {
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .offset(x = (-8 * index).dp)
                            .background(
                                listOf(Primary, Secondary, Tertiary, Info, Warning)[index],
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = listOf("김", "이", "박", "최", "정")[index],
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityMenus(
    onAttendanceCalendarClick: () -> Unit,
    onRecordingClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuCard(
            icon = Icons.Outlined.CalendarMonth,
            title = "출석 캘린더",
            subtitle = "출석 현황을 확인하세요",
            onClick = onAttendanceCalendarClick
        )
        MenuCard(
            icon = Icons.Outlined.Mic,
            title = "미팅 녹음",
            subtitle = "회의내용을 녹음하고 제출해서 리포트를 받아보세요",
            onClick = onRecordingClick
        )
    }
}

@Composable
private fun LeaderMenus(
    onApplicationManagementClick: () -> Unit,
    onExtendRecruitmentClick: () -> Unit,
    onTempChannelClick: () -> Unit,
    onConvertToOfficialClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuCard(
            icon = Icons.Outlined.Assignment,
            title = "지원서 관리",
            subtitle = "새 지원서 2건",
            onClick = onApplicationManagementClick,
            badgeCount = 2
        )
        MenuCard(
            icon = Icons.Outlined.EventRepeat,
            title = "모집 연장",
            subtitle = "모집 기간을 연장하세요",
            onClick = onExtendRecruitmentClick
        )
        MenuCard(
            icon = Icons.Outlined.Forum,
            title = "임시 채널",
            subtitle = "지원자와 소통하세요",
            onClick = onTempChannelClick
        )
        MenuCard(
            icon = Icons.Outlined.Verified,
            title = "정식 전환",
            subtitle = "스터디를 정식으로 전환하세요",
            onClick = onConvertToOfficialClick
        )
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badgeCount: Int = 0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (badgeCount > 0) {
                Surface(
                    shape = CircleShape,
                    color = Error
                ) {
                    Text(
                        text = "$badgeCount",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
