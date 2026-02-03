package com.ssafy.squiz.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// 프리미엄 색상 상수
private object PremiumColors {
    val BackgroundGradientStart = Color(0xFFF8FAFC)  // 아주 연한 하늘빛
    val BackgroundGradientEnd = Color(0xFFFFFFFF)
    val DeepNavy = Color(0xFF1E293B)                  // 순수 검정 대신 깊은 네이비
    val SoftShadow = Color(0xFF1E293B)                // 부드러운 그림자용
    val GlassBorder = Color(0xFFFFFFFF)               // 유리 테두리
    val GlassBorderSubtle = Color(0x33FFFFFF)         // 미세한 테두리
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStudyClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onDmClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onBookmarkedClick: () -> Unit,
    onMyApplicationsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    onStartReview: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val homeState by viewModel.homeState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val todayReviewState by viewModel.todayReviewState.collectAsState()

    // 스크롤 없이 Column으로 고정 레이아웃 + 은은한 배경 그라데이션
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PremiumColors.BackgroundGradientStart,
                        PremiumColors.BackgroundGradientEnd
                    )
                )
            )
            .statusBarsPadding()
    ) {
        when (val state = homeState) {
            is HomeState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GradientLoadingIndicator(size = 48.dp)
                }
            }
            is HomeState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "데이터를 불러올 수 없습니다",
                                color = Error,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GradientButton(
                                text = "다시 시도",
                                onClick = { viewModel.loadHomeData() },
                                icon = Icons.Default.Refresh,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            is HomeState.Success -> {
                val data = state.data

                // weight를 명확히 적용하기 위해 별도 Column 사용
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 웰컴 배너 (알림 아이콘 포함)
                    CompactWelcomeBanner(
                        userName = data.user.nickname,
                        todayScheduleCount = data.todayScheduleCount,
                        hasNotification = unreadCount > 0,
                        onScheduleClick = onScheduleClick,
                        onNotificationClick = onNotificationClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 오늘의 복습 섹션
                    CompactTodayReviewSection(
                        state = todayReviewState,
                        onStartReview = onStartReview,
                        onRetry = { viewModel.loadTodayReview() }
                    )
                }
            }
        }
    }
}

// 마이크로 인터랙션: 눌렀을 때 살짝 눌리는 효과
@Composable
private fun PressableScale(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        onClick()
                    }
                )
            }
    ) {
        content()
    }
}

// 컴팩트 웰컴 배너 (알림 아이콘 통합) - Premium Glassmorphism
@Composable
private fun CompactWelcomeBanner(
    userName: String,
    todayScheduleCount: Int,
    hasNotification: Boolean,
    onScheduleClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = PremiumColors.SoftShadow.copy(alpha = 0.12f),
                spotColor = PremiumColors.SoftShadow.copy(alpha = 0.16f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(GradientPrimaryStart, GradientPrimaryEnd)
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
    ) {
        Column {
            // 상단: 인사 + 알림 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 인사 텍스트
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "👋",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "안녕하세요!",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "${userName}님",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 알림 버튼 (우측 상단) - Glassmorphism
                PressableScale(onClick = onNotificationClick) {
                    Box {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = "알림",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        if (hasNotification) {
                            // 프리미엄 알림 뱃지
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(12.dp)
                                    .shadow(4.dp, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .background(Error, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "오늘도 함께 성장해요",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 일정 정보 카드 - Premium Glass Effect
            PressableScale(onClick = onScheduleClick) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 캘린더 아이콘 박스
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "오늘 일정",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "${todayScheduleCount}개의 스터디",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // "일정 확인" 버튼 - 유리 질감
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "일정 확인",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 컴팩트 오늘의 복습 섹션 - Premium Card
@Composable
private fun CompactTodayReviewSection(
    state: TodayReviewState,
    onStartReview: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = PremiumColors.SoftShadow.copy(alpha = 0.15f),
                spotColor = PremiumColors.SoftShadow.copy(alpha = 0.20f)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(28.dp)
            )
            .border(
                width = 1.5.dp,
                color = Color(0xFFE2E8F0),  // Slate 200 테두리
                shape = RoundedCornerShape(28.dp)
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 헤더 - Premium Typography
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 아이콘 박스 - 그라데이션 + 그림자
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(14.dp),
                                ambientColor = GradientSecondaryStart.copy(alpha = 0.3f),
                                spotColor = GradientSecondaryEnd.copy(alpha = 0.3f)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(GradientSecondaryStart, GradientSecondaryEnd)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "오늘의 복습",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PremiumColors.DeepNavy
                        )
                        when (state) {
                            is TodayReviewState.Success -> {
                                Text(
                                    text = if (state.dueCount > 0) "${state.dueCount}개의 복습 대기 중" else "오늘 복습 완료!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (state.dueCount > 0) Warning else Success
                                )
                            }
                            else -> {
                                Text(
                                    text = "복습 현황 로딩 중...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = OnBackgroundSecondary
                                )
                            }
                        }
                    }
                }

                // FSRS 칩 - 더 세련된 스타일
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FSRS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 상태별 컨텐츠
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is TodayReviewState.Loading -> {
                        GradientLoadingIndicator(size = 40.dp)
                    }

                    is TodayReviewState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                color = Error,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedGradientButton(
                                text = "다시 시도",
                                onClick = onRetry,
                                icon = Icons.Default.Refresh
                            )
                        }
                    }

                    is TodayReviewState.Success -> {
                        if (state.dueCount == 0) {
                            // 복습 완료 상태 - Premium Style
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .shadow(
                                            elevation = 16.dp,
                                            shape = CircleShape,
                                            ambientColor = Success.copy(alpha = 0.25f),
                                            spotColor = Success.copy(alpha = 0.25f)
                                        )
                                        .background(
                                            brush = Brush.linearGradient(
                                                listOf(GradientSuccessStart, GradientSuccessEnd)
                                            ),
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 2.dp,
                                            color = Color.White.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = "오늘의 복습 완료!",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumColors.DeepNavy
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "수고하셨어요! 내일 다시 만나요",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = OnBackgroundSecondary
                                )
                            }
                        } else {
                            // 복습할 카드가 있는 상태
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 통계 카드
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CompactStatBox(
                                        label = "복습 예정",
                                        value = state.dueCount.toString(),
                                        color = Warning,
                                        icon = Icons.Outlined.Schedule,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactStatBox(
                                        label = "새 카드",
                                        value = state.newCount.toString(),
                                        color = Info,
                                        icon = Icons.Outlined.AddCircleOutline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactStatBox(
                                        label = "전체",
                                        value = state.totalCount.toString(),
                                        color = Primary,
                                        icon = Icons.Outlined.Layers,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // 복습 시작 버튼
                                GradientButton(
                                    text = "${state.dueCount}개 복습 시작",
                                    onClick = onStartReview,
                                    icon = Icons.Default.PlayArrow,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Premium Stat Box
@Composable
private fun CompactStatBox(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = OnBackgroundSecondary
            )
        }
    }
}
