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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// FriendListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(
    onBackClick: () -> Unit,
    onFriendClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onRequestsClick: () -> Unit,
    onBlockedClick: () -> Unit,
    viewModel: FriendViewModel = viewModel()
) {
    val friendListState by viewModel.friendListState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    // 액션 결과 처리
    LaunchedEffect(actionResult) {
        actionResult?.let {
            // TODO: Snackbar 표시
            viewModel.consumeActionResult()
        }
    }

    Scaffold(
        topBar = {
            SquizTopBar(title = "친구 목록", onBackClick = onBackClick, actions = {
                IconButton(onClick = onSearchClick) { Icon(Icons.Outlined.PersonAdd, contentDescription = "친구 추가") }
                IconButton(onClick = onRequestsClick) { Icon(Icons.Outlined.PersonSearch, contentDescription = "요청") }
            })
        }
    ) { padding ->
        when (val state = friendListState) {
            is FriendListUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is FriendListUiState.Error -> {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "오류가 발생했습니다",
                    description = state.message,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            is FriendListUiState.Success -> {
                val friends = state.data.friends
                val onlineFriends = friends.filter { it.user.isOnline }
                val offlineFriends = friends.filter { !it.user.isOnline }

                if (friends.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.People,
                        title = "아직 친구가 없습니다",
                        description = "친구를 검색해서 추가해보세요!",
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Online friends
                        if (onlineFriends.isNotEmpty()) {
                            item {
                                Text(
                                    "온라인 (${onlineFriends.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(onlineFriends, key = { it.friendId }) { friend ->
                                FriendCard(
                                    friend = friend,
                                    onClick = { onFriendClick(friend.user.id) },
                                    onDeleteClick = { viewModel.deleteFriend(friend.friendId) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        // Offline friends
                        if (offlineFriends.isNotEmpty()) {
                            item {
                                Text(
                                    "오프라인 (${offlineFriends.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(offlineFriends, key = { it.friendId }) { friend ->
                                FriendCard(
                                    friend = friend,
                                    onClick = { onFriendClick(friend.user.id) },
                                    onDeleteClick = { viewModel.deleteFriend(friend.friendId) }
                                )
                            }
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendCard(
    friend: FriendItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                ProfileImage(imageUrl = friend.user.profileImageUrl, size = 48.dp)
                if (friend.user.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .background(Success, CircleShape)
                            .padding(2.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .padding(2.dp)
                            .background(Success, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(friend.user.nickname, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (friend.user.statusMessage?.isNotBlank() == true) {
                    Text(
                        friend.user.statusMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "더보기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("친구 삭제") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Outlined.PersonRemove, contentDescription = null) }
                    )
                }
            }
        }
    }
}

// FriendSearchScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSearchScreen(
    onBackClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    viewModel: FriendViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchState by viewModel.searchState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    // 검색 debounce
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            kotlinx.coroutines.delay(300)
            viewModel.searchUsers(searchQuery)
        } else {
            viewModel.clearSearch()
        }
    }

    // 액션 결과 처리
    LaunchedEffect(actionResult) {
        actionResult?.let {
            viewModel.consumeActionResult()
        }
    }

    Scaffold(topBar = { SquizTopBar(title = "친구 찾기", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(Primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text("닉네임 또는 이메일로 검색", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = ""; viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = "지우기", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = searchState) {
                is SearchUiState.Idle -> {
                    if (searchQuery.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Search,
                            title = "친구를 검색해보세요",
                            description = "닉네임 또는 이메일로 친구를 찾을 수 있습니다.",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                }
                is SearchUiState.Error -> {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        title = "검색 실패",
                        description = state.message,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is SearchUiState.Success -> {
                    if (state.results.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "검색 결과가 없습니다",
                            description = "다른 키워드로 검색해보세요.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("검색 결과", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.results, key = { it.id }) { user ->
                                SearchResultCard(
                                    user = user,
                                    onClick = { onUserClick(user.id) },
                                    onAddFriendClick = { viewModel.sendFriendRequest(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    user: UserSearchResult,
    onClick: () -> Unit,
    onAddFriendClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileImage(imageUrl = user.profileImageUrl, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.nickname, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (user.statusMessage?.isNotBlank() == true) {
                    Text(user.statusMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            when (user.friendStatus) {
                FriendStatus.NONE -> {
                    Button(
                        onClick = onAddFriendClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("친구 추가", fontSize = 13.sp)
                    }
                }
                FriendStatus.PENDING_SENT -> {
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        enabled = false
                    ) {
                        Text("요청함", fontSize = 13.sp)
                    }
                }
                FriendStatus.PENDING_RECEIVED -> {
                    Button(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("수락 대기", fontSize = 13.sp)
                    }
                }
                FriendStatus.FRIEND -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                        Text("친구", fontSize = 13.sp, color = Success)
                    }
                }
                FriendStatus.BLOCKED, FriendStatus.BLOCKED_BY -> {
                    Text("차단됨", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// FriendRequestsScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    onBackClick: () -> Unit,
    onUserClick: (Long) -> Unit,
    viewModel: FriendViewModel = viewModel()
) {
    val receivedRequests by viewModel.receivedRequests.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadReceivedRequests()
        viewModel.loadSentRequests()
    }

    // 액션 결과 처리
    LaunchedEffect(actionResult) {
        actionResult?.let {
            viewModel.consumeActionResult()
        }
    }

    Scaffold(topBar = { SquizTopBar(title = "친구 요청", onBackClick = onBackClick) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Tab
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("받은 요청 (${receivedRequests.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("보낸 요청 (${sentRequests.size})") }
                )
            }

            when (selectedTab) {
                0 -> {
                    if (receivedRequests.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.PersonSearch,
                            title = "받은 요청이 없습니다",
                            description = "새로운 친구 요청이 오면 여기에 표시됩니다.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(receivedRequests, key = { it.requestId }) { request ->
                                ReceivedRequestCard(
                                    request = request,
                                    onUserClick = { onUserClick(request.fromUser.id) },
                                    onAcceptClick = { viewModel.acceptFriendRequest(request.requestId) },
                                    onRejectClick = { viewModel.rejectFriendRequest(request.requestId) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (sentRequests.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Send,
                            title = "보낸 요청이 없습니다",
                            description = "친구를 검색해서 요청을 보내보세요.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sentRequests, key = { it.requestId }) { request ->
                                SentRequestCard(
                                    request = request,
                                    onUserClick = { onUserClick(request.toUser.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedRequestCard(
    request: ReceivedFriendRequest,
    onUserClick: () -> Unit,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileImage(imageUrl = request.fromUser.profileImageUrl, size = 48.dp, onClick = onUserClick)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.fromUser.nickname, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(request.createdAt, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onRejectClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("거절", fontSize = 13.sp)
                }
                Button(
                    onClick = onAcceptClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("수락", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SentRequestCard(
    request: SentFriendRequest,
    onUserClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onUserClick() }, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileImage(imageUrl = request.toUser.profileImageUrl, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(request.toUser.nickname, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("${request.createdAt}에 요청함", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("대기 중", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// UserProfileScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Long,
    onBackClick: () -> Unit,
    onDMClick: (Long) -> Unit,
    viewModel: FriendViewModel = viewModel()
) {
    // TODO: 사용자 프로필 API 연동 필요
    var isFriend by remember { mutableStateOf(false) }

    Scaffold(topBar = { SquizTopBar(title = "프로필", onBackClick = onBackClick) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileImage(imageUrl = null, size = 100.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("사용자", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("상태 메시지", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(24.dp))

            // Stats
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("스터디", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0%", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("출석률", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("0", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Primary)
                    Text("퀴즈점수", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Actions
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onDMClick(userId) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("쪽지")
                }
                Button(
                    onClick = {
                        if (!isFriend) {
                            viewModel.sendFriendRequest(userId)
                        }
                        isFriend = !isFriend
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFriend) MaterialTheme.colorScheme.surfaceVariant else Primary
                    )
                ) {
                    Icon(
                        if (isFriend) Icons.Default.Check else Icons.Outlined.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isFriend) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isFriend) "친구" else "친구 추가",
                        color = if (isFriend) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Block button
            TextButton(onClick = { viewModel.blockUser(userId) }) {
                Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("차단하기", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// BlockedUsersScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    onBackClick: () -> Unit,
    viewModel: FriendViewModel = viewModel()
) {
    val blockedUsers by viewModel.blockedUsers.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadBlockedUsers()
    }

    Scaffold(topBar = { SquizTopBar(title = "차단 목록", onBackClick = onBackClick) }) { padding ->
        if (blockedUsers.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Block,
                title = "차단한 사용자가 없습니다",
                description = "차단한 사용자가 있으면 여기에 표시됩니다.",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(blockedUsers, key = { it.userId }) { blocked ->
                    BlockedUserCard(
                        blocked = blocked,
                        onUnblockClick = { viewModel.unblockUser(blocked.userId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedUserCard(
    blocked: BlockedUser,
    onUnblockClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileImage(imageUrl = blocked.profileImageUrl, size = 48.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(blocked.nickname, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("${blocked.blockedAt}에 차단함", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onUnblockClick, shape = RoundedCornerShape(8.dp)) {
                Text("차단 해제", fontSize = 13.sp)
            }
        }
    }
}
