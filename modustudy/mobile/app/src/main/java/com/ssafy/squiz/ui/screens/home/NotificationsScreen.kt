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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.EmptyState
import com.ssafy.squiz.ui.components.SquizTopBar
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBackClick: () -> Unit
) {
    val notifications = remember {
        listOf(
            NotificationItem(
                id = 1,
                type = NotificationType.STUDY_APPLICATION,
                title = "스터디 지원 승인",
                message = "알고리즘 스터디 지원이 승인되었습니다.",
                time = "방금 전",
                isRead = false
            ),
            NotificationItem(
                id = 2,
                type = NotificationType.SCHEDULE,
                title = "일정 알림",
                message = "30분 후 'Spring Boot 마스터' 스터디가 시작됩니다.",
                time = "10분 전",
                isRead = false
            ),
            NotificationItem(
                id = 3,
                type = NotificationType.CHAT,
                title = "새 메시지",
                message = "React 프로젝트 채팅방에 새 메시지가 있습니다.",
                time = "1시간 전",
                isRead = true
            ),
            NotificationItem(
                id = 4,
                type = NotificationType.QUIZ,
                title = "퀴즈 대회 시작",
                message = "참여 신청한 퀴즈 대회가 10분 후 시작됩니다.",
                time = "2시간 전",
                isRead = true
            ),
            NotificationItem(
                id = 5,
                type = NotificationType.FRIEND,
                title = "친구 요청",
                message = "홍길동님이 친구 요청을 보냈습니다.",
                time = "어제",
                isRead = true
            ),
            NotificationItem(
                id = 6,
                type = NotificationType.STUDY_APPLICATION,
                title = "새 지원서",
                message = "SQLD 스터디에 새로운 지원서가 도착했습니다.",
                time = "어제",
                isRead = true
            )
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "알림",
                onBackClick = onBackClick,
                actions = {
                    TextButton(onClick = { /* Mark all as read */ }) {
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
        if (notifications.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Notifications,
                title = "알림이 없습니다",
                description = "새로운 알림이 오면 여기에 표시됩니다.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val unreadNotifications = notifications.filter { !it.isRead }
                val readNotifications = notifications.filter { it.isRead }

                if (unreadNotifications.isNotEmpty()) {
                    item {
                        SectionLabel("읽지 않음")
                    }
                    items(unreadNotifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { /* Handle click */ }
                        )
                    }
                }

                if (readNotifications.isNotEmpty()) {
                    item {
                        SectionLabel("읽음")
                    }
                    items(readNotifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { /* Handle click */ }
                        )
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
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val backgroundColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        Primary.copy(alpha = 0.05f)
    }

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
                    notification.type.backgroundColor,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notification.type.icon,
                contentDescription = null,
                tint = notification.type.iconColor,
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
                    text = notification.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
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

private data class NotificationItem(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val message: String,
    val time: String,
    val isRead: Boolean
)

private enum class NotificationType(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color
) {
    STUDY_APPLICATION(
        icon = Icons.Default.Assignment,
        iconColor = Primary,
        backgroundColor = Primary.copy(alpha = 0.1f)
    ),
    SCHEDULE(
        icon = Icons.Default.Event,
        iconColor = Secondary,
        backgroundColor = Secondary.copy(alpha = 0.1f)
    ),
    CHAT(
        icon = Icons.Default.Chat,
        iconColor = Info,
        backgroundColor = Info.copy(alpha = 0.1f)
    ),
    QUIZ(
        icon = Icons.Default.Quiz,
        iconColor = Warning,
        backgroundColor = Warning.copy(alpha = 0.1f)
    ),
    FRIEND(
        icon = Icons.Default.Person,
        iconColor = Tertiary,
        backgroundColor = Tertiary.copy(alpha = 0.1f)
    )
}
