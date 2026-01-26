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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.SquizTopBar
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    studyId: Long,
    onBackClick: () -> Unit,
    onChannelClick: (Long) -> Unit
) {
    val channels = remember {
        listOf(
            Channel(1, "일반", "공지사항과 일반 대화", ChannelType.GENERAL, 3, "김철수: 오늘 스터디 시간 변경됐어요", "10분 전"),
            Channel(2, "질문", "질문과 답변", ChannelType.QNA, 1, "이영희: DFS 질문이요!", "30분 전"),
            Channel(3, "코드리뷰", "코드 리뷰 채널", ChannelType.REVIEW, 0, "박지민: 리뷰 감사합니다", "1시간 전"),
            Channel(4, "잡담", "자유로운 대화", ChannelType.FREE, 0, "최민수: ㅋㅋㅋ", "2시간 전")
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "채팅방",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { /* Add channel */ }) {
                        Icon(Icons.Outlined.Add, contentDescription = "채널 추가")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(channels) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = { onChannelClick(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(channel.type.color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = channel.type.icon,
                    contentDescription = null,
                    tint = channel.type.color,
                    modifier = Modifier.size(24.dp)
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
                        text = "# ${channel.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = channel.time,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = channel.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = channel.lastMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (channel.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${channel.unreadCount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private data class Channel(
    val id: Long,
    val name: String,
    val description: String,
    val type: ChannelType,
    val unreadCount: Int,
    val lastMessage: String,
    val time: String
)

private enum class ChannelType(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
) {
    GENERAL(Icons.Filled.Campaign, Primary),
    QNA(Icons.Filled.QuestionAnswer, Secondary),
    REVIEW(Icons.Filled.RateReview, Tertiary),
    FREE(Icons.Filled.Chat, Info)
}
