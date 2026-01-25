package com.ssafy.squiz.ui.screens.mystudy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// MaterialDetailScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDetailScreen(materialId: Long, onBackClick: () -> Unit) {
    Scaffold(
        topBar = { SquizTopBar(title = "자료 상세", onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("1주차 - 스택/큐", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("PDF · 2.3MB · 김철수", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            GradientButton(text = "다운로드", onClick = { })
        }
    }
}

// MaterialUploadScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialUploadScreen(studyId: Long, onBackClick: () -> Unit, onUploadSuccess: () -> Unit) {
    var title by remember { mutableStateOf("") }
    Scaffold(
        topBar = { SquizTopBar(title = "자료 업로드", onBackClick = onBackClick) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Surface(modifier = Modifier.fillMaxWidth().height(120.dp).clickable { }, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("파일을 선택하세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            GradientButton(text = "업로드", onClick = onUploadSuccess, enabled = title.isNotBlank())
        }
    }
}

// CurriculumScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumScreen(studyId: Long, onBackClick: () -> Unit) {
    val weeks = listOf("1주차: 스택/큐 기초", "2주차: DFS/BFS", "3주차: 동적 프로그래밍", "4주차: 그리디", "5주차: 이분탐색", "6주차: 그래프", "7주차: 최단경로", "8주차: 종합 테스트")
    Scaffold(topBar = { SquizTopBar(title = "커리큘럼", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(weeks.size) { index ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(if (index < 3) Primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                            if (index < 3) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            else Text("${index + 1}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(weeks[index], fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ProgressStatusScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressStatusScreen(studyId: Long, onBackClick: () -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "진행 현황", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("전체 진행률", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("37.5%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { 0.375f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("3/8주 완료", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// TeamDashboardScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDashboardScreen(studyId: Long, onBackClick: () -> Unit, onMemberClick: (Long) -> Unit) {
    val members = listOf(Triple(1L, "김철수", "스터디장"), Triple(2L, "이영희", "팀원"), Triple(3L, "박지민", "팀원"), Triple(4L, "최민수", "팀원"), Triple(5L, "정수진", "팀원"))
    Scaffold(topBar = { SquizTopBar(title = "팀 현황", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members) { (id, name, role) ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onMemberClick(id) }, shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfileImage(imageUrl = null, size = 48.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(role, fontSize = 13.sp, color = if (role == "스터디장") Primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ApplicationManagementScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationManagementScreen(studyId: Long, onBackClick: () -> Unit) {
    val applications = listOf(Triple(1L, "홍길동", "알고리즘에 관심이 많습니다!"), Triple(2L, "김민지", "열심히 참여하겠습니다."))
    Scaffold(topBar = { SquizTopBar(title = "지원서 관리", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(applications) { (id, name, message) ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(imageUrl = null, size = 48.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("거절") }
                            Button(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("수락") }
                        }
                    }
                }
            }
        }
    }
}

// ExtendRecruitmentScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendRecruitmentScreen(studyId: Long, onBackClick: () -> Unit, onSuccess: () -> Unit) {
    var weeks by remember { mutableStateOf(1) }
    Scaffold(topBar = { SquizTopBar(title = "모집 연장", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("연장 기간 선택", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3).forEach { w ->
                    FilterChip(selected = weeks == w, onClick = { weeks = w }, label = { Text("${w}주") })
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            GradientButton(text = "연장하기", onClick = onSuccess)
        }
    }
}

// TempChannelScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempChannelScreen(studyId: Long, onBackClick: () -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "임시 채널", onBackClick = onBackClick) }) { padding ->
        EmptyState(icon = Icons.Outlined.Forum, title = "임시 채널이 없습니다", description = "지원자와 대화를 시작하면 여기에 표시됩니다.", modifier = Modifier.fillMaxSize().padding(padding))
    }
}

// ConvertToOfficialScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertToOfficialScreen(studyId: Long, onBackClick: () -> Unit, onSuccess: () -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "정식 전환", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("정식 스터디로 전환하시겠습니까?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• 모집이 종료됩니다\n• 진행 모드로 전환됩니다\n• 확정된 팀원만 참여할 수 있습니다", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            GradientButton(text = "정식 전환하기", onClick = onSuccess)
        }
    }
}
