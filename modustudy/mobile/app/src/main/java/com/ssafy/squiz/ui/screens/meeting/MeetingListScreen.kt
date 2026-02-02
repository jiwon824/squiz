package com.ssafy.squiz.ui.screens.meeting

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.MeetingDTO
import com.ssafy.squiz.ui.components.EmptyState
import com.ssafy.squiz.ui.theme.*

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
            TopAppBar(
                title = {
                    Text("회의록", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // 녹음 중이면 녹음 중지 FAB, 아니면 녹음 시작 FAB
            if (recordingState.isRecording && recordingState.studyId == studyId) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.stopRecordingAndUpload(context) },
                    containerColor = Error,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("녹음 중지 (${formatDuration(recordingState.elapsedSeconds)})")
                }
            } else {
                FloatingActionButton(
                    onClick = { viewModel.startRecording(context, studyId) },
                    containerColor = Primary
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "녹음 시작", tint = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 녹음 중 상태바
            if (recordingState.isRecording && recordingState.studyId == studyId) {
                RecordingStatusBar(
                    elapsedSeconds = recordingState.elapsedSeconds,
                    isPaused = recordingState.isPaused
                )
            }

            // 업로드 중 표시
            if (uploadState is UploadUiState.Uploading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
                )
            }

            when (val state = meetingsState) {
                is MeetingsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                is MeetingsUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = state.message, color = Error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.loadMeetings(studyId, refresh = true) }) {
                                Text("다시 시도")
                            }
                        }
                    }
                }

                is MeetingsUiState.Success -> {
                    if (state.meetings.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.RecordVoiceOver,
                            title = "회의록이 없습니다",
                            description = "세션을 녹음하면 AI가 자동으로\n회의록을 생성해드려요!",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.meetings) { meeting ->
                                MeetingCard(
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
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = Primary,
                                            strokeWidth = 2.dp
                                        )
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
private fun RecordingStatusBar(
    elapsedSeconds: Long,
    isPaused: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isPaused) Warning.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 녹음 중 애니메이션 점
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) Warning else Error)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isPaused) "녹음 일시정지" else "녹음 중",
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPaused) Warning else Error
                )
            }
            Text(
                text = formatDuration(elapsedSeconds),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MeetingCard(
    meeting: MeetingDTO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = when (meeting.status) {
                                    "COMPLETED" -> Primary.copy(alpha = 0.1f)
                                    "PROCESSING" -> Warning.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(12.dp)
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
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = meeting.title ?: "세션 ${meeting.id}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = meeting.startedAt?.take(16)?.replace("T", " ") ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 상태 뱃지
                StatusBadge(meeting.status ?: "PENDING")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 하단 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 녹음 시간
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = meeting.displayDuration,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 참석자 수
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${meeting.participantCount ?: 0}명",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // AI 요약 여부
                if (meeting.hasSummary) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI 요약",
                            fontSize = 13.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color) = when (status) {
        "RECORDING" -> "녹음 중" to Error
        "PROCESSING" -> "처리 중" to Warning
        "COMPLETED" -> "완료" to Success
        "FAILED" -> "실패" to Error
        else -> "대기" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
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
