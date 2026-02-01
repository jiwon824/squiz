package com.ssafy.squiz.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ssafy.squiz.data.remote.model.StudyDTO
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStudyClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onBookmarkedClick: () -> Unit,
    onMyApplicationsClick: () -> Unit,
    onTemplatesClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val homeState by viewModel.homeState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Top Bar
        item {
            HomeTopBar(
                hasNotification = unreadCount > 0,
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick
            )
        }

        when (val state = homeState) {
            is HomeState.Loading -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            is HomeState.Error -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("데이터를 불러올 수 없습니다", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.loadHomeData() }) {
                                Text("다시 시도")
                            }
                        }
                    }
                }
            }
            is HomeState.Success -> {
                val data = state.data

                // Welcome Banner
                item {
                    WelcomeBanner(
                        userName = data.user.nickname,
                        todayScheduleCount = data.todayScheduleCount,
                        onScheduleClick = onScheduleClick
                    )
                }

                // Quick Actions
                item {
                    QuickActionsSection(
                        onBookmarkedClick = onBookmarkedClick,
                        onMyApplicationsClick = onMyApplicationsClick,
                        onTemplatesClick = onTemplatesClick
                    )
                }

                // Recommended Studies
                if (data.recommendedStudies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "추천 스터디",
                            actionText = "전체보기",
                            onActionClick = onSearchClick
                        )
                    }

                    item {
                        RecommendedStudiesRow(
                            studies = data.recommendedStudies,
                            onStudyClick = onStudyClick
                        )
                    }
                }

                // Popular Studies
                if (data.popularStudies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "인기 스터디",
                            actionText = "전체보기",
                            onActionClick = onSearchClick
                        )
                    }

                    item {
                        PopularStudiesColumn(
                            studies = data.popularStudies,
                            onStudyClick = onStudyClick
                        )
                    }
                }

                // Recent Studies
                if (data.recentStudies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "최근 본 스터디",
                            actionText = "전체보기",
                            onActionClick = onSearchClick
                        )
                    }

                    item {
                        RecentStudiesRow(
                            studies = data.recentStudies,
                            onStudyClick = onStudyClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    hasNotification: Boolean,
    onSearchClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Squiz",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Actions
        Row {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "검색",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = onNotificationClick) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "알림",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (hasNotification) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .size(8.dp)
                            .background(Error, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeBanner(
    userName: String,
    todayScheduleCount: Int,
    onScheduleClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Primary.copy(alpha = 0.2f),
                spotColor = Primary.copy(alpha = 0.2f)
            )
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "안녕하세요, ${userName}님!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "오늘도 함께 성장해요",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Today's Schedule
                Column {
                    Text(
                        text = "오늘 일정",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${todayScheduleCount}개의 스터디",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // View Schedule Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onScheduleClick() },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "일정 보기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onBookmarkedClick: () -> Unit,
    onMyApplicationsClick: () -> Unit,
    onTemplatesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionItem(
            icon = Icons.Outlined.Bookmark,
            label = "찜한 스터디",
            onClick = onBookmarkedClick
        )
        QuickActionItem(
            icon = Icons.Outlined.Assignment,
            label = "내 지원서",
            onClick = onMyApplicationsClick
        )
        QuickActionItem(
            icon = Icons.Outlined.Description,
            label = "템플릿",
            onClick = onTemplatesClick
        )
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Primary.copy(alpha = 0.1f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Primary,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecommendedStudiesRow(studies: List<StudyDTO>, onStudyClick: (Long) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(studies) { study ->
            RecommendedStudyCard(
                study = study,
                onClick = { onStudyClick(study.id) }
            )
        }
    }
}

@Composable
private fun RecommendedStudyCard(
    study: StudyDTO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = study.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = study.description ?: "",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusBadge(
                    text = if (study.status == "RECRUITING") "모집중" else "진행중",
                    color = if (study.status == "RECRUITING") Success else Info
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tags
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                study.tags?.take(2)?.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            color = Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Members
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${study.currentMembers ?: 0}/${study.maxMembers}명",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PopularStudiesColumn(studies: List<StudyDTO>, onStudyClick: (Long) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        studies.forEach { study ->
            PopularStudyCard(
                study = study,
                onClick = { onStudyClick(study.id) }
            )
        }
    }
}

@Composable
private fun PopularStudyCard(
    study: StudyDTO,
    onClick: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Secondary.copy(alpha = 0.2f),
                                Primary.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = study.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    study.tags?.take(2)?.forEach { tag ->
                        Text(
                            text = "#$tag",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isFull = (study.currentMembers ?: 0) >= (study.maxMembers ?: Int.MAX_VALUE)
                StatusBadge(
                    text = if (isFull) "마감" else "모집중",
                    color = if (isFull) Error else Success
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${study.currentMembers ?: 0}/${study.maxMembers}명",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentStudiesRow(studies: List<StudyDTO>, onStudyClick: (Long) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(studies) { study ->
            RecentStudyCard(
                study = study,
                onClick = { onStudyClick(study.id) }
            )
        }
    }
}

@Composable
private fun RecentStudyCard(
    study: StudyDTO,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = study.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = study.tags?.joinToString(" ") { "#$it" } ?: "",
                fontSize = 12.sp,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
