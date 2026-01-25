package com.ssafy.squiz.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

// ScheduleListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    onBackClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onCalendarClick: () -> Unit,
    onGoogleSyncClick: () -> Unit
) {
    val schedules = remember {
        listOf(
            ScheduleItem(1, "알고리즘 스터디", "14:00 - 16:00", "Discord", "오늘"),
            ScheduleItem(2, "Spring Boot", "10:00 - 12:00", "Zoom", "내일"),
            ScheduleItem(3, "SQLD 스터디", "19:00 - 21:00", "강남역 스터디룸", "1월 20일")
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "일정",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = onCalendarClick) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "캘린더")
                    }
                    IconButton(onClick = onGoogleSyncClick) {
                        Icon(Icons.Outlined.Sync, contentDescription = "동기화")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(schedules) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    onClick = { onSessionClick(schedule.id) }
                )
            }
        }
    }
}

@Composable
private fun ScheduleCard(schedule: ScheduleItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(
                        brush = Brush.verticalGradient(listOf(GradientStart, GradientEnd)),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(schedule.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(schedule.date, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(schedule.time, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(schedule.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private data class ScheduleItem(val id: Long, val title: String, val time: String, val location: String, val date: String)

// ScheduleCalendarScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(onBackClick: () -> Unit, onSessionClick: (Long) -> Unit) {
    val scheduledDays = setOf(5, 10, 15, 20, 25)

    Scaffold(topBar = { SquizTopBar(title = "캘린더", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { }) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달") }
                Text("2024년 1월", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { }) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 달") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(2) { Box(modifier = Modifier.aspectRatio(1f)) }
                items((1..31).toList()) { day ->
                    val hasSchedule = scheduledDays.contains(day)
                    Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(if (hasSchedule) Primary.copy(alpha = 0.15f) else Color.Transparent).clickable { if (hasSchedule) onSessionClick(day.toLong()) }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$day", fontSize = 14.sp, fontWeight = if (hasSchedule) FontWeight.Bold else FontWeight.Normal, color = if (hasSchedule) Primary else MaterialTheme.colorScheme.onSurface)
                            if (hasSchedule) {
                                Box(modifier = Modifier.size(4.dp).background(Primary, CircleShape))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ScheduleDetailScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(sessionId: Long, onBackClick: () -> Unit, onAttendanceClick: (Boolean) -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "일정 상세", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("알고리즘 스터디", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("14:00 - 16:00", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Discord", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            GradientButton(text = "출석체크", onClick = { onAttendanceClick(false) })
        }
    }
}

// GoogleCalendarSyncScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleCalendarSyncScreen(onBackClick: () -> Unit) {
    var isConnected by remember { mutableStateOf(false) }

    Scaffold(topBar = { SquizTopBar(title = "Google 캘린더 연동", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Event, contentDescription = null, tint = if (isConnected) Success else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(if (isConnected) "연동 완료" else "Google 캘린더 연동", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (isConnected) "스터디 일정이 자동으로 동기화됩니다." else "Google 캘린더와 연동하여 일정을 관리하세요.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(32.dp))
            if (isConnected) {
                OutlinedButton(onClick = { isConnected = false }, shape = RoundedCornerShape(12.dp)) { Text("연동 해제") }
            } else {
                GradientButton(text = "Google 계정 연동", onClick = { isConnected = true })
            }
        }
    }
}
