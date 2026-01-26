package com.ssafy.squiz.ui.screens.daily

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// DailyReportScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportScreen(studyId: Long, onBackClick: () -> Unit, onHistoryClick: () -> Unit) {
    var todayReport by remember { mutableStateOf("") }
    var studyTime by remember { mutableStateOf("2시간 30분") }

    Scaffold(
        topBar = {
            SquizTopBar(title = "데일리 리포트", onBackClick = onBackClick, actions = {
                IconButton(onClick = onHistoryClick) { Icon(Icons.Outlined.History, contentDescription = "히스토리") }
            })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Today's Stats Card
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("오늘의 학습", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(studyTime, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatChip(icon = Icons.Outlined.MenuBook, value = "3", label = "문제")
                        StatChip(icon = Icons.Outlined.Check, value = "2", label = "완료")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Report Input
            Text("오늘의 학습 기록", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = todayReport,
                onValueChange = { todayReport = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("오늘 무엇을 학습했나요?") },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            GradientButton(text = "저장하기", onClick = { onBackClick() })
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("$value $label", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

// DailyHistoryScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHistoryScreen(studyId: Long, onBackClick: () -> Unit) {
    val history = listOf(
        DailyRecord("1월 15일", "2시간 30분", "DFS/BFS 알고리즘 학습"),
        DailyRecord("1월 14일", "1시간 45분", "스택/큐 복습"),
        DailyRecord("1월 13일", "3시간 00분", "동적 프로그래밍 기초")
    )

    Scaffold(topBar = { SquizTopBar(title = "학습 히스토리", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { record ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(record.date, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(record.studyTime, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(record.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private data class DailyRecord(val date: String, val studyTime: String, val content: String)

// RetrospectiveListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrospectiveListScreen(studyId: Long, onBackClick: () -> Unit, onWriteClick: () -> Unit) {
    val retrospectives = listOf(
        Retrospective(1, "1주차 회고", "스택/큐를 잘 이해했다!", "1월 15일"),
        Retrospective(2, "2주차 회고", "DFS/BFS 어려웠지만 극복!", "1월 22일")
    )

    Scaffold(
        topBar = { SquizTopBar(title = "회고", onBackClick = onBackClick) },
        floatingActionButton = {
            FloatingActionButton(onClick = onWriteClick, containerColor = Primary) {
                Icon(Icons.Default.Add, contentDescription = "작성", tint = Color.White)
            }
        }
    ) { padding ->
        if (retrospectives.isEmpty()) {
            EmptyState(icon = Icons.Outlined.Psychology, title = "회고가 없습니다", description = "첫 번째 회고를 작성해보세요!", modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(retrospectives) { retro ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(retro.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(retro.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(retro.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private data class Retrospective(val id: Long, val title: String, val content: String, val date: String)

// RetrospectiveWriteScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetrospectiveWriteScreen(studyId: Long, onBackClick: () -> Unit, onSuccess: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var keep by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf("") }
    var tryNext by remember { mutableStateOf("") }

    Scaffold(topBar = { SquizTopBar(title = "회고 작성", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            // KPT Section
            item { RetroSection(title = "Keep", subtitle = "잘한 점, 계속할 점", value = keep, onValueChange = { keep = it }, color = Success) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { RetroSection(title = "Problem", subtitle = "아쉬운 점, 개선할 점", value = problem, onValueChange = { problem = it }, color = Error) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item { RetroSection(title = "Try", subtitle = "다음에 시도할 점", value = tryNext, onValueChange = { tryNext = it }, color = Info) }

            item { Spacer(modifier = Modifier.height(32.dp)) }
            item { GradientButton(text = "저장하기", onClick = onSuccess, enabled = title.isNotBlank()) }
        }
    }
}

@Composable
private fun RetroSection(title: String, subtitle: String, value: String, onValueChange: (String) -> Unit, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(100.dp), placeholder = { Text("내용을 입력하세요") }, shape = RoundedCornerShape(12.dp))
    }
}
