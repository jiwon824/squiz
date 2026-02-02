package com.ssafy.squiz.ui.screens.study

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCreateScreen(
    onBackClick: () -> Unit,
    onCreateSuccess: (Long) -> Unit,
    viewModel: StudyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val topics by viewModel.topics.collectAsState()
    val createState by viewModel.createState.collectAsState()

    // 폼 상태
    var name by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTopicId by remember { mutableStateOf<Long?>(null) }
    var selectedSubTopicId by remember { mutableStateOf<Long?>(null) }
    var studyType by remember { mutableStateOf("PLANNED") }
    var meetingType by remember { mutableStateOf("ONLINE") }
    var maxMembers by remember { mutableStateOf("4") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var scheduleSummary by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("BEGINNER") }
    var isPublic by remember { mutableStateOf(true) }
    var goal by remember { mutableStateOf("") }
    var processDetail by remember { mutableStateOf("") }

    // 선택된 대분류의 소분류 목록
    val childTopics = remember(selectedTopicId, topics) {
        topics.find { it.id == selectedTopicId }?.children ?: emptyList()
    }

    // 토픽 로드
    LaunchedEffect(Unit) {
        viewModel.loadTopics()
    }

    // 생성 성공 처리
    LaunchedEffect(createState) {
        if (createState is StudyCreateUiState.Success) {
            val study = (createState as StudyCreateUiState.Success).study
            onCreateSuccess(study.id)
            viewModel.resetCreateState()
        }
    }

    // 날짜 선택기
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(dateFormat.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (studyType == "LIGHTNING") "번개 스터디 만들기" else "스터디 만들기",
                        fontWeight = FontWeight.Bold
                    )
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 스터디 타입 선택
                SectionTitle("스터디 유형")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudyTypeChip(
                        label = "일반 스터디",
                        icon = Icons.Outlined.Groups,
                        selected = studyType == "PLANNED",
                        onClick = { studyType = "PLANNED" },
                        modifier = Modifier.weight(1f)
                    )
                    StudyTypeChip(
                        label = "번개 스터디",
                        icon = Icons.Outlined.FlashOn,
                        selected = studyType == "LIGHTNING",
                        onClick = { studyType = "LIGHTNING" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 스터디 이름
                SectionTitle("스터디 이름 *")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("스터디 이름을 입력하세요") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 한 줄 소개
                SectionTitle("한 줄 소개")
                OutlinedTextField(
                    value = intro,
                    onValueChange = { intro = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("스터디를 한 줄로 소개해주세요") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 토픽 선택
                SectionTitle("주제 선택 *")
                // 대분류
                Text(
                    text = "대분류",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topics) { topic ->
                        FilterChip(
                            selected = selectedTopicId == topic.id,
                            onClick = {
                                selectedTopicId = topic.id
                                selectedSubTopicId = null // 소분류 초기화
                            },
                            label = { Text(topic.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // 소분류 (선택한 대분류가 있을 때만)
                if (childTopics.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "소분류",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(childTopics) { child ->
                            FilterChip(
                                selected = selectedSubTopicId == child.id,
                                onClick = { selectedSubTopicId = child.id },
                                label = { Text(child.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // 진행 방식
                SectionTitle("진행 방식")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MeetingTypeChip(
                        label = "온라인",
                        selected = meetingType == "ONLINE",
                        onClick = { meetingType = "ONLINE" },
                        modifier = Modifier.weight(1f)
                    )
                    MeetingTypeChip(
                        label = "오프라인",
                        selected = meetingType == "OFFLINE",
                        onClick = { meetingType = "OFFLINE" },
                        modifier = Modifier.weight(1f)
                    )
                    MeetingTypeChip(
                        label = "혼합",
                        selected = meetingType == "HYBRID",
                        onClick = { meetingType = "HYBRID" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 난이도
                SectionTitle("난이도")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifficultyChip(
                        label = "입문",
                        selected = difficulty == "BEGINNER",
                        onClick = { difficulty = "BEGINNER" },
                        color = Success,
                        modifier = Modifier.weight(1f)
                    )
                    DifficultyChip(
                        label = "중급",
                        selected = difficulty == "INTERMEDIATE",
                        onClick = { difficulty = "INTERMEDIATE" },
                        color = Warning,
                        modifier = Modifier.weight(1f)
                    )
                    DifficultyChip(
                        label = "고급",
                        selected = difficulty == "ADVANCED",
                        onClick = { difficulty = "ADVANCED" },
                        color = Error,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 최대 인원
                SectionTitle("최대 인원")
                OutlinedTextField(
                    value = maxMembers,
                    onValueChange = { if (it.all { c -> c.isDigit() }) maxMembers = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("4") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { Text("명", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )

                // 일정
                SectionTitle("일정 *")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 시작일
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker { startDate = it } },
                        placeholder = { Text("시작일") },
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    // 종료일
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {},
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDatePicker { endDate = it } },
                        placeholder = { Text("종료일") },
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Primary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // 일정 요약
                OutlinedTextField(
                    value = scheduleSummary,
                    onValueChange = { scheduleSummary = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("예: 매주 화, 목 저녁 8시") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // 공개 설정
                SectionTitle("공개 설정")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPublic = !isPublic }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPublic) Icons.Outlined.Public else Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (isPublic) Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isPublic) "공개 스터디" else "비공개 스터디",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isPublic) "누구나 검색하고 신청할 수 있습니다" else "초대를 통해서만 참여할 수 있습니다",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary)
                    )
                }

                // 스터디 목표 (선택)
                SectionTitle("스터디 목표")
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("스터디를 통해 달성하고자 하는 목표를 작성해주세요") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                // 상세 설명
                SectionTitle("상세 설명")
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("스터디에 대한 상세 설명을 작성해주세요") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 6
                )

                // 진행 방법
                SectionTitle("진행 방법")
                OutlinedTextField(
                    value = processDetail,
                    onValueChange = { processDetail = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("스터디 진행 방법을 설명해주세요") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )

                // 에러 메시지
                if (createState is StudyCreateUiState.Error) {
                    Text(
                        text = (createState as StudyCreateUiState.Error).message,
                        color = Error,
                        fontSize = 14.sp
                    )
                }

                // 생성 버튼
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val topicId = selectedSubTopicId ?: selectedTopicId
                        if (name.isBlank() || topicId == null || startDate.isBlank() || endDate.isBlank()) {
                            return@Button
                        }

                        val request = StudyCreateRequest(
                            name = name,
                            intro = intro.takeIf { it.isNotBlank() },
                            description = description.takeIf { it.isNotBlank() },
                            topicId = topicId,
                            studyType = studyType,
                            meetingType = meetingType,
                            maxMembers = maxMembers.toIntOrNull() ?: 4,
                            isPublic = isPublic,
                            startDate = startDate,
                            endDate = endDate,
                            scheduleSummary = scheduleSummary.takeIf { it.isNotBlank() },
                            difficulty = difficulty,
                            goal = goal.takeIf { it.isNotBlank() },
                            processDetail = processDetail.takeIf { it.isNotBlank() }
                        )
                        viewModel.createStudy(request) { studyId ->
                            onCreateSuccess(studyId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = name.isNotBlank() && (selectedSubTopicId ?: selectedTopicId) != null
                            && startDate.isNotBlank() && endDate.isNotBlank()
                            && createState !is StudyCreateUiState.Loading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (createState is StudyCreateUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "스터디 만들기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun StudyTypeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, Primary, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MeetingTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun DifficultyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White
        )
    )
}
