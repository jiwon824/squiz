package com.ssafy.squiz.ui.screens.friend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// FriendListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(onBackClick: () -> Unit, onFriendClick: (Long) -> Unit, onSearchClick: () -> Unit, onRequestsClick: () -> Unit, onBlockedClick: () -> Unit) {
    val friends = listOf(Friend(1, "김철수", true), Friend(2, "이영희", true), Friend(3, "박지민", false), Friend(4, "최민수", false), Friend(5, "정수진", true))

    Scaffold(
        topBar = {
            SquizTopBar(title = "친구 목록", onBackClick = onBackClick, actions = {
                IconButton(onClick = onSearchClick) { Icon(Icons.Outlined.PersonAdd, contentDescription = "친구 추가") }
                IconButton(onClick = onRequestsClick) { Icon(Icons.Outlined.PersonSearch, contentDescription = "요청") }
            })
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Online friends
            item { Text("온라인 (${friends.count { it.isOnline }})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(friends.filter { it.isOnline }) { friend ->
                FriendCard(friend = friend, onClick = { onFriendClick(friend.id) })
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Offline friends
            item { Text("오프라인 (${friends.count { !it.isOnline }})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(friends.filter { !it.isOnline }) { friend ->
                FriendCard(friend = friend, onClick = { onFriendClick(friend.id) })
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                TextButton(onClick = onBlockedClick, modifier = Modifier.fillMaxWidth()) {
                    Text("차단 목록", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Friend, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ProfileImage(imageUrl = null, size = 48.dp)
                if (friend.isOnline) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(14.dp).background(Success, CircleShape).padding(2.dp).background(MaterialTheme.colorScheme.surface, CircleShape).padding(2.dp).background(Success, CircleShape))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(friend.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class Friend(val id: Long, val name: String, val isOnline: Boolean)

// FriendSearchScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSearchScreen(onBackClick: () -> Unit, onUserClick: (Long) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val results = listOf(Pair(10L, "홍길동"), Pair(11L, "김민지"))

    Scaffold(topBar = { SquizTopBar(title = "친구 찾기", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Search Bar
            Box(modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f), textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface), cursorBrush = SolidColor(Primary), decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) Text("닉네임 또는 이메일로 검색", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        innerTextField()
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (searchQuery.isNotBlank()) {
                Text("검색 결과", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                results.forEach { (id, name) ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(imageUrl = null, size = 48.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Button(onClick = { }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                                Text("친구 추가", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// FriendRequestsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(onBackClick: () -> Unit, onUserClick: (Long) -> Unit) {
    val requests = listOf(Pair(20L, "신규회원1"), Pair(21L, "신규회원2"))

    Scaffold(topBar = { SquizTopBar(title = "친구 요청", onBackClick = onBackClick) }) { padding ->
        if (requests.isEmpty()) {
            EmptyState(icon = Icons.Outlined.PersonSearch, title = "받은 요청이 없습니다", description = "새로운 친구 요청이 오면 여기에 표시됩니다.", modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(requests) { (id, name) ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(imageUrl = null, size = 48.dp, onClick = { onUserClick(id) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("거절", fontSize = 13.sp) }
                                Button(onClick = { }, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = Primary), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text("수락", fontSize = 13.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// UserProfileScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(userId: Long, onBackClick: () -> Unit, onDMClick: (Long) -> Unit) {
    var isFriend by remember { mutableStateOf(false) }

    Scaffold(topBar = { SquizTopBar(title = "프로필", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileImage(imageUrl = null, size = 100.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("홍길동", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("알고리즘 마스터", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            // Stats
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("5", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("스터디", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("98%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("출석률", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("950", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("퀴즈점수", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onDMClick(userId) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("쪽지")
                }
                Button(onClick = { isFriend = !isFriend }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isFriend) MaterialTheme.colorScheme.surfaceVariant else Primary)) {
                    Icon(if (isFriend) Icons.Default.Check else Icons.Outlined.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (isFriend) MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isFriend) "친구" else "친구 추가", color = if (isFriend) MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
                }
            }
        }
    }
}

// BlockedUsersScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(onBackClick: () -> Unit) {
    val blocked = listOf(Pair(100L, "차단유저1"))

    Scaffold(topBar = { SquizTopBar(title = "차단 목록", onBackClick = onBackClick) }) { padding ->
        if (blocked.isEmpty()) {
            EmptyState(icon = Icons.Outlined.Block, title = "차단한 사용자가 없습니다", description = "차단한 사용자가 있으면 여기에 표시됩니다.", modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(blocked) { (id, name) ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ProfileImage(imageUrl = null, size = 48.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { }, shape = RoundedCornerShape(8.dp)) { Text("차단 해제", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }
}
