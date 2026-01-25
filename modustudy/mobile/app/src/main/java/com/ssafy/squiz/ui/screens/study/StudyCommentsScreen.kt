package com.ssafy.squiz.ui.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.SquizTopBar
import com.ssafy.squiz.ui.components.ProfileImage
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCommentsScreen(
    studyId: Long,
    onBackClick: () -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val comments = remember {
        listOf(
            Comment(1, "김영희", null, "이 스터디 정말 좋아요! 같이 공부하니까 동기부여가 되네요 🎉", "10분 전", 5),
            Comment(2, "이철수", null, "혹시 아직 모집 중인가요?", "1시간 전", 2),
            Comment(3, "박지민", null, "스터디장님 답변 감사합니다!", "2시간 전", 0),
            Comment(4, "스터디장", null, "@이철수 네, 아직 2자리 남았습니다!", "30분 전", 3)
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "댓글 ${comments.size}",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            CommentInputBar(
                text = commentText,
                onTextChange = { commentText = it },
                onSend = {
                    // TODO: Send comment
                    commentText = ""
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(comments) { comment ->
                CommentItem(
                    comment = comment,
                    onLikeClick = { /* Like comment */ },
                    onReplyClick = { /* Reply to comment */ }
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    onLikeClick: () -> Unit,
    onReplyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        ProfileImage(
            imageUrl = comment.profileImage,
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.userName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = comment.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLikeClick() }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "좋아요",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    if (comment.likes > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likes}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "답글",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReplyClick() }
                        .padding(4.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
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
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("댓글을 입력하세요") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (text.isNotBlank()) Primary else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "전송",
                    tint = if (text.isNotBlank()) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Comment(
    val id: Long,
    val userName: String,
    val profileImage: String?,
    val content: String,
    val time: String,
    val likes: Int
)
