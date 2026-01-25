package com.ssafy.squiz.ui.screens.mystudy

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
fun MaterialsScreen(
    studyId: Long,
    onBackClick: () -> Unit,
    onMaterialClick: (Long) -> Unit,
    onUploadClick: () -> Unit
) {
    val materials = remember {
        listOf(
            Material(1, "1주차 - 스택/큐", "PDF", "2.3MB", "김철수", "2024.01.10"),
            Material(2, "2주차 - DFS/BFS 정리", "PDF", "1.8MB", "이영희", "2024.01.17"),
            Material(3, "알고리즘 팁 모음", "DOCX", "520KB", "박지민", "2024.01.15"),
            Material(4, "코드 템플릿", "ZIP", "45KB", "김철수", "2024.01.12")
        )
    }

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "자료실",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onUploadClick,
                containerColor = Primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "업로드", tint = Color.White)
            }
        }
    ) { paddingValues ->
        if (materials.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Folder,
                title = "자료가 없습니다",
                description = "첫 번째 자료를 업로드해보세요!",
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(materials) { material ->
                    MaterialCard(
                        material = material,
                        onClick = { onMaterialClick(material.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(
    material: Material,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (material.type) {
                    "PDF" -> Icons.Filled.PictureAsPdf
                    "ZIP" -> Icons.Filled.FolderZip
                    else -> Icons.Filled.Description
                },
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = material.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "${material.type} · ${material.size} · ${material.uploadedBy}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Download, contentDescription = "다운로드", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class Material(val id: Long, val title: String, val type: String, val size: String, val uploadedBy: String, val date: String)
