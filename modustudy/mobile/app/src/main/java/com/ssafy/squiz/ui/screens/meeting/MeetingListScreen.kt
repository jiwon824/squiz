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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    sessionId: Long? = null,  // 세션과 연결된 경우 전달 (일정 화면에서 진입 시)
    isLeader: Boolean = false,  // 스터디장 여부 (녹음 기능은 스터디장만 사용 가능)
    onBackClick: () -> Unit,
    onMeetingClick: (Long) -> Unit,
    viewModel: MeetingViewModel = viewModel()
) {
    val context = LocalContext.current
    val meetingsState by viewModel.meetingsState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val showSessionSelectDialog by viewModel.showSessionSelectDialog.collectAsState()
    val showRecordingCompleteDialog by viewModel.showRecordingCompleteDialog.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val recordingStartError by viewModel.recordingStartError.collectAsState()
    val uploadedSessionIds by viewModel.uploadedSessionIds.collectAsState()

    // 로컬 녹음 관련 상태
    val localRecordings by viewModel.localRecordings.collectAsState()
    val selectedCount by viewModel.selectedCount.collectAsState()
    val selectedTotalSeconds by viewModel.selectedTotalSeconds.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()

    // 세션 연결 안내 텍스트 (일정 화면에서 진입한 경우)
    val sessionInfoText = remember(sessionId) {
        if (sessionId != null) "세션 #$sessionId 과 연결됩니다" else null
    }

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
            Log.d(TAG, "모든 권한 승인됨, 녹음 시작 (sessionId=$sessionId)")
            viewModel.startRecording(context, studyId, sessionId)
        } else {
            Log.w(TAG, "권한 거부됨: $permissions")
            Toast.makeText(context, "녹음에는 마이크 권한이 필요합니다", Toast.LENGTH_SHORT).show()
        }
    }

    // 권한 확인 후 녹음 시작
    fun checkPermissionsAndStartRecording() {
        // 남은 시간 체크
        if (remainingSeconds <= 0) {
            Toast.makeText(context, "오늘 녹음 가능 시간을 모두 사용했습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val allGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            Log.d(TAG, "권한 이미 승인됨, 녹음 시작 (sessionId=$sessionId)")
            viewModel.startRecording(context, studyId, sessionId)
        } else {
            Log.d(TAG, "권한 요청 필요: ${requiredPermissions.toList()}")
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Repository 초기화 및 데이터 로드
    LaunchedEffect(studyId) {
        viewModel.initRepository(context)
        viewModel.loadMeetings(studyId, refresh = true)
        viewModel.loadSessions(studyId)  // 업로드된 세션 ID 먼저 로드
        viewModel.loadLocalRecordings(studyId)  // 세션 로드 후 녹음 목록 로드
        viewModel.loadRemainingTime(studyId)
    }

    // 녹음 시작 에러 처리
    LaunchedEffect(recordingStartError) {
        recordingStartError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearRecordingStartError()
        }
    }

    // 업로드된 세션 ID가 변경되면 녹음 목록 새로고침 (필터링 적용)
    LaunchedEffect(uploadedSessionIds) {
        if (uploadedSessionIds.isNotEmpty()) {
            viewModel.loadLocalRecordings(studyId)
        }
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

    // 녹음 완료 다이얼로그
    if (showRecordingCompleteDialog) {
        RecordingCompleteDialog(
            onDismiss = { viewModel.dismissRecordingCompleteDialog() }
        )
    }

    // 세션 선택 Bottom Sheet (업로드용)
    if (showSessionSelectDialog) {
        SessionSelectForUploadBottomSheet(
            sessions = sessions,
            selectedCount = selectedCount,
            selectedTotalSeconds = selectedTotalSeconds,
            onSessionSelected = { selectedSessionId ->
                viewModel.uploadSelectedRecordings(studyId, selectedSessionId)
                viewModel.dismissSessionSelectDialog()
            },
            onDismiss = { viewModel.dismissSessionSelectDialog() }
        )
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 녹음 중일 때는 전체 화면 녹음 UI가 대신 표시되므로 상단 배너 숨김

            // 녹음 중이 아닐 때만 세션 연결 안내 및 업로드 중 표시
            if (!(recordingState.isRecording && recordingState.studyId == studyId)) {
                // 세션 연결 안내 (일정 화면에서 진입한 경우)
                if (sessionInfoText != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = sessionInfoText,
                                fontSize = 13.sp,
                                color = Primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
            }

            // 녹음 중일 때 전체 화면 녹음 UI 표시
            if (recordingState.isRecording && recordingState.studyId == studyId) {
                FullScreenRecordingView(
                    elapsedSeconds = recordingState.elapsedSeconds,
                    onStopClick = { viewModel.stopRecordingAndSave(context) }
                )
            } else when (val state = meetingsState) {
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
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 녹음 시작 섹션 (스터디장만 표시)
                        if (isLeader) {
                            item {
                                RecordingStartSection(
                                    remainingSeconds = remainingSeconds,
                                    onStartRecording = { checkPermissionsAndStartRecording() }
                                )
                            }

                            // 미업로드 녹음 목록 섹션 (스터디장만 표시)
                            if (localRecordings.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LocalRecordingsSection(
                                        recordings = localRecordings,
                                        selectedCount = selectedCount,
                                        selectedTotalSeconds = selectedTotalSeconds,
                                        onToggleSelection = { viewModel.toggleRecordingSelection(it, studyId) },
                                        onDeleteRecording = { viewModel.deleteRecording(it) },
                                        onUploadClick = { viewModel.showSessionSelectForUpload() }
                                    )
                                }
                            }
                        }

                        // 회의록 섹션 헤더
                        if (state.meetings.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "AI 회의록",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // 회의록 목록
                        items(state.meetings) { meeting ->
                            PremiumMeetingCard(
                                meeting = meeting,
                                onClick = { onMeetingClick(meeting.id) }
                            )
                        }

                        // 더 불러오기
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

                        // 빈 상태일 때 안내 (스터디장은 녹음도 없을 때, 스터디원은 회의록만 없을 때)
                        val showEmptyHint = if (isLeader) {
                            state.meetings.isEmpty() && localRecordings.isEmpty()
                        } else {
                            state.meetings.isEmpty()
                        }
                        if (showEmptyHint) {
                            item {
                                Spacer(modifier = Modifier.height(32.dp))
                                EmptyStateHint()
                            }
                        }
                    }
                }
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

/**
 * 전체 화면 녹음 UI (9.png 참고)
 */
@Composable
private fun FullScreenRecordingView(
    elapsedSeconds: Long,
    onStopClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // REC 표시
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                PulsingDot(color = Error, size = 12.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Error
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 파형 애니메이션 (간단한 바 형태)
            WaveformAnimation()

            Spacer(modifier = Modifier.height(48.dp))

            // 타이머
            Text(
                text = formatDurationLarge(elapsedSeconds),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 정지 버튼
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = Error.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onStopClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = Error,
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 백그라운드 실행 중 텍스트
            Text(
                text = "백그라운드 실행 중",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 파형 애니메이션 컴포넌트
 */
@Composable
private fun WaveformAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(60.dp)
    ) {
        repeat(9) { index ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 20f,
                targetValue = 60f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + (index * 50),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(animatedHeight.dp)
                    .background(
                        color = Primary,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

private fun formatDurationLarge(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

/**
 * 세션 선택 Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSelectBottomSheet(
    sessions: List<com.ssafy.squiz.data.remote.model.StudySessionDTO>,
    onSessionSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 헤더
            Text(
                text = "녹음을 연결할 세션 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "녹음 파일이 선택한 세션의 미팅 리포트로 생성됩니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 세션 목록
            if (sessions.isEmpty()) {
                // 세션이 없는 경우
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "등록된 세션이 없습니다",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { session ->
                        SessionSelectItem(
                            session = session,
                            onClick = { onSessionSelected(session.id) }
                        )
                    }
                }
            }

        }
    }
}

@Composable
private fun SessionSelectItem(
    session: com.ssafy.squiz.data.remote.model.StudySessionDTO,
    onClick: () -> Unit
) {
    val dateTimeText = remember(session.scheduledAt) {
        try {
            val dateTime = java.time.LocalDateTime.parse(session.scheduledAt?.take(19))
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            dateTime.format(formatter)
        } catch (e: Exception) {
            session.scheduledAt?.replace("T", " ")?.take(16) ?: "시간 미정"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 세션 번호 뱃지
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(GradientPrimaryStart, GradientPrimaryEnd)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${session.sessionNumber ?: "-"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: "${session.sessionNumber}회차 세션",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = dateTimeText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 빈 상태 가이드 UI - 회의록이 없을 때 표시
 */
@Composable
private fun EmptyStateGuide(
    onStartRecording: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 마이크 아이콘 (클릭 가능) + 그라데이션 배경
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .clickable { onStartRecording() }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GradientPrimaryStart.copy(alpha = 0.15f),
                            GradientPrimaryEnd.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .shadow(8.dp, CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(GradientPrimaryStart, GradientPrimaryEnd)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "녹음 시작",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "오늘의 스터디를 기록해 보세요",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "마이크를 눌러 녹음을 시작하면\nAI가 자동으로 회의록을 생성해드려요",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 기능 소개 (간단하게)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureChip(icon = Icons.Default.RecordVoiceOver, label = "음성 녹음")
            FeatureChip(icon = Icons.Default.AutoAwesome, label = "AI 요약")
            FeatureChip(icon = Icons.Default.Checklist, label = "액션 아이템")
        }
    }
}

/**
 * 기능 칩 - 간단한 아이콘 + 라벨
 */
@Composable
private fun FeatureChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GradientPrimaryStart,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 녹음 시작 섹션
 */
@Composable
private fun RecordingStartSection(
    remainingSeconds: Int,
    onStartRecording: () -> Unit
) {
    val remainingText = remember(remainingSeconds) {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
    }

    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 마이크 아이콘 (클릭 가능)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable(enabled = remainingSeconds > 0) { onStartRecording() }
                    .background(
                        brush = if (remainingSeconds > 0) {
                            Brush.linearGradient(listOf(GradientPrimaryStart, GradientPrimaryEnd))
                        } else {
                            Brush.linearGradient(listOf(Color.Gray, Color.Gray))
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "녹음 시작",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (remainingSeconds > 0) "탭하여 녹음 시작" else "오늘 녹음 시간 초과",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (remainingSeconds > 0) MaterialTheme.colorScheme.onSurface else Error
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "오늘 남은 시간: $remainingText",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 로컬 녹음 목록 섹션
 */
@Composable
private fun LocalRecordingsSection(
    recordings: List<com.ssafy.squiz.data.local.recording.LocalRecording>,
    selectedCount: Int,
    selectedTotalSeconds: Int,
    onToggleSelection: (String) -> Unit,
    onDeleteRecording: (com.ssafy.squiz.data.local.recording.LocalRecording) -> Unit,
    onUploadClick: () -> Unit
) {
    val selectedTotalText = remember(selectedTotalSeconds) {
        val minutes = selectedTotalSeconds / 60
        val seconds = selectedTotalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    Column {
        // 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "미업로드 녹음 (${recordings.size}개)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedCount > 0) {
                    Text(
                        text = "${selectedCount}개 선택됨 · 총 $selectedTotalText",
                        fontSize = 13.sp,
                        color = Primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 업로드 버튼 (선택된 항목이 있을 때만)
            if (selectedCount > 0) {
                GradientButton(
                    text = "세션에 업로드",
                    onClick = onUploadClick,
                    icon = Icons.Default.CloudUpload,
                    modifier = Modifier
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 녹음 목록
        recordings.forEach { recording ->
            LocalRecordingItem(
                recording = recording,
                onToggleSelection = { onToggleSelection(recording.id) },
                onDelete = { onDeleteRecording(recording) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 안내 문구
        Text(
            text = "7일 후 자동 삭제됩니다",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * 로컬 녹음 항목
 */
@Composable
private fun LocalRecordingItem(
    recording: com.ssafy.squiz.data.local.recording.LocalRecording,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit
) {
    val durationText = remember(recording.durationSeconds) {
        val minutes = recording.durationSeconds / 60
        val seconds = recording.durationSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val dateText = remember(recording.createdAt) {
        val date = java.util.Date(recording.createdAt)
        java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(date)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() },
        shape = RoundedCornerShape(12.dp),
        color = if (recording.selected) {
            Primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        border = if (recording.selected) {
            androidx.compose.foundation.BorderStroke(1.dp, Primary)
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 선택 순서 번호 또는 빈 원
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (recording.selected) Primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .border(
                        width = if (recording.selected) 0.dp else 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (recording.selected && recording.selectedOrder != null) {
                    Text(
                        text = "${recording.selectedOrder}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 오디오 아이콘
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (recording.selected) Primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = if (recording.selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 녹음 정보
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = durationText,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateText,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 삭제 버튼
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "삭제",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 빈 상태 힌트
 */
@Composable
private fun EmptyStateHint() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "아직 회의록이 없어요",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "녹음을 시작하고 세션에 업로드하면\nAI가 자동으로 회의록을 생성해요",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 녹음 완료 다이얼로그
 */
@Composable
private fun RecordingCompleteDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Success,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "녹음 저장 완료",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "녹음이 로컬에 저장되었습니다.\n녹음 목록에서 세션에 업로드할 수 있습니다.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("확인", color = Primary)
            }
        }
    )
}

/**
 * 세션 선택 Bottom Sheet (업로드용)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionSelectForUploadBottomSheet(
    sessions: List<com.ssafy.squiz.data.remote.model.StudySessionDTO>,
    selectedCount: Int,
    selectedTotalSeconds: Int,
    onSessionSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val selectedTotalText = remember(selectedTotalSeconds) {
        val minutes = selectedTotalSeconds / 60
        val seconds = selectedTotalSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 헤더
            Text(
                text = "업로드할 세션 선택",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${selectedCount}개 녹음 (총 $selectedTotalText)을 선택한 세션에 업로드합니다.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 세션 목록
            if (sessions.isEmpty()) {
                // 세션이 없는 경우
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "등록된 오프라인 세션이 없습니다",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "웹에서 세션을 먼저 생성해주세요",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions) { session ->
                        SessionSelectItem(
                            session = session,
                            onClick = { onSessionSelected(session.id) }
                        )
                    }
                }
            }
        }
    }
}
