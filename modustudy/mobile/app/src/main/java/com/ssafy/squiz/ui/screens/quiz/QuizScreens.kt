package com.ssafy.squiz.ui.screens.quiz

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.FsrsRating
import com.ssafy.squiz.data.remote.model.ReviewCardDTO
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// QuizHomeScreen
@Composable
fun QuizHomeScreen(
    onQuizClick: (Long) -> Unit,
    onWrongNotesClick: () -> Unit,
    onStartReview: () -> Unit = {},
    viewModel: ReviewViewModel = viewModel()
) {
    val todayReviewState by viewModel.todayReviewState.collectAsState()
    val statsState by viewModel.statsState.collectAsState()

    // 오늘 복습 로드
    LaunchedEffect(Unit) {
        viewModel.loadTodayReviews()
        viewModel.loadStats()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("퀴즈", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // 오늘 복습 카드
        item {
            TodayReviewCard(
                state = todayReviewState,
                onStartReview = {
                    viewModel.startSession()
                    onStartReview()
                },
                onRetry = { viewModel.loadTodayReviews() }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Quick Actions
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizQuickAction(icon = Icons.Filled.MenuBook, title = "오답노트", subtitle = "복습하기", color = Error, onClick = onWrongNotesClick, modifier = Modifier.weight(1f))
                QuizQuickAction(icon = Icons.Filled.Analytics, title = "학습 현황", subtitle = "통계 보기", color = Info, onClick = { }, modifier = Modifier.weight(1f))
            }
        }

        // 학습 통계 섹션
        item {
            when (val state = statsState) {
                is ReviewStatsUiState.Success -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    StatsCard(stats = state.stats)
                }
                else -> {}
            }
        }

        item { SectionHeader(title = "추천 퀴즈", actionText = "전체보기", onActionClick = { }) }

        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listOf(1L to "알고리즘 기초", 2L to "자료구조", 3L to "운영체제")) { (id, title) ->
                    QuizCard(id = id, title = title, questionCount = 10, onClick = { onQuizClick(id) })
                }
            }
        }

        item { SectionHeader(title = "최근 풀이") }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RecentQuizItem(title = "DFS/BFS 퀴즈", score = 80, date = "어제")
                RecentQuizItem(title = "스택/큐 퀴즈", score = 90, date = "3일 전")
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
    var showAnswer by remember { mutableStateOf(false) }

    // 세션 완료 시 이동
    LaunchedEffect(sessionState) {
        if (sessionState is ReviewSessionState.Completed) {
            onComplete()
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
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

                // 스터디 이름
                currentCard.studyName?.let { studyName ->
                    Text(
                        text = studyName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Question
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
                        Text(
                            text = "Q.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentCard.question,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Answer (접힘/펼침)
                if (!showAnswer) {
                    Button(
                        onClick = { showAnswer = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Primary.copy(alpha = 0.1f),
                            contentColor = Primary
                        )
                    ) {
                        Text(
                            text = "정답 확인",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Success.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
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

                Spacer(modifier = Modifier.weight(1f))

                // FSRS 평점 버튼
                if (showAnswer) {
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
                            onClick = {
                                viewModel.submitReview(FsrsRating.AGAIN)
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FsrsRatingButton(
                            rating = FsrsRating.HARD,
                            color = Warning,
                            onClick = {
                                viewModel.submitReview(FsrsRating.HARD)
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FsrsRatingButton(
                            rating = FsrsRating.GOOD,
                            color = Success,
                            onClick = {
                                viewModel.submitReview(FsrsRating.GOOD)
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FsrsRatingButton(
                            rating = FsrsRating.EASY,
                            color = Primary,
                            onClick = {
                                viewModel.submitReview(FsrsRating.EASY)
                                showAnswer = false
                            },
                            modifier = Modifier.weight(1f)
                        )
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
        )
    ) {
        Text(
            text = rating.label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
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
