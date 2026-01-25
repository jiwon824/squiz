package com.ssafy.squiz.ui.screens.course

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

// CourseListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseListScreen(onBackClick: () -> Unit, onCourseClick: (Long) -> Unit, onMyProgressClick: () -> Unit) {
    val courses = listOf(
        Course(1, "알고리즘 기초", "기초부터 차근차근", 10, 3),
        Course(2, "자료구조 마스터", "핵심 자료구조 학습", 8, 0),
        Course(3, "운영체제 이론", "OS 기본 개념", 12, 5)
    )

    Scaffold(
        topBar = {
            SquizTopBar(title = "학습 코스", onBackClick = onBackClick, actions = {
                IconButton(onClick = onMyProgressClick) { Icon(Icons.Outlined.Analytics, contentDescription = "내 진행현황") }
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(courses) { course ->
                CourseCard(course = course, onClick = { onCourseClick(course.id) })
            }
        }
    }
}

@Composable
private fun CourseCard(course: Course, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(52.dp).background(brush = Brush.linearGradient(listOf(Secondary, Secondary.copy(alpha = 0.7f))), shape = RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(course.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(course.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${course.completedSections}/${course.totalSections} 섹션", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.weight(1f))
                Text("${(course.completedSections * 100 / course.totalSections)}%", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Secondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { course.completedSections / course.totalSections.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Secondary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

private data class Course(val id: Long, val title: String, val description: String, val totalSections: Int, val completedSections: Int)

// CourseDetailScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(courseId: Long, onBackClick: () -> Unit, onSectionClick: (Long) -> Unit) {
    val sections = listOf(
        Section(1, "기초 개념", true),
        Section(2, "스택과 큐", true),
        Section(3, "연결 리스트", true),
        Section(4, "트리", false),
        Section(5, "그래프", false)
    )

    Scaffold(topBar = { SquizTopBar(title = "알고리즘 기초", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Secondary)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("알고리즘 기초", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("3/5 섹션 완료 • 60%", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(sections) { section ->
                SectionCard(section = section, index = sections.indexOf(section) + 1, onClick = { onSectionClick(section.id) })
            }
        }
    }
}

@Composable
private fun SectionCard(section: Section, index: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(enabled = section.isUnlocked) { onClick() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (section.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(if (section.isUnlocked) Secondary else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) {
                if (section.isUnlocked) {
                    Text("$index", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(section.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (section.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            if (section.isUnlocked) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class Section(val id: Long, val title: String, val isUnlocked: Boolean)

// SectionSolveScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionSolveScreen(sectionId: Long, onBackClick: () -> Unit, onComplete: (Long) -> Unit) {
    var currentQ by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("문제 ${currentQ + 1}/5") }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.Close, contentDescription = "닫기") } }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            LinearProgressIndicator(progress = { (currentQ + 1) / 5f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Secondary)
            Spacer(modifier = Modifier.height(32.dp))

            Text("Q${currentQ + 1}. 스택의 특징은?", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))

            listOf("FIFO", "LIFO", "FILO", "없음").forEachIndexed { i, opt ->
                val isSel = selected == i
                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = i }.border(2.dp, if (isSel) Secondary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)), shape = RoundedCornerShape(12.dp), color = if (isSel) Secondary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface) {
                    Text(opt, modifier = Modifier.padding(16.dp), fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { if (currentQ < 4) { currentQ++; selected = null } else onComplete(1L) }, modifier = Modifier.fillMaxWidth(), enabled = selected != null, colors = ButtonDefaults.buttonColors(containerColor = Secondary)) {
                Text(if (currentQ < 4) "다음" else "완료", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// SectionResultScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionResultScreen(attemptId: Long, onBackClick: () -> Unit, onNextSection: (Long) -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "결과", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(40.dp))
            Box(modifier = Modifier.size(150.dp).background(brush = Brush.linearGradient(listOf(Secondary, Secondary.copy(alpha = 0.7f))), shape = CircleShape), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("4", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("/ 5", fontSize = 18.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("잘했어요!", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("80% 정답률", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBackClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("목록으로") }
                Button(onClick = { onNextSection(2L) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Secondary)) { Text("다음 섹션") }
            }
        }
    }
}

// MyCourseProgressScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCourseProgressScreen(onBackClick: () -> Unit, onCourseClick: (Long) -> Unit) {
    Scaffold(topBar = { SquizTopBar(title = "내 진행현황", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Secondary)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("3", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("진행중", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("완료", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("45%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("평균 진행률", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("진행 중인 코스", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))

            listOf(Triple(1L, "알고리즘 기초", 60), Triple(2L, "자료구조", 30)).forEach { (id, title, progress) ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onCourseClick(id) }, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("$progress%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Secondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = Secondary)
                    }
                }
            }
        }
    }
}
