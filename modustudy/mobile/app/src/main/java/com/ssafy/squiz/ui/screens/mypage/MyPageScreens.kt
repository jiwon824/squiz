package com.ssafy.squiz.ui.screens.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// MyPageScreen
@Composable
fun MyPageScreen(
    onEditProfileClick: () -> Unit,
    onMyActivityClick: () -> Unit,
    onNotificationSettingsClick: () -> Unit,
    onPrivacySettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val profileState by viewModel.profileState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadMyProfile()
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("마이페이지", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Profile Card
        item {
            when (val state = profileState) {
                is ProfileState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is ProfileState.Error -> {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.loadMyProfile() }) { Text("다시 시도") }
                        }
                    }
                }
                is ProfileState.Success -> {
                    val profile = state.profile
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            ProfileImage(imageUrl = profile.profileImage, size = 80.dp, onClick = onEditProfileClick)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(profile.nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(profile.email, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                StatItem("스터디", "${profile.studyCount}개")
                                StatItem("출석률", "${profile.attendanceRate}%")
                                StatItem("퀴즈점수", "${profile.quizScore}점")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Menu Items
        item { MenuSection(title = "활동") }
        item { MenuItem(icon = Icons.Outlined.TrendingUp, title = "내 활동", subtitle = "학습 현황 확인", onClick = onMyActivityClick) }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item { MenuSection(title = "설정") }
        item { MenuItem(icon = Icons.Outlined.Person, title = "프로필 수정", onClick = onEditProfileClick) }
        item { MenuItem(icon = Icons.Outlined.Notifications, title = "알림 설정", onClick = onNotificationSettingsClick) }
        item { MenuItem(icon = Icons.Outlined.Lock, title = "개인정보 설정", onClick = onPrivacySettingsClick) }
        item { MenuItem(icon = Icons.Outlined.Help, title = "도움말", onClick = { }) }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            TextButton(onClick = onLogoutClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("로그아웃", color = Error)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MenuSection(title: String) {
    Text(title, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit, badge: Int = 0) {
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, color = MaterialTheme.colorScheme.surface) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge > 0) {
                Box(modifier = Modifier.size(22.dp).background(Error, CircleShape), contentAlignment = Alignment.Center) {
                    Text("$badge", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// EditProfileScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val profileState by viewModel.profileState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val profile = (profileState as? ProfileState.Success)?.profile

    var nickname by remember(profile) { mutableStateOf(profile?.nickname ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }

    LaunchedEffect(Unit) {
        viewModel.loadMyProfile()
    }

    Scaffold(topBar = { SquizTopBar(title = "프로필 수정", onBackClick = onBackClick) }) { padding ->
        when (profileState) {
            is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ProfileState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text((profileState as ProfileState.Error).message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadMyProfile() }) { Text("다시 시도") }
                    }
                }
            }
            is ProfileState.Success -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        ProfileImage(imageUrl = profile?.profileImage, size = 100.dp)
                        Box(modifier = Modifier.size(32.dp).background(Primary, CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "사진 변경", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("닉네임") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("자기소개") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    GradientButton(
                        text = if (saveState is SaveState.Saving) "저장 중..." else "저장",
                        onClick = {
                            if (saveState !is SaveState.Saving && nickname.isNotBlank()) {
                                viewModel.updateProfile(nickname, bio.ifBlank { null }, onSaveSuccess)
                            }
                        }
                    )
                }
            }
        }
    }
}

// NotificationSettingsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val settingsState by viewModel.notificationSettingsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotificationSettings()
    }

    Scaffold(topBar = { SquizTopBar(title = "알림 설정", onBackClick = onBackClick) }) { padding ->
        when (val state = settingsState) {
            is NotificationSettingsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is NotificationSettingsState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadNotificationSettings() }) { Text("다시 시도") }
                    }
                }
            }
            is NotificationSettingsState.Success -> {
                val settings = state.settings
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    SettingSwitch(
                        title = "푸시 알림",
                        subtitle = "앱 푸시 알림 받기",
                        checked = settings.pushEnabled,
                        onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(pushEnabled = it)) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    SettingSwitch(
                        title = "스터디 알림",
                        subtitle = "일정, 출석 관련 알림",
                        checked = settings.studyAlertEnabled,
                        onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(studyAlertEnabled = it)) }
                    )
                    SettingSwitch(
                        title = "채팅 알림",
                        subtitle = "새 메시지 알림",
                        checked = settings.chatAlertEnabled,
                        onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(chatAlertEnabled = it)) }
                    )
                    SettingSwitch(
                        title = "친구 알림",
                        subtitle = "친구 요청 및 수락 알림",
                        checked = settings.friendAlertEnabled,
                        onCheckedChange = { viewModel.updateNotificationSettings(settings.copy(friendAlertEnabled = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.5f)))
    }
}

// MyActivityScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyActivityScreen(
    onBackClick: () -> Unit,
    onGrassClick: () -> Unit,
    onDateClick: (String) -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val activityStatsState by viewModel.activityStatsState.collectAsState()
    val grassState by viewModel.grassState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadActivityStats()
        val now = java.util.Calendar.getInstance()
        viewModel.loadGrassData(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
    }

    Scaffold(topBar = { SquizTopBar(title = "내 활동", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Stats Overview
            when (val state = activityStatsState) {
                is ActivityStatsState.Loading -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                }
                is ActivityStatsState.Error -> {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.loadActivityStats() }) { Text("다시 시도") }
                        }
                    }
                }
                is ActivityStatsState.Success -> {
                    val stats = state.stats
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats.totalStudyDays}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("총 학습일", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats.currentStreak}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("연속 학습", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${stats.attendanceRate}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("출석률", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Grass Graph Preview
            Card(modifier = Modifier.fillMaxWidth().clickable { onGrassClick() }, shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("활동 잔디", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("전체보기", fontSize = 13.sp, color = Primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Grass preview from state
                    when (val state = grassState) {
                        is GrassState.Loading -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..7).forEach { _ ->
                                    Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                        is GrassState.Error -> {
                            Text(state.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                        is GrassState.Success -> {
                            val recentDays = state.data.days.takeLast(7)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                recentDays.forEach { day ->
                                    val intensity = day.level / 4f
                                    Box(modifier = Modifier.size(24.dp).background(
                                        if (day.level == 0) MaterialTheme.colorScheme.surfaceVariant else GrassLevel4.copy(alpha = 0.25f + intensity * 0.75f),
                                        RoundedCornerShape(4.dp)
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// GrassGraphScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrassGraphScreen(
    onBackClick: () -> Unit,
    onDateClick: (String) -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val grassState by viewModel.grassState.collectAsState()
    var currentYear by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }

    LaunchedEffect(currentYear, currentMonth) {
        viewModel.loadGrassData(currentYear, currentMonth)
    }

    Scaffold(topBar = { SquizTopBar(title = "활동 잔디", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Month selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (currentMonth == 1) {
                        currentMonth = 12
                        currentYear -= 1
                    } else {
                        currentMonth -= 1
                    }
                }) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전") }
                Text("${currentYear}년 ${currentMonth}월", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    if (currentMonth == 12) {
                        currentMonth = 1
                        currentYear += 1
                    } else {
                        currentMonth += 1
                    }
                }) { Icon(Icons.Default.ChevronRight, contentDescription = "다음") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = grassState) {
                is GrassState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is GrassState.Error -> {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { viewModel.loadGrassData(currentYear, currentMonth) }) { Text("다시 시도") }
                        }
                    }
                }
                is GrassState.Success -> {
                    val days = state.data.days
                    // Grass grid
                    LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(days.size) { index ->
                            val day = days[index]
                            val intensity = day.level / 4f
                            Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(
                                color = if (day.level == 0) MaterialTheme.colorScheme.surfaceVariant else GrassLevel4.copy(alpha = 0.25f + intensity * 0.75f)
                            ).clickable {
                                val dateStr = String.format("%04d-%02d-%02d", currentYear, currentMonth, day.day)
                                onDateClick(dateStr)
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("적음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { intensity ->
                    Box(modifier = Modifier.size(16.dp).background(GrassLevel4.copy(alpha = intensity), RoundedCornerShape(2.dp)))
                }
                Text("많음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ActivityDetailScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    date: String,
    onBackClick: () -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val activityDetailState by viewModel.activityDetailState.collectAsState()

    LaunchedEffect(date) {
        viewModel.loadActivityDetail(date)
    }

    Scaffold(topBar = { SquizTopBar(title = date, onBackClick = onBackClick) }) { padding ->
        when (val state = activityDetailState) {
            is ActivityDetailState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ActivityDetailState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadActivityDetail(date) }) { Text("다시 시도") }
                    }
                }
            }
            is ActivityDetailState.Success -> {
                val detail = state.detail
                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("학습 시간", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(detail.studyTime, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("활동 내역", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (detail.activities.isEmpty()) {
                        Text("이 날의 활동 기록이 없습니다.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        detail.activities.forEach { activity ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Success)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(activity.title, fontSize = 15.sp)
                                        activity.description?.let {
                                            Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

// PrivacySettingsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBackClick: () -> Unit,
    viewModel: MyPageViewModel = viewModel()
) {
    val settingsState by viewModel.privacySettingsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPrivacySettings()
    }

    Scaffold(topBar = { SquizTopBar(title = "개인정보 설정", onBackClick = onBackClick) }) { padding ->
        when (val state = settingsState) {
            is PrivacySettingsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PrivacySettingsState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { viewModel.loadPrivacySettings() }) { Text("다시 시도") }
                    }
                }
            }
            is PrivacySettingsState.Success -> {
                val settings = state.settings
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    SettingSwitch(
                        title = "프로필 공개",
                        subtitle = "다른 사용자에게 프로필 공개",
                        checked = settings.profilePublic,
                        onCheckedChange = { viewModel.updatePrivacySettings(settings.copy(profilePublic = it)) }
                    )
                    SettingSwitch(
                        title = "활동 공개",
                        subtitle = "학습 활동 공개",
                        checked = settings.activityPublic,
                        onCheckedChange = { viewModel.updatePrivacySettings(settings.copy(activityPublic = it)) }
                    )
                }
            }
        }
    }
}
