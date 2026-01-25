package com.ssafy.squiz.ui.screens.contest

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*
import kotlinx.coroutines.delay

// ContestListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestListScreen(onBackClick: () -> Unit, onContestClick: (Long) -> Unit, onHistoryClick: () -> Unit, onMyRecordsClick: () -> Unit) {
    val contests = listOf(Triple(1L, "알고리즘 챌린지", "오늘 15:00"), Triple(2L, "자료구조 마스터", "내일 14:00"), Triple(3L, "CS 기초 대회", "1월 20일 10:00"))

    Scaffold(
        topBar = {
            SquizTopBar(title = "퀴즈 대회", onBackClick = onBackClick, actions = {
                IconButton(onClick = onHistoryClick) { Icon(Icons.Outlined.History, contentDescription = "히스토리") }
                IconButton(onClick = onMyRecordsClick) { Icon(Icons.Outlined.EmojiEvents, contentDescription = "내 기록") }
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(contests) { (id, title, time) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onContestClick(id) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(56.dp).background(brush = Brush.linearGradient(listOf(Warning, Warning.copy(alpha = 0.7f))), shape = RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(time, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        StatusBadge(text = if (id == 1L) "진행중" else "예정", color = if (id == 1L) Success else Info)
                    }
                }
            }
        }
    }
}

// ContestWaitingScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestWaitingScreen(contestId: Long, onBackClick: () -> Unit, onStart: () -> Unit) {
    var countdown by remember { mutableStateOf(10) }
    val participants = 15

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        onStart()
    }

    Scaffold(topBar = { SquizTopBar(title = "대기실", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("알고리즘 챌린지", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.size(150.dp).background(brush = Brush.linearGradient(listOf(Warning, Warning.copy(alpha = 0.7f))), shape = CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$countdown", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("초", fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.People, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("$participants 명 참여 중", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ContestPlayScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestPlayScreen(contestId: Long, onComplete: () -> Unit) {
    var currentQuestion by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var timeLeft by remember { mutableStateOf(30) }

    LaunchedEffect(currentQuestion) {
        timeLeft = 30
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        if (currentQuestion < 9) {
            currentQuestion++
            selectedAnswer = null
        } else {
            onComplete()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("문제 ${currentQuestion + 1}/10") }, actions = { Text("$timeLeft 초", modifier = Modifier.padding(end = 16.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (timeLeft <= 10) Error else Warning) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            LinearProgressIndicator(progress = { (currentQuestion + 1) / 10f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Warning)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Q${currentQuestion + 1}. 스택의 시간 복잡도는?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))

            listOf("O(1)", "O(n)", "O(log n)", "O(n²)").forEachIndexed { index, option ->
                val isSelected = selectedAnswer == index
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selectedAnswer = index }.border(2.dp, if (isSelected) Warning else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Warning.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                ) {
                    Text(option, modifier = Modifier.padding(16.dp), fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { if (currentQuestion < 9) { currentQuestion++; selectedAnswer = null } else onComplete() }, modifier = Modifier.fillMaxWidth(), enabled = selectedAnswer != null, colors = ButtonDefaults.buttonColors(containerColor = Warning)) {
                Text(if (currentQuestion < 9) "다음" else "제출", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ContestResultScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestResultScreen(contestId: Long, onBackClick: () -> Unit, onDetailClick: () -> Unit) {
    val rank = 3
    val score = 850

    Scaffold(topBar = { SquizTopBar(title = "결과", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Warning, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("${rank}위", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Warning)
            Text("$score 점", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(40.dp))

            // Leaderboard
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("순위표", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOf(Triple(1, "김철수", 950), Triple(2, "이영희", 900), Triple(3, "나", 850)).forEach { (r, name, s) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("$r", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (r <= 3) Warning else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
                            ProfileImage(imageUrl = null, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, modifier = Modifier.weight(1f), fontWeight = if (name == "나") FontWeight.Bold else FontWeight.Normal)
                            Text("$s", fontWeight = FontWeight.SemiBold, color = Primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            GradientButton(text = "상세 보기", onClick = onDetailClick)
        }
    }
}

// ContestHistoryScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestHistoryScreen(onBackClick: () -> Unit, onContestClick: (Long) -> Unit) {
    val history = listOf(Triple(1L, "알고리즘 챌린지", "3위 / 850점"), Triple(2L, "자료구조 대회", "1위 / 980점"), Triple(3L, "CS 기초", "5위 / 720점"))

    Scaffold(topBar = { SquizTopBar(title = "대회 히스토리", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { (id, title, result) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onContestClick(id) }, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Warning)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(result, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ContestResultDetailScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestResultDetailScreen(contestId: Long, onBackClick: () -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "상세 결과", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("알고리즘 챌린지", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("3위 / 850점", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Warning)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("8/10 정답 • 5분 30초 소요", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("문제별 결과", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            (1..10).forEach { q ->
                val isCorrect = q !in listOf(3, 7)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("문제 $q", modifier = Modifier.weight(1f))
                    Icon(if (isCorrect) Icons.Default.Check else Icons.Default.Close, contentDescription = null, tint = if (isCorrect) Success else Error)
                }
            }
        }
    }
}

// MyContestRecordsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyContestRecordsScreen(onBackClick: () -> Unit, onContestClick: (Long) -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "내 기록", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Stats Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("12", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("참여", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("3", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("1위", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("850", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("평균점수", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("최근 기록", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            listOf(Triple(1L, "알고리즘 챌린지", "3위"), Triple(2L, "자료구조 대회", "1위")).forEach { (id, title, rank) ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onContestClick(id) }, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp)
                        Text(rank, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Warning)
                    }
                }
            }
        }
    }
}
