package com.ssafy.squiz.ui.screens.dm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.*
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

// DMListScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMListScreen(
    onBackClick: () -> Unit,
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit = {},
    viewModel: DMViewModel = viewModel()
) {
    val conversationsState by viewModel.conversationsState.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    // 액션 결과 처리
    LaunchedEffect(actionResult) {
        actionResult?.let {
            viewModel.consumeActionResult()
        }
    }

    Scaffold(
        topBar = {
            SquizTopBar(title = "쪽지", onBackClick = onBackClick, actions = {
                IconButton(onClick = onNewChatClick) { Icon(Icons.Outlined.Edit, contentDescription = "새 쪽지") }
            })
        }
    ) { padding ->
        when (val state = conversationsState) {
            is ConversationsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is ConversationsUiState.Error -> {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "오류가 발생했습니다",
                    description = state.message,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            is ConversationsUiState.Success -> {
                val conversations = state.conversations

                if (conversations.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Chat,
                        title = "쪽지가 없습니다",
                        description = "친구에게 쪽지를 보내보세요!",
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                        items(conversations, key = { it.conversationId }) { conversation ->
                            DMChatItem(
                                conversation = conversation,
                                onClick = { onChatClick(conversation.conversationId) },
                                onDeleteClick = { viewModel.deleteConversation(conversation.conversationId) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DMChatItem(
    conversation: ConversationResponse,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            ProfileImage(imageUrl = conversation.partner.profileImageUrl, size = 52.dp)
            if (conversation.partner.isOnline) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    conversation.partner.nickname,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    conversation.lastMessageAt ?: "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.lastMessage ?: "",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(22.dp).background(Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${conversation.unreadCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "더보기", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("대화 삭제") },
                    onClick = {
                        showMenu = false
                        onDeleteClick()
                    },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                )
            }
        }
    }
}

// DMChatScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DMChatScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    onProfileClick: (Long) -> Unit,
    viewModel: DMViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val chatState by viewModel.chatState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val sendingMessage by viewModel.sendingMessage.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()
    val listState = rememberLazyListState()

    // 대화 입장
    LaunchedEffect(conversationId) {
        viewModel.enterConversation(conversationId)
    }

    // 대화 나가기
    DisposableEffect(Unit) {
        onDispose {
            viewModel.leaveConversation()
        }
    }

    // 액션 결과 처리
    LaunchedEffect(actionResult) {
        actionResult?.let {
            viewModel.consumeActionResult()
        }
    }

    // 새 메시지가 오면 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            when (val state = chatState) {
                is ChatUiState.Success -> {
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.clickable { onProfileClick(state.partner.id) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileImage(imageUrl = state.partner.profileImageUrl, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(state.partner.nickname, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    if (state.partner.isOnline) {
                                        Text("온라인", fontSize = 12.sp, color = Success)
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                            }
                        }
                    )
                }
                else -> {
                    TopAppBar(
                        title = { Text("쪽지") },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                            }
                        }
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("메시지 입력") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && !sendingMessage) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && !sendingMessage,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank() && !sendingMessage) Primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        if (sendingMessage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "전송",
                                tint = if (messageText.isNotBlank()) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = chatState) {
            is ChatUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            }
            is ChatUiState.Error -> {
                EmptyState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = "오류가 발생했습니다",
                    description = state.message,
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }
            is ChatUiState.Success -> {
                val partner = state.partner
                val currentUserId = 0L // TODO: 현재 로그인한 사용자 ID 가져오기

                if (messages.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Chat,
                        title = "대화를 시작해보세요",
                        description = "${partner.nickname}님에게 첫 메시지를 보내보세요!",
                        modifier = Modifier.fillMaxSize().padding(padding)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                        state = listState,
                        reverseLayout = true, // 최신 메시지가 아래에
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            val isMine = message.senderId != partner.id

                            if (isMine) {
                                MyMessageBubble(
                                    message = message,
                                    onDeleteClick = { viewModel.deleteMessage(message.id) }
                                )
                            } else {
                                PartnerMessageBubble(
                                    message = message,
                                    partner = partner,
                                    onProfileClick = { onProfileClick(partner.id) }
                                )
                            }
                        }

                        // 더 불러오기 트리거
                        item {
                            LaunchedEffect(Unit) {
                                viewModel.loadMoreMessages()
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
private fun MyMessageBubble(
    message: DMMessage,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End) {
            Box {
                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Primary,
                    modifier = Modifier.clickable { showMenu = true }
                ) {
                    Text(
                        message.content,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("삭제") },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (message.isRead) {
                    Text("읽음", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(message.createdAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PartnerMessageBubble(
    message: DMMessage,
    partner: DMPartner,
    onProfileClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        ProfileImage(imageUrl = partner.profileImageUrl, size = 36.dp, onClick = onProfileClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(partner.nickname, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    message.content,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(message.createdAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// NewDMScreen - 새 대화 시작
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDMScreen(
    partnerId: Long,
    onBackClick: () -> Unit,
    onConversationCreated: (String) -> Unit,
    viewModel: DMViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val sendingMessage by viewModel.sendingMessage.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    // 액션 결과 처리
    LaunchedEffect(actionResult) {
        actionResult?.let { result ->
            if (result is DMActionResult.Error) {
                // TODO: 에러 표시
            }
            viewModel.consumeActionResult()
        }
    }

    Scaffold(
        topBar = {
            SquizTopBar(title = "새 쪽지", onBackClick = onBackClick)
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("첫 메시지를 입력하세요") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && !sendingMessage) {
                                viewModel.startConversation(partnerId, messageText) { conversationId ->
                                    onConversationCreated(conversationId)
                                }
                            }
                        },
                        enabled = messageText.isNotBlank() && !sendingMessage,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (messageText.isNotBlank() && !sendingMessage) Primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    ) {
                        if (sendingMessage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "전송",
                                tint = if (messageText.isNotBlank()) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        EmptyState(
            icon = Icons.Outlined.Chat,
            title = "새 대화 시작",
            description = "첫 메시지를 보내서 대화를 시작하세요.",
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}
