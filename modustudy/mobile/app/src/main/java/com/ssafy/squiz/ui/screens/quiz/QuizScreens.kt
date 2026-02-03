package com.ssafy.squiz.ui.screens.quiz

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.FsrsRating
import com.ssafy.squiz.data.remote.model.ReviewCardDTO
import com.ssafy.squiz.data.remote.model.WrongAnswerSortType
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// 퀴즈 탭 타입
enum class QuizTabType(val title: String) {
    REVIEW("오늘의 복습"),
    WRONG("틀린 문제"),
    STATS("통계")
}

// QuizHomeScreen - 프론트엔드 /quiz/my-quiz 와 동일한 탭 구조
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizHomeScreen(
    onQuizClick: (Long) -> Unit,
    onWrongNotesClick: () -> Unit,
    onStartReview: () -> Unit = {},
    viewModel: ReviewViewModel = viewModel()
) {
    val todayReviewState by viewModel.todayReviewState.collectAsState()
    val wrongAnswersState by viewModel.wrongAnswersState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()
    val wrongSortType by viewModel.wrongSortType.collectAsState()

    var selectedTab by remember { mutableStateOf(QuizTabType.REVIEW) }

    // 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.loadTodayReviews()
        viewModel.loadWrongAnswers()
        viewModel.loadStats()
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
                text = "퀴즈 관리",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Primary,
            edgePadding = 16.dp
        ) {
            QuizTabType.values().forEach { tab ->
                val count = when (tab) {
                    QuizTabType.REVIEW -> (todayReviewState as? TodayReviewUiState.Success)?.cards?.size ?: 0
                    QuizTabType.WRONG -> (wrongAnswersState as? WrongAnswersUiState.Success)?.cards?.size ?: 0
                    QuizTabType.STATS -> null
                }
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTab == tab) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (count != null && count > 0) {
                                Badge(
                                    containerColor = if (selectedTab == tab) Primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selectedTab == tab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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

        // Tab Content
        when (selectedTab) {
            QuizTabType.REVIEW -> TodayReviewTab(
                state = todayReviewState,
                onStartReview = {
                    viewModel.startSession()
                    onStartReview()
                },
                onRetry = { viewModel.loadTodayReviews() }
            )
            QuizTabType.WRONG -> WrongAnswersTab(
                state = wrongAnswersState,
                sortType = wrongSortType,
                onSortTypeChange = { viewModel.setWrongSortType(it) },
                onRetry = { viewModel.loadWrongAnswers() },
                onItemClick = onStartReview
            )
            QuizTabType.STATS -> StatsTab(
                statsState = statsState,
                wrongAnswersState = wrongAnswersState,
                onRetry = { viewModel.loadStats() }
            )
        }
    }
}

// 오늘의 복습 탭
@Composable
private fun TodayReviewTab(
    state: TodayReviewUiState,
    onStartReview: () -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is TodayReviewUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is TodayReviewUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = Error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("다시 시도")
                    }
                }
            }
        }
        is TodayReviewUiState.Success -> {
            if (state.cards.isEmpty()) {
                // 빈 상태
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "오늘의 복습 끝!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "오늘은 더 이상 복습할 내용이 없습니다. 훌륭해요!",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 복습 카드가 있을 때 - 하단 고정 버튼 레이아웃
                Box(modifier = Modifier.fillMaxSize()) {
                    // 복습 문제 목록
                    LazyColumn(
                        contentPadding = PaddingValues(
                            top = 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 100.dp // 하단 버튼 영역 확보
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.cards) { card ->
                            ReviewItemCard(card = card, type = "review", onClick = onStartReview)
                        }
                    }

                    // 하단 고정 복습 시작 버튼
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        GradientButton(
                            text = "${state.dueCount}개 복습 시작",
                            onClick = onStartReview
                        )
                    }
                }
            }
        }
    }
}

