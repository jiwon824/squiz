package com.ssafy.squiz.ui.screens.study

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkedStudiesScreen(
    onBackClick: () -> Unit,
    onStudyClick: (Long) -> Unit
) {
    val bookmarkedStudies = remember {
        listOf(
            BookmarkedStudy(1, "알고리즘 스터디", "코딩테스트 대비", listOf("알고리즘"), 5, 8, true),
            BookmarkedStudy(2, "Spring Boot", "백엔드 심화", listOf("Spring", "백엔드"), 4, 6, true),
            BookmarkedStudy(3, "React 프로젝트", "프론트엔드 실전", listOf("React"), 5, 5, false)
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "찜한 스터디",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        if (bookmarkedStudies.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.BookmarkBorder,
                title = "찜한 스터디가 없습니다",
                description = "관심 있는 스터디를 찜해보세요!",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookmarkedStudies) { study ->
                    BookmarkedStudyCard(
                        study = study,
                        onClick = { onStudyClick(study.id) },
                        onRemoveBookmark = { /* Remove bookmark */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarkedStudyCard(
    study: BookmarkedStudy,
    onClick: () -> Unit,
    onRemoveBookmark: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = study.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = if (study.isRecruiting) "모집중" else "마감",
                        color = if (study.isRecruiting) Success else Error
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = study.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${study.currentMembers}/${study.maxMembers}명",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemoveBookmark) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "찜 해제",
                    tint = Warning
                )
            }
        }
    }
}

private data class BookmarkedStudy(
    val id: Long,
    val title: String,
    val description: String,
    val tags: List<String>,
    val currentMembers: Int,
    val maxMembers: Int,
    val isRecruiting: Boolean
)
