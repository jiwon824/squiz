package com.ssafy.squiz.ui.screens.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

private const val TAG = "AttendanceScreens"

// AttendanceMemberScreen - BLE로 출석하는 멤버용 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceMemberScreen(
    studyId: Long,
    sessionId: Long,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val attendanceState by viewModel.attendanceState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val foundBeacon by viewModel.foundBeacon.collectAsState()

    // BLE 권한 목록 (Android 12+ 새로운 권한)
    val blePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // 권한 상태
    var permissionsGranted by remember { mutableStateOf(false) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            Log.d(TAG, "BLE 권한 승인됨, 스캔 시작")
            viewModel.startBleCheck(studyId, sessionId)
        } else {
            Log.w(TAG, "BLE 권한 거부됨")
            Toast.makeText(context, "BLE 출석에는 블루투스와 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // 화면 진입 시 권한 확인 후 자동 스캔 시작
    LaunchedEffect(studyId, sessionId) {
        val allGranted = blePermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            Log.d(TAG, "BLE 권한 이미 승인됨, 스캔 시작")
            permissionsGranted = true
            viewModel.startBleCheck(studyId, sessionId)
        } else {
            Log.d(TAG, "BLE 권한 요청 필요")
            permissionLauncher.launch(blePermissions)
        }
    }

    // 화면 이탈 시 스캔 중지
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopBleCheck()
        }
    }

    // 출석 성공 시 화면 이동
    LaunchedEffect(attendanceState) {
        if (attendanceState is AttendanceUiState.Success) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = { SquizTopBar(title = "출석체크", onBackClick = {
            viewModel.stopBleCheck()
            onBackClick()
        }) }
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
                    .then(
                        if (isScanning) Modifier.scale(scale) else Modifier
                    )
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
                                colors = when (attendanceState) {
                                    is AttendanceUiState.Success -> listOf(Success, Success)
                                    is AttendanceUiState.Error -> listOf(Error, Error)
                                    else -> listOf(GradientStart, GradientEnd)
                                }
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (attendanceState) {
                            is AttendanceUiState.Success -> Icons.Filled.Check
                            is AttendanceUiState.Error -> Icons.Filled.Close
                            else -> Icons.Filled.Bluetooth
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 상태별 메시지
            when (val state = attendanceState) {
                is AttendanceUiState.Scanning -> {
                    Text(
                        text = "스터디장을 찾는 중...",
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
                    // 발견된 Beacon 거리 표시
                    foundBeacon?.let { beacon ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "신호 감지됨 (${String.format("%.1f", beacon.distance)}m)",
                            fontSize = 14.sp,
                            color = Success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is AttendanceUiState.Success -> {
                    Text(
                        text = state.message,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Success
                    )
                }
                is AttendanceUiState.Error -> {
                    Text(
                        text = "출석 실패",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.startBleCheck(studyId, sessionId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("다시 시도")
                    }
                }
                else -> {
                    Text(
                        text = "준비 중...",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Bluetooth 꺼져있을 때 안내
            if (!viewModel.isBluetoothEnabled()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Warning
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bluetooth를 켜주세요",
                            color = Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// AttendanceLeaderScreen - BLE로 출석받는 스터디장용 화면
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceLeaderScreen(
    studyId: Long,
    sessionId: Long,
    onBackClick: () -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val attendanceState by viewModel.attendanceState.collectAsState()
    val sessionAttendanceState by viewModel.sessionAttendanceState.collectAsState()
    val isAdvertising by viewModel.isAdvertising.collectAsState()

    // BLE 권한 목록 (Android 12+ 새로운 권한)
    val blePermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // 권한 상태
    var permissionsGranted by remember { mutableStateOf(false) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            Log.d(TAG, "BLE 권한 승인됨, 광고 시작")
            viewModel.startBleAttendance(studyId, sessionId)
        } else {
            Log.w(TAG, "BLE 권한 거부됨")
            Toast.makeText(context, "BLE 출석에는 블루투스와 위치 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 화면 진입 시 권한 확인 후 자동 광고 시작
    LaunchedEffect(studyId, sessionId) {
        val allGranted = blePermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            Log.d(TAG, "BLE 권한 이미 승인됨, 광고 시작")
            permissionsGranted = true
            viewModel.startBleAttendance(studyId, sessionId)
        } else {
            Log.d(TAG, "BLE 권한 요청 필요")
            permissionLauncher.launch(blePermissions)
        }
    }

    // 화면 이탈 시 광고 중지
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopBleAttendance()
        }
    }

    Scaffold(
        topBar = { SquizTopBar(title = "출석 관리", onBackClick = {
            viewModel.stopBleAttendance()
            onBackClick()
        }) }
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
                colors = CardDefaults.cardColors(
                    containerColor = if (isAdvertising) Primary else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isAdvertising) Icons.Filled.BluetoothSearching else Icons.Outlined.BluetoothDisabled,
                        contentDescription = null,
                        tint = if (isAdvertising) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isAdvertising) "BLE 출석 진행 중" else "BLE 출석 준비 중...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdvertising) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 출석 현황
                    when (val state = sessionAttendanceState) {
                        is SessionAttendanceUiState.Success -> {
                            Text(
                                text = "${state.info.presentCount} / ${state.info.totalMembers}명 출석",
                                fontSize = 14.sp,
                                color = if (isAdvertising) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = "출석 현황 로딩 중...",
                                fontSize = 14.sp,
                                color = if (isAdvertising) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 에러 상태 표시
                    if (attendanceState is AttendanceUiState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (attendanceState as AttendanceUiState.Error).message,
                            fontSize = 12.sp,
                            color = Error
                        )
                    }
                }
            }

            // 광고 시작/중지 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isAdvertising) {
                    Button(
                        onClick = { viewModel.stopBleAttendance() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Error)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("출석 종료")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startBleAttendance(studyId, sessionId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("출석 시작")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 출석 목록
            when (val state = sessionAttendanceState) {
                is SessionAttendanceUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is SessionAttendanceUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = Error
                        )
                    }
                }
                is SessionAttendanceUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.info.members) { member ->
                            val isPresent = member.status == "PRESENT"
                            val isLate = member.status == "LATE"

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isPresent -> Success.copy(alpha = 0.1f)
                                        isLate -> Warning.copy(alpha = 0.1f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProfileImage(imageUrl = member.profileImage, size = 44.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = member.nickname,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (member.checkedAt != null) {
                                            Text(
                                                text = member.checkedAt.takeLast(8),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    when (member.status) {
                                        "PRESENT" -> {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "출석",
                                                tint = Success
                                            )
                                        }
                                        "LATE" -> {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Warning.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "지각",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 12.sp,
                                                    color = Warning,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        else -> {
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

            // Bluetooth 꺼져있을 때 안내
            if (!viewModel.isBluetoothEnabled()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Warning
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bluetooth를 켜주세요",
                            color = Warning,
                            fontWeight = FontWeight.Medium
                        )
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
    studyId: Long,
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
    onSessionClick: (Long) -> Unit,
    viewModel: AttendanceViewModel = viewModel()
) {
    val statsState by viewModel.statsState.collectAsState()
    val historyState by viewModel.historyState.collectAsState()

    // 데이터 로드
    LaunchedEffect(studyId) {
        viewModel.loadAttendanceStats(studyId)
        viewModel.loadAttendanceHistory(studyId)
    }

    // 출석한 날짜 추출
    val attendedDays = remember(historyState) {
        when (val state = historyState) {
            is AttendanceHistoryUiState.Success -> {
                state.history
                    .filter { it.status == "PRESENT" || it.status == "LATE" }
                    .mapNotNull {
                        try {
                            it.sessionDate.substring(8, 10).toInt()
                        } catch (e: Exception) { null }
                    }
                    .toSet()
            }
            else -> emptySet()
        }
    }

    val lateDays = remember(historyState) {
        when (val state = historyState) {
            is AttendanceHistoryUiState.Success -> {
                state.history
                    .filter { it.status == "LATE" }
                    .mapNotNull {
                        try {
                            it.sessionDate.substring(8, 10).toInt()
                        } catch (e: Exception) { null }
                    }
                    .toSet()
            }
            else -> emptySet()
        }
    }

    val days = (1..31).toList()

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
                    val isLate = lateDays.contains(day)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isLate -> AttendanceLate.copy(alpha = 0.15f)
                                    isAttended -> AttendancePresent.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSessionClick(day.toLong()) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$day",
                            fontSize = 14.sp,
                            fontWeight = if (isAttended) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isLate -> AttendanceLate
                                isAttended -> AttendancePresent
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 통계
            when (val state = statsState) {
                is AttendanceStatsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary, modifier = Modifier.size(24.dp))
                    }
                }
                is AttendanceStatsUiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = state.message,
                            modifier = Modifier.padding(16.dp),
                            color = Error
                        )
                    }
                }
                is AttendanceStatsUiState.Success -> {
                    val stats = state.stats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatItem("출석", "${stats.presentCount}회", AttendancePresent)
                                StatItem("지각", "${stats.lateCount}회", AttendanceLate)
                                StatItem("결석", "${stats.absentCount}회", AttendanceAbsent)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            // 출석률
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "출석률: ${(stats.attendanceRate * 100).toInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
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
