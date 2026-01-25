package com.ssafy.squiz.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.components.GradientButton
import com.ssafy.squiz.ui.components.SquizTopBar
import com.ssafy.squiz.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalInfoScreen(
    onNavigateToMain: () -> Unit,
    onBackClick: () -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var nicknameError by remember { mutableStateOf<String?>(null) }
    var isNicknameAvailable by remember { mutableStateOf(false) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var isLoading by remember { mutableStateOf(false) }

    val interests = listOf(
        "프로그래밍", "알고리즘", "데이터분석", "인공지능",
        "웹개발", "앱개발", "디자인", "마케팅",
        "어학", "자격증", "취업준비", "기타"
    )

    val isFormValid = nickname.length >= 2 && isNicknameAvailable && selectedInterests.isNotEmpty()

    Scaffold(
        topBar = {
            SquizTopBar(
                title = "추가 정보 입력",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { 0.5f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Nickname Section
            Text(
                text = "닉네임",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    if (it.length <= 12) {
                        nickname = it
                        nicknameError = null
                        isNicknameAvailable = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("2-12자의 닉네임을 입력해주세요")
                },
                trailingIcon = {
                    if (nickname.length >= 2) {
                        TextButton(
                            onClick = {
                                // TODO: Check nickname availability
                                isNicknameAvailable = true
                            }
                        ) {
                            Text(
                                text = "중복확인",
                                color = Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                isError = nicknameError != null,
                supportingText = {
                    when {
                        nicknameError != null -> {
                            Text(nicknameError!!, color = Error)
                        }
                        isNicknameAvailable -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("사용 가능한 닉네임입니다", color = Success)
                            }
                        }
                        else -> {
                            Text("${nickname.length}/12")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Interests Section
            Text(
                text = "관심 분야",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "스터디 추천에 활용됩니다 (1개 이상 선택)",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interest Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                interests.forEach { interest ->
                    val isSelected = selectedInterests.contains(interest)
                    InterestChip(
                        text = interest,
                        isSelected = isSelected,
                        onClick = {
                            selectedInterests = if (isSelected) {
                                selectedInterests - interest
                            } else {
                                selectedInterests + interest
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            GradientButton(
                text = "시작하기",
                onClick = {
                    isLoading = true
                    // TODO: Submit user info
                    onNavigateToMain()
                },
                enabled = isFormValid,
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Skip Button
            TextButton(
                onClick = onNavigateToMain,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "나중에 입력하기",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InterestChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        Primary
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val borderColor = if (isSelected) {
        Primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
        }
    }
}
