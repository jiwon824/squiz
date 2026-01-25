package com.ssafy.squiz.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToMain: () -> Unit,
    onNavigateToAdditionalInfo: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    val uiState by viewModel.uiState.collectAsState()
    val loginEvent by viewModel.loginEvent.collectAsState()

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data)
        } else {
            // 사용자가 취소했거나 실패
            viewModel.handleGoogleSignInResult(null)
        }
    }

    // 로그인 이벤트 처리
    LaunchedEffect(loginEvent) {
        when (loginEvent) {
            is LoginEvent.NavigateToMain -> {
                viewModel.consumeLoginEvent()
                onNavigateToMain()
            }
            is LoginEvent.NavigateToAdditionalInfo -> {
                viewModel.consumeLoginEvent()
                onNavigateToAdditionalInfo()
            }
            null -> { /* no-op */ }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Logo Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Primary.copy(alpha = 0.3f),
                        spotColor = Primary.copy(alpha = 0.3f)
                    )
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Squiz",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "가장 밀도 높은 몰입\n성장을 흡수하는 기쁨",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Error Message
            if (uiState.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Social Login Buttons
            Text(
                text = "소셜 계정으로 시작하기",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Kakao Login
            SocialLoginButton(
                text = "카카오로 시작하기",
                backgroundColor = Color(0xFFFEE500),
                textColor = Color(0xFF191919),
                isLoading = uiState.isLoading && uiState.selectedProvider == "kakao",
                enabled = !uiState.isLoading,
                onClick = {
                    viewModel.loginWithKakao(activity)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Naver Login
            SocialLoginButton(
                text = "네이버로 시작하기",
                backgroundColor = Color(0xFF03C75A),
                textColor = Color.White,
                isLoading = uiState.isLoading && uiState.selectedProvider == "naver",
                enabled = !uiState.isLoading,
                onClick = {
                    viewModel.loginWithNaver(activity)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Google Login
            SocialLoginButton(
                text = "Google로 시작하기",
                backgroundColor = Color.White,
                textColor = Color(0xFF5F6368),
                borderColor = Color(0xFFDDDDDD),
                isLoading = uiState.isLoading && uiState.selectedProvider == "google",
                enabled = !uiState.isLoading,
                onClick = {
                    val signInIntent = viewModel.getGoogleSignInIntent(activity)
                    googleSignInLauncher.launch(signInIntent)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Terms
            Text(
                text = "로그인 시 이용약관 및 개인정보처리방침에 동의합니다.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .then(
                if (borderColor != null) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = backgroundColor.copy(alpha = 0.3f),
                        spotColor = backgroundColor.copy(alpha = 0.3f)
                    )
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !isLoading) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) textColor else textColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}
