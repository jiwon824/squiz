package com.ssafy.squiz.ui.screens.ai

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// AIChatbotScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatbotScreen(studyId: Long, onBackClick: () -> Unit) {
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            AIMessage(1, "AI", "안녕하세요! 저는 알고리즘 스터디 AI 도우미입니다. 스터디 자료나 퀴즈에 대해 궁금한 점을 물어보세요!", false),
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(title = "AI 챗봇", onBackClick = onBackClick, actions = {
                IconButton(onClick = { messages.clear(); messages.add(AIMessage(1, "AI", "새로운 대화를 시작합니다!", false)) }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "새 대화")
                }
            })
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("질문을 입력하세요...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                messages.add(AIMessage(messages.size.toLong() + 1, "나", messageText, true))
                                val question = messageText
                                messageText = ""
                                isLoading = true
                                // Simulate AI response
                                messages.add(AIMessage(messages.size.toLong() + 1, "AI", "네, '$question'에 대해 답변드리겠습니다.\n\n스택(Stack)은 LIFO(Last In First Out) 구조로, 마지막에 삽입된 데이터가 가장 먼저 삭제됩니다.\n\n더 궁금한 점이 있으시면 언제든 물어보세요!", false))
                                isLoading = false
                            }
                        },
                        enabled = messageText.isNotBlank() && !isLoading,
                        modifier = Modifier.size(48.dp).background(if (messageText.isNotBlank() && !isLoading) Primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "전송", tint = if (messageText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                if (message.isMine) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = Primary) {
                            Text(message.content, modifier = Modifier.padding(14.dp), fontSize = 14.sp, color = Color.White, lineHeight = 20.sp)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        // AI Avatar
                        Box(modifier = Modifier.size(40.dp).background(brush = Brush.linearGradient(listOf(Secondary, Secondary.copy(alpha = 0.7f))), shape = CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.SmartToy, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f, fill = false)) {
                            Text(message.content, modifier = Modifier.padding(14.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class AIMessage(val id: Long, val sender: String, val content: String, val isMine: Boolean)

// AIRecommendationScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIRecommendationScreen(onBackClick: () -> Unit, onStudyClick: (Long) -> Unit) {
    val recommendations = listOf(
        StudyRecommendation(1, "알고리즘 심화 스터디", "당신의 퀴즈 점수를 분석한 결과, 알고리즘 심화 학습을 추천드립니다.", 95),
        StudyRecommendation(2, "시스템 디자인 스터디", "다음 단계로 시스템 디자인을 학습해보세요.", 88),
        StudyRecommendation(3, "코딩테스트 실전 스터디", "실전 문제 풀이로 실력을 높여보세요.", 82)
    )

    Scaffold(topBar = { SquizTopBar(title = "AI 추천", onBackClick = onBackClick) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Secondary)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("AI 맞춤 추천", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("당신의 학습 패턴과 실력을 분석하여\n최적의 스터디를 추천해드립니다.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f), lineHeight = 20.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            items(recommendations) { study ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onStudyClick(study.id) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(study.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(study.reason, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Match Score
                            Box(modifier = Modifier.size(56.dp).background(brush = Brush.linearGradient(listOf(Secondary.copy(alpha = 0.2f), Primary.copy(alpha = 0.2f))), shape = CircleShape), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${study.matchScore}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Primary)
                                    Text("%", fontSize = 10.sp, color = Primary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("추천도 ${study.matchScore}%", fontSize = 12.sp, color = Success, fontWeight = FontWeight.Medium)
                            }
                            Text("자세히 보기 →", fontSize = 13.sp, color = Primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

private data class StudyRecommendation(val id: Long, val title: String, val reason: String, val matchScore: Int)