// 틀린 문제 탭
@Composable
private fun WrongAnswersTab(
    state: WrongAnswersUiState,
    sortType: WrongAnswerSortType,
    onSortTypeChange: (WrongAnswerSortType) -> Unit,
    onRetry: () -> Unit,
    onItemClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 정렬 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = sortType == WrongAnswerSortType.MOST_WRONG,
                onClick = { onSortTypeChange(WrongAnswerSortType.MOST_WRONG) },
                label = { Text("많이 틀린 순") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = sortType == WrongAnswerSortType.FSRS_RECOMMENDED,
                onClick = { onSortTypeChange(WrongAnswerSortType.FSRS_RECOMMENDED) },
                label = { Text("복습 우선순위") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = Color.White
                )
            )
        }

        when (state) {
            is WrongAnswersUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is WrongAnswersUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = Error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text("다시 시도")
                        }
                    }
                }
            }
            is WrongAnswersUiState.Success -> {
                if (state.cards.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "틀린 문제가 없습니다",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "완벽하게 이해하고 계시네요.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 80.dp // 바텀 네비게이션 바 여백
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.cards) { card ->
                            ReviewItemCard(card = card, type = "wrong", onClick = onItemClick)
                        }
                    }
                }
            }
        }
    }
}

// 통계 탭
@Composable
private fun StatsTab(
    statsState: ReviewStatsUiState,
    wrongAnswersState: WrongAnswersUiState,
    onRetry: () -> Unit
) {
    when (statsState) {
        is ReviewStatsUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        }
        is ReviewStatsUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = statsState.message, color = Error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRetry) {
                        Text("다시 시도")
                    }
                }
            }
        }
        is ReviewStatsUiState.Success -> {
            val stats = statsState.stats
            val wrongCards = (wrongAnswersState as? WrongAnswersUiState.Success)?.cards ?: emptyList()
            val totalWrongCount = wrongCards.sumOf { it.lapses ?: 0 }
            val avgWrongCount = if (wrongCards.isNotEmpty()) totalWrongCount.toFloat() / wrongCards.size else 0f

            // 카테고리별 오답 분포
            val categoryStats = wrongCards
                .groupBy { it.questionDetail?.category ?: "기타" }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 요약 카드
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsInfoCard(
                        label = "틀린 문제 수",
                        value = "${wrongCards.size}",
                        icon = Icons.Outlined.Close,
                        color = Error,
                        modifier = Modifier.weight(1f)
                    )
                    StatsInfoCard(
                        label = "총 오답 횟수",
                        value = "$totalWrongCount",
                        icon = Icons.Outlined.Warning,
                        color = Warning,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatsInfoCard(
                        label = "평균 오답 횟수",
                        value = String.format("%.1f", avgWrongCount),
                        icon = Icons.Outlined.TrendingDown,
                        color = Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatsInfoCard(
                        label = "기억률",
                        value = "${((stats.averageRetention ?: 0f) * 100).toInt()}%",
                        icon = Icons.Outlined.Psychology,
                        color = Primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 카테고리별 오답 분포
                if (categoryStats.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "카테고리별 오답 분포",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val maxCount = categoryStats.maxOfOrNull { it.second } ?: 1
                            categoryStats.forEach { (category, count) ->
                                val percentage = count.toFloat() / maxCount
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = category,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${count}문제",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = when {
                                            percentage >= 0.7f -> Error
                                            percentage >= 0.4f -> Warning
                                            else -> Primary
                                        },
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 학습 통계
                StatsCard(stats = stats)
            }
        }
    }
}

// 통계 정보 카드
@Composable
private fun StatsInfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 복습 아이템 카드
@Composable
private fun ReviewItemCard(
    card: ReviewCardDTO,
    type: String,
    onClick: () -> Unit
) {
    val difficultyColor = when {
        (card.difficulty ?: 0.0) < 3.0 -> Success
        (card.difficulty ?: 0.0) < 7.0 -> Info
        else -> Error
    }
    val difficultyLabel = when {
        (card.difficulty ?: 0.0) < 3.0 -> "쉬움"
        (card.difficulty ?: 0.0) < 7.0 -> "보통"
        else -> "어려움"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 메타 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 난이도 뱃지
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = difficultyColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = difficultyLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = difficultyColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 카테고리 뱃지
                card.questionDetail?.category?.let { category ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 오답 횟수 (틀린 문제 탭)
                if (type == "wrong" && (card.lapses ?: 0) > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Error.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${card.lapses}회 오답",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Error
                            )
                        }
                    }
                }

                // 복습 예정 (오늘의 복습 탭)
                if (type == "review") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "오늘 복습",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 문제 텍스트
            Text(
                text = card.question,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 하단 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 날짜 정보
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val dateText = card.nextReviewAt?.take(10) ?: ""
                    Text(
                        text = if (type == "review") "복습 예정: $dateText" else "마지막 오답: $dateText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 복습하기/다시 풀기 버튼
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (type == "review") "복습하기" else "다시 풀기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayReviewCard(
    state: TodayReviewUiState,
    onStartReview: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 복습",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (state) {
                is TodayReviewUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Primary
                        )
                    }
                }

                is TodayReviewUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = Error,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text("다시 시도")
                        }
                    }
                }

                is TodayReviewUiState.Success -> {
                    if (state.dueCount == 0) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "오늘 복습 완료!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Success
                            )
                            Text(
                                text = "내일 다시 만나요",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ReviewStatItem(
                                label = "복습 예정",
                                value = state.dueCount.toString(),
                                color = Warning
                            )
                            ReviewStatItem(
                                label = "새 카드",
                                value = state.newCount.toString(),
                                color = Info
                            )
                            ReviewStatItem(
                                label = "전체",
                                value = state.totalCount.toString(),
                                color = Primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        GradientButton(
                            text = "${state.dueCount}개 복습 시작",
                            onClick = onStartReview
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsCard(stats: com.ssafy.squiz.data.remote.model.ReviewStatsResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Primary.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${stats.streak}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Text(
                    text = "연속 학습일",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${stats.reviewedToday}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                Text(
                    text = "오늘 복습",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${((stats.averageRetention ?: 0f) * 100).toInt()}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Warning
                )
                Text(
                    text = "기억률",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuizQuickAction(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuizCard(id: Long, title: String, questionCount: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.width(200.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(48.dp).background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Quiz, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${questionCount}문제", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentQuizItem(title: String, score: Int, date: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${score}점", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (score >= 80) Success else Warning)
        }
    }
}

// ReviewSessionScreen - FSRS 복습 세션
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    onBackClick: () -> Unit,
    onComplete: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val todayReviewState by viewModel.todayReviewState.collectAsState()
    val currentCardIndex by viewModel.currentCardIndex.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()

    // 선택한 답안과 정답 표시 상태
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var userInputAnswer by remember { mutableStateOf("") }  // 단답형 사용자 입력

    // 화면 진입 시 데이터 로드 및 세션 시작
    LaunchedEffect(Unit) {
        viewModel.loadTodayReviews()
        viewModel.startSession()
    }

    // 세션 완료 시 이동
    LaunchedEffect(sessionState) {
        if (sessionState is ReviewSessionState.Completed) {
            onComplete()
        }
    }

    // 문제가 바뀔 때 선택 초기화
    LaunchedEffect(currentCardIndex) {
        selectedAnswer = null
        showResult = false
        userInputAnswer = ""
    }

    val currentCard = viewModel.getCurrentCard()
    val totalCards = when (val state = todayReviewState) {
        is TodayReviewUiState.Success -> state.cards.size
        else -> 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (totalCards > 0) {
                        Text("${currentCardIndex + 1} / $totalCards")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetSession()
                        onBackClick()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
            )
        }
    ) { padding ->
        if (currentCard == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            val options = currentCard.questionDetail?.options
            val correctAnswer = currentCard.questionDetail?.correctAnswer
            val hasOptions = !options.isNullOrEmpty()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 상단 고정 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 20.dp)
                ) {
                    // Progress
                    LinearProgressIndicator(
                        progress = { (currentCardIndex + 1) / totalCards.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 카테고리
                    currentCard.studyName?.let { studyName ->
                        Text(
                            text = studyName,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 스크롤 가능한 중간 영역
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Primary
                                ) {
                                    Text(
                                        text = "Q${currentCardIndex + 1}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (hasOptions) "객관식" else "단답형",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = currentCard.question,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 28.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 보기 (객관식)
                    if (hasOptions) {
                        options?.forEachIndexed { index, option ->
                            val optionLabel = option.optionLabel.ifEmpty { "${index + 1}" }
                            val optionText = option.text ?: ""
                            val isSelected = selectedAnswer == optionLabel
                            val isCorrect = optionLabel == correctAnswer

                            // 결과 표시 시 색상 결정
                            val backgroundColor = when {
                                !showResult && isSelected -> Primary.copy(alpha = 0.1f)
                                showResult && isCorrect -> Success.copy(alpha = 0.1f)
                                showResult && isSelected && !isCorrect -> Error.copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val borderColor = when {
                                !showResult && isSelected -> Primary
                                showResult && isCorrect -> Success
                                showResult && isSelected && !isCorrect -> Error
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                            val iconTint = when {
                                !showResult && isSelected -> Primary
                                showResult && isCorrect -> Success
                                showResult && isSelected && !isCorrect -> Error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                                    .clickable(enabled = !showResult) {
                                        selectedAnswer = optionLabel
                                    },
                                color = backgroundColor
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 선택 아이콘
                                    Icon(
                                        imageVector = when {
                                            showResult && isCorrect -> Icons.Filled.CheckCircle
                                            showResult && isSelected && !isCorrect -> Icons.Filled.Cancel
                                            isSelected -> Icons.Filled.RadioButtonChecked
                                            else -> Icons.Outlined.RadioButtonUnchecked
                                        },
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = "$optionLabel. $optionText",
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected || (showResult && isCorrect)) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    } else {
                        // 단답형 - 입력 필드 또는 정답 표시
                        if (!showResult) {
                            // 답변 입력 필드
                            OutlinedTextField(
                                value = userInputAnswer,
                                onValueChange = { userInputAnswer = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("답변을 입력하세요") },
                                placeholder = { Text("정답을 입력하세요") },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                singleLine = false,
                                minLines = 2,
                                maxLines = 4
                            )
                        } else {
                            // 내 답변 표시
                            if (userInputAnswer.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Info.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = Info,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "내 답변",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Info
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = userInputAnswer,
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // 정답 표시
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Success.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Success,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "정답",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Success
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = currentCard.answer,
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 하단 고정 버튼 영역
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp, top = 12.dp)
                ) {
                    if (hasOptions) {
                        if (!showResult) {
                            // 정답 확인 버튼
                            Button(
                                onClick = { showResult = true },
                                enabled = selectedAnswer != null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = "정답 확인",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            // 정답/오답 결과 및 다음 버튼
                            val isCorrectAnswer = selectedAnswer == correctAnswer

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCorrectAnswer) Success.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCorrectAnswer) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                        contentDescription = null,
                                        tint = if (isCorrectAnswer) Success else Error,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (isCorrectAnswer) "정답입니다!" else "오답입니다",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrectAnswer) Success else Error
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    // FSRS 평점 자동 계산
                                    val rating = if (isCorrectAnswer) FsrsRating.GOOD else FsrsRating.AGAIN
                                    viewModel.submitReview(rating)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text(
                                    text = "다음 문제",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // 단답형
                        if (!showResult) {
                            // 정답 확인 버튼
                            Button(
                                onClick = { showResult = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary)
                            ) {
                                Text(
                                    text = "정답 확인",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            // 정답 확인 후 FSRS 평점 버튼 표시
                            Text(
                                text = "얼마나 기억나셨나요?",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FsrsRatingButton(
                                    rating = FsrsRating.AGAIN,
                                    color = Error,
                                    onClick = { viewModel.submitReview(FsrsRating.AGAIN) },
                                    modifier = Modifier.weight(1f)
                                )
                                FsrsRatingButton(
                                    rating = FsrsRating.HARD,
                                    color = Warning,
                                    onClick = { viewModel.submitReview(FsrsRating.HARD) },
                                    modifier = Modifier.weight(1f)
                                )
                                FsrsRatingButton(
                                    rating = FsrsRating.GOOD,
                                    color = Success,
                                    onClick = { viewModel.submitReview(FsrsRating.GOOD) },
                                    modifier = Modifier.weight(1f)
                                )
                                FsrsRatingButton(
                                    rating = FsrsRating.EASY,
                                    color = Primary,
                                    onClick = { viewModel.submitReview(FsrsRating.EASY) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FsrsRatingButton(
    rating: FsrsRating,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor = color
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = rating.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

// ReviewCompleteScreen - 세션 완료 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewCompleteScreen(
    onBackClick: () -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val sessionResults by viewModel.sessionResults.collectAsState()
    val totalCount = sessionResults.size
    val goodCount = sessionResults.count { it.rating == FsrsRating.GOOD || it.rating == FsrsRating.EASY }

    Scaffold(
        topBar = {
            SquizTopBar(title = "복습 완료", onBackClick = {
                viewModel.resetSession()
                viewModel.loadTodayReviews()
                onBackClick()
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 완료 아이콘
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "오늘의 복습 완료!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${totalCount}개 카드를 복습했습니다",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 통계
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBox(label = "복습 완료", value = "$totalCount", color = Primary)
                StatBox(label = "잘 기억함", value = "$goodCount", color = Success)
                StatBox(label = "다시 복습", value = "${totalCount - goodCount}", color = Warning)
            }

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(
                text = "확인",
                onClick = {
                    viewModel.resetSession()
                    viewModel.loadTodayReviews()
                    onBackClick()
                }
            )
        }
    }
}

// QuizSolveScreen - 기존 퀴즈 풀이 (복습 세션과 별개)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizSolveScreen(
    quizId: Long,
    onBackClick: () -> Unit,
    onComplete: (Long) -> Unit,
    viewModel: ReviewViewModel = viewModel()
) {
    val quizDetailState by viewModel.quizDetailState.collectAsState()
    var currentQuestion by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var answers by remember { mutableStateOf<List<Int>>(emptyList()) }

    // 퀴즈 로드
    LaunchedEffect(quizId) {
        viewModel.loadQuizDetail(quizId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = quizDetailState) {
            is QuizDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            is QuizDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = Error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadQuizDetail(quizId) }) {
                            Text("다시 시도")
                        }
                    }
                }
            }

            is QuizDetailUiState.Success -> {
                val quiz = state.quiz
                val questions = quiz.questions
                val totalQuestions = questions.size

                if (questions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("문제가 없습니다")
                    }
                    return@Scaffold
                }

                val question = questions[currentQuestion]
                val options = question.options ?: emptyList()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp)
                ) {
                    // Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = quiz.title,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${currentQuestion + 1} / $totalQuestions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (currentQuestion + 1) / totalQuestions.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Question
                    Text("Q${currentQuestion + 1}.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(question.question, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp)

                    Spacer(modifier = Modifier.height(32.dp))

                    // Options
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        options.forEachIndexed { index, option ->
                            val isSelected = selectedAnswer == index
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAnswer = index }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${index + 1}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(option, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Next Button
                    GradientButton(
                        text = if (currentQuestion == totalQuestions - 1) "제출" else "다음",
                        onClick = {
                            answers = answers + (selectedAnswer ?: -1)
                            if (currentQuestion == totalQuestions - 1) {
                                onComplete(quizId)
                            } else {
                                currentQuestion++
                                selectedAnswer = null
                            }
                        },
                        enabled = selectedAnswer != null
                    )
                }
            }
        }
    }
}

// QuizResultScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizResultScreen(attemptId: Long, onBackClick: () -> Unit, onRetry: (Long) -> Unit) {
    val score = 80
    val correctCount = 8
    val totalCount = 10

    Scaffold(topBar = { SquizTopBar(title = "결과", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))

            // Score Circle
            Box(modifier = Modifier.size(180.dp).background(brush = Brush.linearGradient(listOf(GradientStart, GradientEnd)), shape = CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("점", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("알고리즘 기초 퀴즈", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("$correctCount / $totalCount 문제 정답", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            // Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBox(label = "정답", value = "$correctCount", color = Success)
                StatBox(label = "오답", value = "${totalCount - correctCount}", color = Error)
                StatBox(label = "소요시간", value = "5:30", color = Info)
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onRetry(1L) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("다시 풀기") }
                Button(onClick = onBackClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("확인") }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// WrongNotesScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongNotesScreen(onBackClick: () -> Unit, onNoteClick: (Long) -> Unit) {
    val notes = listOf(Triple(1L, "스택의 특징", "알고리즘 기초"), Triple(2L, "BFS vs DFS", "그래프"), Triple(3L, "동적 프로그래밍", "DP 기초"))

    Scaffold(topBar = { SquizTopBar(title = "오답노트", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(notes) { (id, title, category) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onNoteClick(id) }, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).background(Error.copy(alpha = 0.1f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Close, contentDescription = null, tint = Error)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(category, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// WrongNoteReviewScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrongNoteReviewScreen(noteId: Long, onBackClick: () -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "오답 복습", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("Q.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("다음 중 스택(Stack)의 특징으로 올바른 것은?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("내 답변", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("FIFO (First In First Out)", fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("정답", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Success)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LIFO (Last In First Out)", fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("해설", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("스택은 LIFO(Last In First Out) 구조로, 마지막에 삽입된 데이터가 가장 먼저 삭제됩니다. FIFO는 큐(Queue)의 특징입니다.", fontSize = 14.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
