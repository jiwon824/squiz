package com.ssafy.squiz.ui.screens.meeting

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(
    studyId: Long,
    meetingId: Long,
    onBackClick: () -> Unit,
    viewModel: MeetingViewModel = viewModel()
) {
    val detailState by viewModel.meetingDetailState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf(
        TabItem("요약", Icons.Outlined.Summarize),
        TabItem("녹취록", Icons.Outlined.Description),
        TabItem("참석자", Icons.Outlined.People)
    )

    // 데이터 로드
    LaunchedEffect(studyId, meetingId) {
        viewModel.loadMeetingDetail(studyId, meetingId)
    }

    Scaffold(
        topBar = {
            PremiumTopAppBar(
                title = "회의 상세",
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = detailState) {
            is MeetingDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    GradientLoadingIndicator()
                }
            }

            is MeetingDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "오류가 발생했습니다",
                        description = state.message,
                        actionButton = {
                            GradientButton(
                                text = "다시 시도",
                                onClick = { viewModel.loadMeetingDetail(studyId, meetingId) },
                                icon = Icons.Default.Refresh
                            )
                        }
                    )
                }
            }

            is MeetingDetailUiState.Success -> {
                val meeting = state.meeting

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 프리미엄 헤더
                    PremiumMeetingHeader(meeting)

                    // 커스텀 탭 바
                    PremiumTabBar(
                        tabs = tabs,
                        selectedIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )

                    // 탭 컨텐츠 (스와이프 지원)
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> SummaryTab(meeting.summary)
                            1 -> TranscriptTab(meeting.transcript)
                            2 -> ParticipantsTab(meeting.participants ?: emptyList())
                        }
                    }
                }
            }
        }
    }
}

// 탭 아이템 데이터 클래스
private data class TabItem(
    val title: String,
    val icon: ImageVector
)

// ============================================================
// 프리미엄 헤더
// ============================================================

@Composable
private fun PremiumMeetingHeader(meeting: MeetingDetailDTO) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        gradientColors = listOf(GradientPrimaryStart, GradientPrimaryEnd)
    ) {
        // 제목
        Text(
            text = meeting.title ?: "세션 ${meeting.id}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 통계 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HeaderStatItem(
                icon = Icons.Outlined.CalendarToday,
                value = meeting.startedAt?.take(10) ?: "-",
                label = "날짜"
            )
            HeaderStatItem(
                icon = Icons.Outlined.Timer,
                value = formatDuration(meeting.duration ?: 0),
                label = "녹음시간"
            )
            HeaderStatItem(
                icon = Icons.Outlined.People,
                value = "${meeting.participants?.size ?: 0}명",
                label = "참석자"
            )
        }

        // 상태 칩
        if (meeting.summary != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI 요약 완료",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderStatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

// ============================================================
// 프리미엄 탭 바
// ============================================================

@Composable
private fun PremiumTabBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Primary else Color.Transparent,
                    onClick = { onTabSelected(index) }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 요약 탭
// ============================================================

@Composable
private fun SummaryTab(summary: MeetingSummaryDTO?) {
    if (summary == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Outlined.Pending,
                title = "AI 요약 생성 중...",
                description = "잠시 후 다시 확인해주세요"
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 전체 요약
        item {
            SummarySection(
                title = "요약",
                icon = Icons.Outlined.Summarize,
                iconColor = Primary
            ) {
                Text(
                    text = summary.summary ?: "요약 내용이 없습니다.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                )
            }
        }

        // 핵심 포인트
        if (!summary.keyPoints.isNullOrEmpty()) {
            item {
                SummarySection(
                    title = "핵심 포인트",
                    icon = Icons.Outlined.Lightbulb,
                    iconColor = Tertiary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        summary.keyPoints.forEachIndexed { index, point ->
                            KeyPointItem(index + 1, point)
                        }
                    }
                }
            }
        }

        // 액션 아이템
        if (!summary.actionItems.isNullOrEmpty()) {
            item {
                SummarySection(
                    title = "액션 아이템",
                    icon = Icons.Outlined.TaskAlt,
                    iconColor = Secondary
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        summary.actionItems.forEach { action ->
                            PremiumActionItem(action)
                        }
                    }
                }
            }
        }

        // 결정 사항
        if (!summary.decisions.isNullOrEmpty()) {
            item {
                SummarySection(
                    title = "결정 사항",
                    icon = Icons.Outlined.Gavel,
                    iconColor = Success
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        summary.decisions.forEach { decision ->
                            DecisionItem(decision)
                        }
                    }
                }
            }
        }

        // 논의 주제
        if (!summary.topics.isNullOrEmpty()) {
            item {
                SummarySection(
                    title = "논의된 주제",
                    icon = Icons.Outlined.Topic,
                    iconColor = Info
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        summary.topics.forEach { topic ->
                            PremiumTopicItem(topic)
                        }
                    }
                }
            }
        }

        // 하단 여백
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SummarySection(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    SurfaceCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        content()
    }
}

@Composable
private fun KeyPointItem(index: Int, point: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(GradientPrimaryStart, GradientPrimaryEnd)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = point,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PremiumActionItem(action: ActionItemDTO) {
    val isCompleted = action.status == "COMPLETED"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCompleted) SuccessContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (isCompleted) Success else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Circle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.content,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (action.assignee != null || action.dueDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        if (action.assignee != null) {
                            StatusChip(
                                text = action.assignee,
                                color = Primary,
                                icon = Icons.Outlined.Person
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (action.dueDate != null) {
                            StatusChip(
                                text = action.dueDate.take(10),
                                color = Warning,
                                icon = Icons.Outlined.Event
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DecisionItem(decision: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = decision,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun PremiumTopicItem(topic: DiscussedTopicDTO) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = topic.topic,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (topic.duration != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Info.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = formatDuration(topic.duration),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Info,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (topic.summary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = topic.summary,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ============================================================
// 녹취록 탭
// ============================================================

@Composable
private fun TranscriptTab(transcript: String?) {
    if (transcript.isNullOrBlank()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Outlined.TextSnippet,
                title = "녹취록이 없습니다",
                description = "음성 인식 결과가 아직 생성되지 않았습니다"
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SurfaceCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "전체 녹취록",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 녹취록 텍스트
                SelectionContainer {
                    Text(
                        text = transcript,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ============================================================
// 참석자 탭
// ============================================================

@Composable
private fun ParticipantsTab(participants: List<MeetingParticipantDTO>) {
    if (participants.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Outlined.GroupOff,
                title = "참석자 정보가 없습니다",
                description = "회의 참석자 정보를 찾을 수 없습니다"
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 참석자 수 헤더
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "총 ${participants.size}명 참석",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(participants) { participant ->
            PremiumParticipantCard(participant)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PremiumParticipantCard(participant: MeetingParticipantDTO) {
    SurfaceCard(elevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 아바타
            ProfileAvatar(
                imageUrl = participant.profileImage,
                name = participant.nickname ?: "사용자",
                size = 52.dp,
                showBorder = true,
                borderColor = Primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // 참석자 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = participant.nickname ?: "사용자 ${participant.userId}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (participant.joinedAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = participant.joinedAt.take(16).replace("T", " "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 발언 시간 통계
            if (participant.speakingDuration != null && participant.speakingDuration > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(Primary.copy(alpha = 0.1f), Color.Transparent)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDuration(participant.speakingDuration),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
        }
    }
}

// ============================================================
// 유틸리티
// ============================================================

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
