package com.ssafy.squiz.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.NotificationDTO
import com.ssafy.squiz.ui.components.EmptyState
import com.ssafy.squiz.ui.components.SquizTopBar
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val notificationsState by viewModel.notificationsState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "알림",
                onBackClick = onBackClick,
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text(
                            text = "모두 읽음",
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = notificationsState) {
            is NotificationsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is NotificationsState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadNotifications() }) {
                            Text("다시 시도")
                        }
                    }
                }
            }
            is NotificationsState.Success -> {
                val notifications = state.notifications

                if (notifications.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Notifications,
                        title = "알림이 없습니다",
                        description = "새로운 알림이 오면 여기에 표시됩니다.",
                        modifier = Modifier.fillMaxSize().padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        val unreadNotifications = notifications.filter { !it.isRead }
                        val readNotifications = notifications.filter { it.isRead }

                        if (unreadNotifications.isNotEmpty()) {
                            item { SectionLabel("읽지 않음") }
                            items(unreadNotifications) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = { viewModel.markAsRead(notification.id) }
                                )
                            }
                        }

                        if (readNotifications.isNotEmpty()) {
                            item { SectionLabel("읽음") }
                            items(readNotifications) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = { }
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NotificationCard(
    notification: NotificationDTO,
    onClick: () -> Unit
) {
    val backgroundColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        Primary.copy(alpha = 0.05f)
    }

    val notificationType = getNotificationType(notification.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    notificationType.backgroundColor,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notificationType.icon,
                contentDescription = null,
                tint = notificationType.iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    fontSize = 15.sp,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTime(notification.createdAt),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Primary, CircleShape)
            )
        }
    }
}

private data class NotificationTypeInfo(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color
)

private fun getNotificationType(type: String): NotificationTypeInfo {
    return when (type) {
        "STUDY_APPLICATION", "APPLICATION" -> NotificationTypeInfo(
            icon = Icons.Default.Assignment,
            iconColor = Primary,
            backgroundColor = Primary.copy(alpha = 0.1f)
        )
        "SCHEDULE", "MEETING" -> NotificationTypeInfo(
            icon = Icons.Default.Event,
            iconColor = Secondary,
            backgroundColor = Secondary.copy(alpha = 0.1f)
        )
        "CHAT", "MESSAGE" -> NotificationTypeInfo(
            icon = Icons.Default.Chat,
            iconColor = Info,
            backgroundColor = Info.copy(alpha = 0.1f)
        )
        "QUIZ", "CONTEST" -> NotificationTypeInfo(
            icon = Icons.Default.Quiz,
            iconColor = Warning,
            backgroundColor = Warning.copy(alpha = 0.1f)
        )
        "FRIEND" -> NotificationTypeInfo(
            icon = Icons.Default.Person,
            iconColor = Tertiary,
            backgroundColor = Tertiary.copy(alpha = 0.1f)
        )
        else -> NotificationTypeInfo(
            icon = Icons.Default.Notifications,
            iconColor = Primary,
            backgroundColor = Primary.copy(alpha = 0.1f)
        )
    }
}

private fun formatTime(timestamp: String): String {
    // 간단한 시간 포맷팅 (실제로는 날짜 라이브러리 사용 권장)
    return try {
        // 예: "2024-01-15T14:30:00" -> "1월 15일"
        val parts = timestamp.split("T")[0].split("-")
        if (parts.size >= 3) {
            "${parts[1].toInt()}월 ${parts[2].toInt()}일"
        } else {
            timestamp
        }
    } catch (e: Exception) {
        timestamp
    }
}
