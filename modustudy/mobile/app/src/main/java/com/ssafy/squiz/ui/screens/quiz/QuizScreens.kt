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
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// QuizHomeScreen
@Composable
fun QuizHomeScreen(
    onQuizClick: (Long) -> Unit,
    onContestClick: () -> Unit,
    onCourseClick: () -> Unit,
    onWrongNotesClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("퀴즈", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Quick Actions
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizQuickAction(icon = Icons.Filled.EmojiEvents, title = "퀴즈 대회", subtitle = "실시간 대결", color = Warning, onClick = onContestClick, modifier = Modifier.weight(1f))
                QuizQuickAction(icon = Icons.Filled.School, title = "학습 코스", subtitle = "단계별 학습", color = Secondary, onClick = onCourseClick, modifier = Modifier.weight(1f))
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuizQuickAction(icon = Icons.Filled.MenuBook, title = "오답노트", subtitle = "복습하기", color = Error, onClick = onWrongNotesClick, modifier = Modifier.weight(1f))
                QuizQuickAction(icon = Icons.Filled.Analytics, title = "학습 현황", subtitle = "통계 보기", color = Info, onClick = { }, modifier = Modifier.weight(1f))
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

// QuizSolveScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizSolveScreen(quizId: Long, onBackClick: () -> Unit, onComplete: (Long) -> Unit) {
    var currentQuestion by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    val totalQuestions = 10
    val question = "다음 중 스택(Stack)의 특징으로 올바른 것은?"
    val options = listOf("FIFO (First In First Out)", "LIFO (Last In First Out)", "랜덤 접근 가능", "양쪽에서 삽입/삭제 가능")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${currentQuestion + 1} / $totalQuestions") },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = "닫기") } },
                actions = { Text("남은 시간: 9:30", modifier = Modifier.padding(end = 16.dp), fontSize = 14.sp, color = Warning) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            // Progress
            LinearProgressIndicator(progress = { (currentQuestion + 1) / totalQuestions.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)

            Spacer(modifier = Modifier.height(32.dp))

            // Question
            Text("Q${currentQuestion + 1}.", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(question, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // Options
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedAnswer == index
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { selectedAnswer = index }.border(width = 2.dp, color = if (isSelected) Primary else MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(28.dp).background(if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                                Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
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
                    if (currentQuestion == totalQuestions - 1) {
                        onComplete(1L) // attemptId
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
