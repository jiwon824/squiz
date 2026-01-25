package com.ssafy.squiz.ui.screens.dm

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// DMListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMListScreen(onBackClick: () -> Unit, onChatClick: (Long) -> Unit) {
    val chats = listOf(
        DMChat(1, "김철수", "오늘 스터디 어땠어요?", "방금", true, 2),
        DMChat(2, "이영희", "알려주셔서 감사합니다!", "10분 전", false, 0),
        DMChat(3, "박지민", "자료 받았어요 👍", "1시간 전", false, 0)
    )

    Scaffold(
        topBar = {
            SquizTopBar(title = "쪽지", onBackClick = onBackClick, actions = {
                IconButton(onClick = { }) { Icon(Icons.Outlined.Edit, contentDescription = "새 쪽지") }
            })
        }
    ) { padding ->
        if (chats.isEmpty()) {
            EmptyState(icon = Icons.Outlined.Chat, title = "쪽지가 없습니다", description = "친구에게 쪽지를 보내보세요!", modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(chats) { chat ->
                    DMChatItem(chat = chat, onClick = { onChatClick(chat.id) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun DMChatItem(chat: DMChat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            ProfileImage(imageUrl = null, size = 52.dp)
            if (chat.isOnline) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).size(14.dp).background(Success, CircleShape).padding(2.dp).background(MaterialTheme.colorScheme.surface, CircleShape).padding(2.dp).background(Success, CircleShape))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(chat.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(chat.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(chat.lastMessage, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (chat.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(22.dp).background(Primary, CircleShape), contentAlignment = Alignment.Center) {
                        Text("${chat.unreadCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

private data class DMChat(val id: Long, val name: String, val lastMessage: String, val time: String, val isOnline: Boolean, val unreadCount: Int)

// DMChatScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMChatScreen(chatId: Long, onBackClick: () -> Unit, onProfileClick: (Long) -> Unit) {
    var messageText by remember { mutableStateOf("") }
    val messages = remember {
        listOf(
            DMMessage(1, "김철수", "안녕하세요!", "14:00", false),
            DMMessage(2, "나", "안녕하세요! 반갑습니다 :)", "14:02", true),
            DMMessage(3, "김철수", "오늘 스터디 어땠어요?", "14:05", false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(modifier = Modifier.clickable { onProfileClick(1L) }, verticalAlignment = Alignment.CenterVertically) {
                        ProfileImage(imageUrl = null, size = 36.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("김철수", fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "뒤로") } }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = messageText, onValueChange = { messageText = it }, modifier = Modifier.weight(1f), placeholder = { Text("메시지 입력") }, shape = RoundedCornerShape(24.dp), maxLines = 4)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { messageText = "" }, enabled = messageText.isNotBlank(), modifier = Modifier.size(48.dp).background(if (messageText.isNotBlank()) Primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Send, contentDescription = "전송", tint = if (messageText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(messages) { message ->
                if (message.isMine) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = Primary) {
                                Text(message.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontSize = 14.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(message.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        ProfileImage(imageUrl = null, size = 36.dp, onClick = { onProfileClick(1L) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(message.senderName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(message.content, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(message.time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private data class DMMessage(val id: Long, val senderName: String, val content: String, val time: String, val isMine: Boolean)
