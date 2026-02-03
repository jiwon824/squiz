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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// ScheduleListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListScreen(
    onBackClick: () -> Unit,
    onSessionClick: (Long, Long) -> Unit,  // (studyId, sessionId)
    onCalendarClick: () -> Unit,
    onGoogleSyncClick: () -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val schedulesState by viewModel.schedulesState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
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
                    IconButton(onClick = { viewModel.loadSchedules() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "새로고침")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = schedulesState) {
            is SchedulesState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SchedulesState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadSchedules() }) { Text("다시 시도") }
                    }
                }
            }
            is SchedulesState.Success -> {
                if (state.schedules.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "일정이 없습니다",
                        description = "예정된 스터디 일정이 없습니다.",
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.schedules) { schedule ->
                            ScheduleCard(
                                schedule = schedule,
                                onClick = { onSessionClick(schedule.studyId, schedule.sessionId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCard(schedule: ScheduleDTO, onClick: () -> Unit) {
    val displayDate = formatScheduleDate(schedule.date)
    val timeRange = "${schedule.startTime} - ${schedule.endTime}"
    val locationText = schedule.location ?: if (schedule.isOnline) "온라인" else "장소 미정"

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
                    Text(schedule.studyName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(displayDate, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(timeRange, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (schedule.isOnline) Icons.Outlined.Videocam else Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(locationText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatScheduleDate(dateStr: String): String {
    return try {
        val today = java.time.LocalDate.now()
        val date = java.time.LocalDate.parse(dateStr)
        when {
            date == today -> "오늘"
            date == today.plusDays(1) -> "내일"
            else -> "${date.monthValue}월 ${date.dayOfMonth}일"
        }
    } catch (e: Exception) { dateStr }
}

// ScheduleCalendarScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    onBackClick: () -> Unit,
    onSessionClick: (Long, Long) -> Unit,  // (studyId, sessionId)
    viewModel: ScheduleViewModel = viewModel()
) {
    val calendarState by viewModel.calendarState.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadCalendarData()
    }

    Scaffold(topBar = { SquizTopBar(title = "캘린더", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.goToPreviousMonth() }) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달") }
                Text("${currentYear}년 ${currentMonth}월", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.goToNextMonth() }) { Icon(Icons.Default.ChevronRight, contentDescription = "다음 달") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = calendarState) {
                is CalendarState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is CalendarState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.loadCalendarData() }) { Text("다시 시도") }
                        }
                    }
                }
                is CalendarState.Success -> {
                    val scheduledDays = state.data.scheduledDays.toSet()
                    val schedules = state.data.schedules

                    // 달의 첫째날 요일 계산
                    val firstDayOfMonth = java.time.LocalDate.of(currentYear, currentMonth, 1)
                    val startOffset = firstDayOfMonth.dayOfWeek.value % 7
                    val daysInMonth = firstDayOfMonth.lengthOfMonth()

                    // 선택된 날짜의 일정 목록
                    var selectedDaySchedules by remember { mutableStateOf<List<ScheduleDTO>>(emptyList()) }
                    var selectedDay by remember { mutableStateOf<Int?>(null) }

                    LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 빈 칸
                        items(startOffset) { Box(modifier = Modifier.aspectRatio(1f)) }
                        // 날짜
                        items(daysInMonth) { index ->
                            val day = index + 1
                            // LocalDate 객체로 비교 (문자열 비교 대신)
                            val targetDate = java.time.LocalDate.of(currentYear, currentMonth, day)
                            val dayScheduleList = schedules.filter { schedule ->
                                try {
                                    val scheduleDate = java.time.LocalDate.parse(schedule.date.take(10)) // "YYYY-MM-DD" 부분만 파싱
                                    scheduleDate == targetDate
                                } catch (e: Exception) {
                                    // 파싱 실패 시 문자열 비교 시도
                                    schedule.date.startsWith(targetDate.toString())
                                }
                            }
                            val hasSchedule = dayScheduleList.isNotEmpty()

                            Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(if (hasSchedule) Primary.copy(alpha = 0.15f) else Color.Transparent).clickable {
                                if (dayScheduleList.isNotEmpty()) {
                                    if (dayScheduleList.size == 1) {
                                        // 일정이 1개면 바로 이동
                                        onSessionClick(dayScheduleList.first().studyId, dayScheduleList.first().sessionId)
                                    } else {
                                        // 일정이 여러개면 선택 목록 표시
                                        selectedDay = day
                                        selectedDaySchedules = dayScheduleList
                                    }
                                }
                            }, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$day", fontSize = 14.sp, fontWeight = if (hasSchedule) FontWeight.Bold else FontWeight.Normal, color = if (hasSchedule) Primary else MaterialTheme.colorScheme.onSurface)
                                    if (hasSchedule) {
                                        Box(modifier = Modifier.size(4.dp).background(Primary, CircleShape))
                                    }
                                }
                            }
                        }
                    }

                    // 선택된 날짜의 일정 목록 표시
                    if (selectedDaySchedules.isNotEmpty() && selectedDay != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "${currentMonth}월 ${selectedDay}일 일정",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        selectedDaySchedules.forEach { schedule ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onSessionClick(schedule.studyId, schedule.sessionId) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Primary, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            schedule.studyName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${schedule.startTime} - ${schedule.endTime}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
fun ScheduleDetailScreen(
    studyId: Long,
    sessionId: Long,
    onBackClick: () -> Unit,
    onAttendanceClick: (Boolean) -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val sessionDetailState by viewModel.sessionDetailState.collectAsState()

    LaunchedEffect(studyId, sessionId) {
        viewModel.loadSessionDetail(studyId, sessionId)
    }

    Scaffold(topBar = { SquizTopBar(title = "일정 상세", onBackClick = onBackClick) }) { padding ->
        when (val state = sessionDetailState) {
            is SessionDetailState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SessionDetailState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadSessionDetail(studyId, sessionId) }) { Text("다시 시도") }
                    }
                }
            }
            is SessionDetailState.Success -> {
                val detail = state.session
                val locationText = detail.location ?: if (detail.isOnline) "온라인" else "장소 미정"
                val timeRange = "${detail.startTime} - ${detail.endTime ?: ""}"

                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(detail.studyName ?: "스터디", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            detail.title?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(it, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(timeRange, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (detail.isOnline) Icons.Outlined.Videocam else Icons.Outlined.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(locationText, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }
                    }

                    // 출석 상태 표시
                    detail.attendanceStatus?.let { status ->
                        Spacer(modifier = Modifier.height(16.dp))
                        val statusText = when (status) {
                            "PRESENT" -> "출석 완료"
                            "LATE" -> "지각"
                            "ABSENT" -> "결석"
                            else -> status
                        }
                        val statusColor = when (status) {
                            "PRESENT" -> Success
                            "LATE" -> Warning
                            "ABSENT" -> Error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = statusColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("출석 상태: $statusText", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = statusColor)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (detail.attendanceStatus == null || detail.attendanceStatus == "PENDING") {
                        GradientButton(text = "출석체크", onClick = { onAttendanceClick(detail.isLeader == true) })
                    }
                }
            }
        }
    }
}

// GoogleCalendarSyncScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleCalendarSyncScreen(
    onBackClick: () -> Unit,
    viewModel: ScheduleViewModel = viewModel()
) {
    val googleSyncState by viewModel.googleSyncState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadGoogleSyncStatus()
    }

    Scaffold(topBar = { SquizTopBar(title = "Google 캘린더 연동", onBackClick = onBackClick) }) { padding ->
        when (val state = googleSyncState) {
            is GoogleSyncState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GoogleSyncState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadGoogleSyncStatus() }) { Text("다시 시도") }
                    }
                }
            }
            is GoogleSyncState.Success -> {
                val status = state.status
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Filled.Event, contentDescription = null, tint = if (status.isConnected) Success else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(if (status.isConnected) "연동 완료" else "Google 캘린더 연동", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (status.isConnected) {
                        status.email?.let {
                            Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text("스터디 일정이 자동으로 동기화됩니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        status.lastSyncTime?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("마지막 동기화: ${formatSyncTime(it)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Google 캘린더와 연동하여 일정을 관리하세요.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    if (status.isConnected) {
                        OutlinedButton(onClick = { viewModel.disconnectGoogleCalendar() }, shape = RoundedCornerShape(12.dp)) { Text("연동 해제") }
                    } else {
                        // 실제로는 Google OAuth 흐름을 시작해야 함
                        GradientButton(text = "Google 계정 연동", onClick = {
                            // TODO: Google OAuth 흐름 시작
                            // 성공 시 viewModel.connectGoogleCalendar(authCode) { } 호출
                        })
                    }
                }
            }
        }
    }
}

private fun formatSyncTime(timestamp: String): String {
    return try {
        val parts = timestamp.split("T")
        if (parts.size >= 2) {
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            if (dateParts.size >= 3 && timeParts.size >= 2) {
                "${dateParts[1].toInt()}/${dateParts[2].toInt()} ${timeParts[0]}:${timeParts[1]}"
            } else timestamp
        } else timestamp
    } catch (e: Exception) { timestamp }
}
