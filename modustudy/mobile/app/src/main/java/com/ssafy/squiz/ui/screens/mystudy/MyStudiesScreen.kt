package com.ssafy.squiz.ui.screens.mystudy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.base.SquizApplication
import com.ssafy.squiz.data.remote.model.StudyDTO
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.screens.study.StudiesUiState
import com.ssafy.squiz.ui.screens.study.StudyViewModel
import com.ssafy.squiz.ui.theme.*

// 스터디 상태 상수
private object StudyStatusConstants {
    val ACTIVE_STATUSES = listOf("RECRUITING", "IN_PROGRESS", "RECRUIT_CLOSED", "PENDING", "SCHEDULED", "DRAFT")
    val COMPLETED_STATUSES = listOf("COMPLETED", "CANCELLED")
    val WARNING_STATUSES = listOf("DRAFT", "PENDING") // 주의가 필요한 상태
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyStudiesScreen(
    onStudyClick: (Long) -> Unit,
    onCreateStudyClick: () -> Unit = {},
    onExploreStudyClick: () -> Unit = {},
    viewModel: StudyViewModel = viewModel()
) {
    val myStudiesState by viewModel.myStudiesState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("스터디장", "스터디원", "완료")

    // 현재 사용자 ID 가져오기
    val currentUserId = remember {
        SquizApplication.getInstance().authManager.getCurrentUserId()
    }

    // 내 스터디 로드
    LaunchedEffect(Unit) {
        viewModel.loadMyStudies()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "내 스터디",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Tabs with Badge
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Primary
        ) {
            tabs.forEachIndexed { index, title ->
                // 각 탭의 스터디 개수 계산 (메모이제이션)
                val count = remember(myStudiesState, currentUserId) {
                    when (val state = myStudiesState) {
                        is StudiesUiState.Success -> {
                            state.studies.count { study ->
                                val isLeader = currentUserId > 0 && study.leader?.id == currentUserId
                                when (index) {
                                    0 -> isLeader && study.status in StudyStatusConstants.ACTIVE_STATUSES
                                    1 -> !isLeader && study.status in StudyStatusConstants.ACTIVE_STATUSES
                                    else -> study.status in StudyStatusConstants.COMPLETED_STATUSES
                                }
                            }
                        }
                        else -> 0
                    }
                }

                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTab == index) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (count > 0) {
                                Badge(
                                    containerColor = if (selectedTab == index) Primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTab == index) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 6.dp)
                                ) {
                                    Text("$count")
                                }
                            }
                        }
                    }
                )
            }
        }

        when (val state = myStudiesState) {
            is StudiesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            is StudiesUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = Error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadMyStudies() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            is StudiesUiState.Success -> {
                // 메모이제이션된 필터링 로직
                val studies = remember(selectedTab, state.studies, currentUserId) {
                    state.studies.filter { study ->
                        val isLeader = currentUserId > 0 && study.leader?.id == currentUserId
                        when (selectedTab) {
                            0 -> isLeader && study.status in StudyStatusConstants.ACTIVE_STATUSES
                            1 -> !isLeader && study.status in StudyStatusConstants.ACTIVE_STATUSES
                            else -> study.status in StudyStatusConstants.COMPLETED_STATUSES
                        }
                    }
                }

                val (emptyTitle, emptyDescription, emptyIcon) = remember(selectedTab) {
                    when (selectedTab) {
                        0 -> Triple(
                            "스터디장으로 참여 중인 스터디가 없습니다",
                            "새로운 스터디를 만들어 리더가 되어보세요!",
                            Icons.Outlined.Star
                        )
                        1 -> Triple(
                            "스터디원으로 참여 중인 스터디가 없습니다",
                            "관심있는 스터디를 찾아 참여해보세요!",
                            Icons.Outlined.Groups
                        )
                        else -> Triple(
                            "완료된 스터디가 없습니다",
                            "스터디를 완료하면 여기에 표시됩니다.",
                            Icons.Outlined.CheckCircle
                        )
                    }
                }

                if (studies.isEmpty()) {
                    // Empty State with Action Button
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = emptyIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = emptyTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = emptyDescription,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // 액션 버튼
                            when (selectedTab) {
                                0 -> {
                                    Button(
                                        onClick = onCreateStudyClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("첫 스터디 만들기")
                                    }
                                }
                                1 -> {
                                    Button(
                                        onClick = onExploreStudyClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("스터디 탐색하기")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 80.dp // 바텀 네비게이션 바 여백
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(studies) { study ->
                            MyStudyCard(
                                study = study,
                                currentUserId = currentUserId,
                                selectedTab = selectedTab,
                                onClick = { onStudyClick(study.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyStudyCard(
    study: StudyDTO,
    currentUserId: Long,
    selectedTab: Int,
    onClick: () -> Unit
) {
    val isLeader = currentUserId > 0 && study.leader?.id == currentUserId
    val showLeaderBadge = isLeader && selectedTab != 0
    val isWarningStatus = study.status in StudyStatusConstants.WARNING_STATUSES

    val statusText = when (study.status) {
        "DRAFT" -> "임시저장"
        "SCHEDULED" -> "모집예정"
        "RECRUITING" -> "모집중"
        "RECRUIT_CLOSED" -> "모집완료"
        "PENDING" -> "확정대기"
        "IN_PROGRESS" -> "진행중"
        "COMPLETED" -> "완료"
        "CANCELLED" -> "취소"
        else -> study.status ?: "준비중"
    }

    val statusColor = when (study.status) {
        "RECRUITING" -> Success
        "RECRUIT_CLOSED", "PENDING", "SCHEDULED" -> Warning
        "IN_PROGRESS" -> Info
        "COMPLETED" -> Primary
        "CANCELLED" -> Error
        "DRAFT" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val progress = study.progress ?: 0
    val nextMeeting = study.nextMeeting ?: study.startDate?.let { "시작일: ${it.take(10)}" } ?: ""

    // 스터디 카테고리에 따른 아이콘 색상
    val iconColors = remember(study.id) {
        listOf(
            listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), // 보라
            listOf(Color(0xFF14B8A6), Color(0xFF06B6D4)), // 청록
            listOf(Color(0xFFF59E0B), Color(0xFFF97316)), // 오렌지
            listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), // 핑크
            listOf(GradientStart, GradientEnd)  // 기본
        )[(study.id % 5).toInt()]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 상단: 아이콘 + 제목 + 상태 배지
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Study Icon with dynamic color
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                brush = Brush.linearGradient(colors = iconColors),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = study.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (showLeaderBadge) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "스터디장",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Primary
                                    )
                                }
                            }
                        }

                        // 다음 모임 정보
                        if (nextMeeting.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = nextMeeting,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 상태 배지 (경고 상태일 때 아이콘 추가)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWarningStatus) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    StatusBadge(
                        text = statusText,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 중단: 인원수 뱃지 + 진행률 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 인원수 뱃지
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${study.currentMembers ?: 0}/${study.maxMembers}명",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 진행률 텍스트
                Text(
                    text = "진행률 $progress%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 하단: 진행률 바 (둥근 형태)
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.1f)
            )
        }
    }
}
