package com.ssafy.squiz.ui.screens.meeting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.MeetingDTO
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

private const val TAG = "MeetingListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingListScreen(
    studyId: Long,
    onBackClick: () -> Unit,
    onMeetingClick: (Long) -> Unit,
    viewModel: MeetingViewModel = viewModel()
) {
    val context = LocalContext.current
    val meetingsState by viewModel.meetingsState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    // 필요한 권한 목록
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            // Android 13+ 알림 권한
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(TAG, "모든 권한 승인됨, 녹음 시작")
            viewModel.startRecording(context, studyId)
        } else {
            Log.w(TAG, "권한 거부됨: $permissions")
            Toast.makeText(context, "녹음에는 마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 권한 확인 후 녹음 시작
    fun checkPermissionsAndStartRecording() {
        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            Log.d(TAG, "권한 이미 승인됨, 녹음 시작")
            viewModel.startRecording(context, studyId)
        } else {
            Log.d(TAG, "권한 요청 필요: ${requiredPermissions.toList()}")
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // 데이터 로드
    LaunchedEffect(studyId) {
        viewModel.loadMeetings(studyId, refresh = true)
    }

    // 업로드 상태 처리
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is UploadUiState.Success -> {
                snackbarHostState.showSnackbar((uploadState as UploadUiState.Success).message)
                viewModel.resetUploadState()
                viewModel.loadMeetings(studyId, refresh = true)
            }
            is UploadUiState.Error -> {
                snackbarHostState.showSnackbar((uploadState as UploadUiState.Error).message)
                viewModel.resetUploadState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            PremiumTopAppBar(
                title = "회의록",
                onBackClick = onBackClick,
                actions = {
                    // 새로고침 버튼
                    IconButton(onClick = { viewModel.loadMeetings(studyId, refresh = true) }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "새로고침",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // 프리미엄 FAB
            if (recordingState.isRecording && recordingState.studyId == studyId) {
                // 녹음 중지 FAB (확장형)
                ExtendedFloatingActionButton(
                    onClick = { viewModel.stopRecordingAndUpload(context) },
                    modifier = Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Error.copy(alpha = 0.3f),
                        spotColor = Error.copy(alpha = 0.3f)
                    ),
                    containerColor = Error,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    PulsingDot(color = Color.White, size = 10.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "녹음 중지",
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formatDuration(recordingState.elapsedSeconds),
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // 녹음 시작 FAB (프리미엄)
                FloatingActionButton(
                    onClick = { checkPermissionsAndStartRecording() },
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = GradientPrimaryStart.copy(alpha = 0.3f),
                            spotColor = GradientPrimaryEnd.copy(alpha = 0.3f)
                        ),
                    shape = CircleShape,
                    containerColor = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(GradientPrimaryStart, GradientPrimaryEnd)
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "녹음 시작",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 프리미엄 녹음 중 상태바
            if (recordingState.isRecording && recordingState.studyId == studyId) {
                PremiumRecordingStatusBar(
                    elapsedSeconds = recordingState.elapsedSeconds,
                    isPaused = recordingState.isPaused
                )
            }

            // 프리미엄 업로드 중 표시
            if (uploadState is UploadUiState.Uploading) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GradientLoadingIndicator(size = 24.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI가 회의록을 분석하고 있어요...",
                            fontSize = 14.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            when (val state = meetingsState) {
                is MeetingsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        GradientLoadingIndicator(size = 56.dp)
                    }
                }

                is MeetingsUiState.Error -> {
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
                                    text = state.message,
                                    color = Error,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                GradientButton(
                                    text = "다시 시도",
                                    onClick = { viewModel.loadMeetings(studyId, refresh = true) },
                                    icon = Icons.Default.Refresh,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                is MeetingsUiState.Success -> {
                    if (state.meetings.isEmpty()) {
                        // 프리미엄 빈 상태
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .background(
                                            brush = Brush.linearGradient(
                                                listOf(
                                                    GradientPrimaryStart.copy(alpha = 0.1f),
                                                    GradientPrimaryEnd.copy(alpha = 0.1f)
                                                )
                                            ),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "회의록이 없습니다",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "세션을 녹음하면 AI가 자동으로\n회의록을 생성해드려요!",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                GradientButton(
                                    text = "녹음 시작하기",
                                    onClick = { checkPermissionsAndStartRecording() },
                                    icon = Icons.Default.Mic
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 안내 카드
                            item {
                                PremiumInfoCard()
                            }

                            items(state.meetings) { meeting ->
                                PremiumMeetingCard(
                                    meeting = meeting,
                                    onClick = { onMeetingClick(meeting.id) }
                                )
                            }

                            if (state.hasMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GradientLoadingIndicator(size = 32.dp)
                                    }
                                    LaunchedEffect(Unit) {
                                        viewModel.loadMore(studyId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumInfoCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GradientSecondaryStart.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(GradientSecondaryStart, GradientSecondaryEnd)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI 회의록 자동 생성",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GradientSecondaryStart
                )
                Text(
                    text = "녹음이 끝나면 AI가 자동으로 요약해드려요",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PremiumRecordingStatusBar(
    elapsedSeconds: Long,
    isPaused: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isPaused) Warning.copy(alpha = 0.15f) else Error.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 프리미엄 펄싱 점
                if (!isPaused) {
                    PulsingDot(color = Error, size = 14.dp)
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(Warning, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isPaused) "녹음 일시정지" else "녹음 중",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPaused) Warning else Error
                    )
                    Text(
                        text = "AI가 실시간으로 분석 준비 중",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 프리미엄 타이머
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isPaused) Warning.copy(alpha = 0.2f) else Error.copy(alpha = 0.2f)
            ) {
                Text(
                    text = formatDuration(elapsedSeconds),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isPaused) Warning else Error
                )
            }
        }
    }
}

@Composable
private fun PremiumMeetingCard(
    meeting: MeetingDTO,
    onClick: () -> Unit
) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 아이콘 + 제목
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 프리미엄 아이콘
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                brush = when (meeting.status) {
                                    "COMPLETED" -> Brush.linearGradient(
                                        listOf(GradientPrimaryStart.copy(alpha = 0.15f), GradientPrimaryEnd.copy(alpha = 0.15f))
                                    )
                                    "PROCESSING" -> Brush.linearGradient(
                                        listOf(GradientWarningStart.copy(alpha = 0.15f), GradientWarningEnd.copy(alpha = 0.15f))
                                    )
                                    else -> Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (meeting.status) {
                                "COMPLETED" -> Icons.Outlined.Description
                                "PROCESSING" -> Icons.Outlined.Pending
                                "RECORDING" -> Icons.Default.Mic
                                else -> Icons.Outlined.AudioFile
                            },
                            contentDescription = null,
                            tint = when (meeting.status) {
                                "COMPLETED" -> Primary
                                "PROCESSING" -> Warning
                                "RECORDING" -> Error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = meeting.title ?: "세션 ${meeting.id}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = meeting.startedAt?.take(16)?.replace("T", " ") ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 프리미엄 상태 칩
                StatusChip(
                    text = when (meeting.status) {
                        "RECORDING" -> "녹음 중"
                        "PROCESSING" -> "처리 중"
                        "COMPLETED" -> "완료"
                        "FAILED" -> "실패"
                        else -> "대기"
                    },
                    color = when (meeting.status) {
                        "RECORDING" -> Error
                        "PROCESSING" -> Warning
                        "COMPLETED" -> Success
                        "FAILED" -> Error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    icon = when (meeting.status) {
                        "RECORDING" -> Icons.Default.FiberManualRecord
                        "PROCESSING" -> Icons.Default.Pending
                        "COMPLETED" -> Icons.Default.CheckCircle
                        else -> null
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 프리미엄 하단 정보
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 녹음 시간
                PremiumInfoItem(
                    icon = Icons.Outlined.Timer,
                    text = meeting.displayDuration,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 구분선
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )

                // 참석자 수
                PremiumInfoItem(
                    icon = Icons.Outlined.People,
                    text = "${meeting.participantCount ?: 0}명",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // AI 요약 여부
                if (meeting.hasSummary) {
                    // 구분선
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    PremiumInfoItem(
                        icon = Icons.Default.AutoAwesome,
                        text = "AI 요약",
                        color = Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
