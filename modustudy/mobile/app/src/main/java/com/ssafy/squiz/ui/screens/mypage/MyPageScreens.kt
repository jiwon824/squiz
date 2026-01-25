package com.ssafy.squiz.ui.screens.mypage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// MyPageScreen
@Composable
fun MyPageScreen(onEditProfileClick: () -> Unit, onFriendListClick: () -> Unit, onDMListClick: () -> Unit, onMyActivityClick: () -> Unit, onLogoutClick: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("마이페이지", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Profile Card
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileImage(imageUrl = null, size = 80.dp, onClick = onEditProfileClick)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("김모두", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("squiz@gmail.com", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        StatItem("스터디", "3개")
                        StatItem("출석률", "95%")
                        StatItem("퀴즈점수", "850점")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        // Menu Items
        item { MenuSection(title = "활동") }
        item { MenuItem(icon = Icons.Outlined.TrendingUp, title = "내 활동", subtitle = "학습 현황 확인", onClick = onMyActivityClick) }
        item { MenuItem(icon = Icons.Outlined.People, title = "친구 목록", subtitle = "15명의 친구", onClick = onFriendListClick) }
        item { MenuItem(icon = Icons.Outlined.Chat, title = "쪽지", subtitle = "새 쪽지 2개", onClick = onDMListClick, badge = 2) }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item { MenuSection(title = "설정") }
        item { MenuItem(icon = Icons.Outlined.Person, title = "프로필 수정", onClick = onEditProfileClick) }
        item { MenuItem(icon = Icons.Outlined.Notifications, title = "알림 설정", onClick = { }) }
        item { MenuItem(icon = Icons.Outlined.Lock, title = "개인정보 설정", onClick = { }) }
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
fun EditProfileScreen(onBackClick: () -> Unit, onSaveSuccess: () -> Unit) {
    var nickname by remember { mutableStateOf("김모두") }
    var bio by remember { mutableStateOf("") }

    Scaffold(topBar = { SquizTopBar(title = "프로필 수정", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                ProfileImage(imageUrl = null, size = 100.dp)
                Box(modifier = Modifier.size(32.dp).background(Primary, CircleShape).clickable { }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "사진 변경", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("닉네임") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("자기소개") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.weight(1f))
            GradientButton(text = "저장", onClick = onSaveSuccess)
        }
    }
}

// NotificationSettingsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBackClick: () -> Unit) {
    var pushEnabled by remember { mutableStateOf(true) }
    var studyAlert by remember { mutableStateOf(true) }
    var chatAlert by remember { mutableStateOf(true) }
    var friendAlert by remember { mutableStateOf(true) }

    Scaffold(topBar = { SquizTopBar(title = "알림 설정", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SettingSwitch(title = "푸시 알림", subtitle = "앱 푸시 알림 받기", checked = pushEnabled, onCheckedChange = { pushEnabled = it })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingSwitch(title = "스터디 알림", subtitle = "일정, 출석 관련 알림", checked = studyAlert, onCheckedChange = { studyAlert = it })
            SettingSwitch(title = "채팅 알림", subtitle = "새 메시지 알림", checked = chatAlert, onCheckedChange = { chatAlert = it })
            SettingSwitch(title = "친구 알림", subtitle = "친구 요청 및 수락 알림", checked = friendAlert, onCheckedChange = { friendAlert = it })
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
fun MyActivityScreen(onBackClick: () -> Unit, onGrassClick: () -> Unit, onDateClick: (String) -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "내 활동", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Stats Overview
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("156", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("총 학습일", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("12", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("연속 학습", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("95%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("출석률", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
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
                    // Simple grass preview
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..7).forEach { day ->
                            val intensity = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f).random()
                            Box(modifier = Modifier.size(24.dp).background(GrassLevel4.copy(alpha = intensity), RoundedCornerShape(4.dp)))
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
fun GrassGraphScreen(onBackClick: () -> Unit, onDateClick: (String) -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "활동 잔디", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Month selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { }) { Icon(Icons.Default.ChevronLeft, contentDescription = "이전") }
                Text("2024년 1월", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { }) { Icon(Icons.Default.ChevronRight, contentDescription = "다음") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grass grid
            LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(31) { day ->
                    val intensity = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f).random()
                    Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(color = if (intensity == 0f) MaterialTheme.colorScheme.surfaceVariant else GrassLevel4.copy(alpha = intensity)).clickable { onDateClick("2024-01-${day + 1}") })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("적음", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0.2f, 0.4f, 0.6f, 0.8f, 1f).forEach { intensity ->
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
fun ActivityDetailScreen(date: String, onBackClick: () -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = date, onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("학습 시간", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2시간 30분", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("활동 내역", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            listOf("알고리즘 스터디 출석", "퀴즈 3문제 풀이", "데일리 리포트 작성").forEach { activity ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Success)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(activity, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// PrivacySettingsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(onBackClick: () -> Unit) {
    var profilePublic by remember { mutableStateOf(true) }
    var activityPublic by remember { mutableStateOf(false) }

    Scaffold(topBar = { SquizTopBar(title = "개인정보 설정", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SettingSwitch(title = "프로필 공개", subtitle = "다른 사용자에게 프로필 공개", checked = profilePublic, onCheckedChange = { profilePublic = it })
            SettingSwitch(title = "활동 공개", subtitle = "학습 활동 공개", checked = activityPublic, onCheckedChange = { activityPublic = it })
        }
    }
}
