package com.ssafy.squiz.ui.screens.mystudy

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.StudySessionDTO
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

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
    onNavigateToMeetingList: () -> Unit,
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
                            // 세션 정보가 있으면 출석 화면으로 이동
                            onNavigateToAttendance(studyId, session.id, isLeader)
                        } else {
                            // 세션 정보가 없으면 토스트 메시지
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
                    onRecordingClick = onNavigateToMeetingList
                )
            }

            /* 간소화를 위해 주석처리
            // Quick Actions (채팅, 자료실, 커리큘럼, 진행현황)
            item {
                QuickActionsSection(
                    onChatClick = onNavigateToChannelList,
                    onMaterialsClick = onNavigateToMaterials,
                    onCurriculumClick = onNavigateToCurriculum,
                    onProgressClick = onNavigateToProgressStatus
                )
            }

            // Team Section
            item {
                SectionHeader(title = "팀 현황")
            }

            item {
                TeamQuickView(
                    onViewAllClick = onNavigateToTeamDashboard
                )
            }

            // Leader Only Section
            if (isLeader) {
                item {
                    SectionHeader(title = "스터디장 관리")
                }

                item {
                    LeaderMenus(
                        onApplicationManagementClick = onNavigateToApplicationManagement,
                        onExtendRecruitmentClick = onNavigateToExtendRecruitment,
                        onTempChannelClick = onNavigateToTempChannel,
                        onConvertToOfficialClick = onNavigateToConvertToOfficial
                    )
                }
            }
            */
        }
    }
}

@Composable
private fun NextSessionCard(
    session: StudySessionDTO?,
    studyName: String,
    onAttendanceClick: () -> Unit
) {
    // 세션 정보 파싱
    val sessionInfo = if (session != null) {
        // 날짜/시간 포맷팅 (scheduledAt: "2024-01-15T14:00:00")
        val dateTime = session.scheduledAt?.replace("T", " ")?.take(16) ?: "시간 미정"
        val location = if (session.isOnline == true) "온라인" else session.location ?: "오프라인"
        Triple(dateTime, location, true)
    } else {
        Triple("예정된 세션 없음", "", false)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                Column {
                    Text(
                        text = "다음 모임",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sessionInfo.first,
                        fontSize = if (sessionInfo.third) 22.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (sessionInfo.second.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sessionInfo.second,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                // 세션이 있을 때만 출석 버튼 활성화
                if (sessionInfo.third) {
                    Surface(
                        modifier = Modifier.clickable { onAttendanceClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "출석체크",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
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
            subtitle = "실시간 STT로 회의 내용을 기록하세요",
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
