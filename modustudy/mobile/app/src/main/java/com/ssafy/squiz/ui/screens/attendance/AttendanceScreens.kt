package com.ssafy.squiz.ui.screens.attendance

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// AttendanceMemberScreen - BLE로 출석하는 멤버용 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMemberScreen(
    sessionId: Long,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    var isSearching by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )

    Scaffold(
        topBar = { SquizTopBar(title = "출석체크", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // BLE 탐색 애니메이션
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Primary.copy(alpha = 0.3f),
                                Primary.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(GradientStart, GradientEnd)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bluetooth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = if (isSearching) "스터디장을 찾는 중..." else "출석 완료!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "스터디장 근처에서 대기해주세요",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 디버그용 버튼 (실제로는 BLE로 자동 출석)
            OutlinedButton(
                onClick = onSuccess,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("수동 출석 (테스트)")
            }
        }
    }
}

// AttendanceLeaderScreen - BLE로 출석받는 스터디장용 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceLeaderScreen(
    sessionId: Long,
    onBackClick: () -> Unit
) {
    val attendees = remember {
        mutableStateListOf(
            Pair("김철수", true),
            Pair("이영희", true),
            Pair("박지민", false),
            Pair("최민수", false),
            Pair("정수진", false)
        )
    }
    val attendedCount = attendees.count { it.second }

    Scaffold(
        topBar = { SquizTopBar(title = "출석 관리", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 상태 카드
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.BluetoothSearching,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "BLE 출석 진행 중",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$attendedCount / ${attendees.size}명 출석",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // 출석 목록
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attendees) { (name, attended) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (attended) Success.copy(alpha = 0.1f)
                                           else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileImage(imageUrl = null, size = 44.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            if (attended) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "출석",
                                    tint = Success
                                )
                            } else {
                                Text(
                                    text = "대기중",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// AttendanceSuccessScreen - 출석 성공 화면
@Composable
fun AttendanceSuccessScreen(
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Success.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "출석 완료!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "성공적으로 출석이 완료되었습니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "확인",
                onClick = onConfirm,
                modifier = Modifier.padding(horizontal = 48.dp)
            )
        }
    }
}

// SelfAttendanceScreen - 셀프 출석 (위치 기반)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfAttendanceScreen(
    sessionId: Long,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    Scaffold(
        topBar = { SquizTopBar(title = "셀프 출석", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "셀프 출석",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "현재 위치를 확인하여 출석합니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = "출석하기",
                onClick = onSuccess
            )
        }
    }
}

// AttendanceCalendarScreen - 출석 캘린더
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCalendarScreen(
    studyId: Long,
    onBackClick: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    val days = (1..31).toList()
    val attendedDays = setOf(1, 3, 5, 8, 10, 12, 15, 17, 19, 22, 24, 26)

    Scaffold(
        topBar = { SquizTopBar(title = "출석 캘린더", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 월 선택
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                }
                Text(
                    text = "2024년 1월",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 요일 헤더
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("일", "월", "화", "수", "목", "금", "토").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 캘린더 그리드
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 빈 칸 (월의 시작 요일에 맞춤)
                items(2) {
                    Box(modifier = Modifier.aspectRatio(1f))
                }

                items(days) { day ->
                    val isAttended = attendedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isAttended) AttendancePresent.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { onSessionClick(day.toLong()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$day",
                            fontSize = 14.sp,
                            fontWeight = if (isAttended) FontWeight.Bold else FontWeight.Normal,
                            color = if (isAttended) AttendancePresent
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 통계
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("출석", "${attendedDays.size}회", AttendancePresent)
                    StatItem("지각", "2회", AttendanceLate)
                    StatItem("결석", "1회", AttendanceAbsent)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// SessionMemoScreen - 세션 메모
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionMemoScreen(
    sessionId: Long,
    onBackClick: () -> Unit
) {
    var memo by remember { mutableStateOf("") }

    Scaffold(
        topBar = { SquizTopBar(title = "세션 메모", onBackClick = onBackClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "1월 15일 스터디 메모",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("오늘 학습 내용을 기록해보세요...") },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            GradientButton(
                text = "저장",
                onClick = { onBackClick() }
            )
        }
    }
}
