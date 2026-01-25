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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.*
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApplicationsScreen(
    onBackClick: () -> Unit,
    onStudyClick: (Long) -> Unit
) {
    val applications = remember {
        listOf(
            Application(1, 1, "알고리즘 스터디", ApplicationStatus.PENDING, "2024.01.10"),
            Application(2, 2, "Spring Boot 마스터", ApplicationStatus.APPROVED, "2024.01.08"),
            Application(3, 3, "React 프로젝트", ApplicationStatus.REJECTED, "2024.01.05")
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "내 지원서",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        if (applications.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Assignment,
                title = "지원서가 없습니다",
                description = "관심 있는 스터디에 지원해보세요!",
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
                items(applications) { application ->
                    ApplicationCard(
                        application = application,
                        onClick = { onStudyClick(application.studyId) },
                        onCancel = { /* Cancel application */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(
    application: Application,
    onClick: () -> Unit,
    onCancel: () -> Unit
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = application.studyTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                StatusBadge(
                    text = application.status.displayName,
                    color = application.status.color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "지원일: ${application.appliedDate}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (application.status == ApplicationStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = "지원 취소",
                            color = Error,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private data class Application(
    val id: Long,
    val studyId: Long,
    val studyTitle: String,
    val status: ApplicationStatus,
    val appliedDate: String
)

private enum class ApplicationStatus(val displayName: String, val color: Color) {
    PENDING("대기중", Warning),
    APPROVED("승인됨", Success),
    REJECTED("거절됨", Error)
}
