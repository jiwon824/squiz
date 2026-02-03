package com.ssafy.squiz.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.ui.theme.*

// 소셜 로그인 브랜드 색상
private object SocialBrandColors {
    val KakaoBackground = Color(0xFFFEE500)
    val KakaoText = Color(0xFF191919)
    val NaverBackground = Color(0xFF03C75A)
    val NaverText = Color.White
    val GoogleBackground = Color.White
    val GoogleText = Color(0xFF5F6368)
    val GoogleBorder = Color(0xFFDADCE0)
}

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

    // 로고 애니메이션
    val infiniteTransition = rememberInfiniteTransition(label = "logo")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // 페이드인 애니메이션
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data)
        } else {
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo Section with Animation
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800)) + slideInVertically(
                    initialOffsetY = { -40 },
                    animationSpec = tween(800)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer(
                                scaleX = logoScale,
                                scaleY = logoScale
                            )
                            .size(110.dp)
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = Primary.copy(alpha = 0.4f),
                                spotColor = Primary.copy(alpha = 0.4f)
                            )
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GradientStart, GradientEnd)
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Squiz",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "가장 밀도 높은 몰입\n성장을 흡수하는 기쁨",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error Message with Animation
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -20 })
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Social Login Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) + slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(800, delayMillis = 300)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "소셜 계정으로 시작하기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Kakao Login
                    SocialLoginButton(
                        text = "카카오로 시작하기",
                        iconText = "K",
                        backgroundColor = SocialBrandColors.KakaoBackground,
                        textColor = SocialBrandColors.KakaoText,
                        iconBackgroundColor = Color(0xFF3C1E1E).copy(alpha = 0.1f),
                        isLoading = uiState.isLoading && uiState.selectedProvider == "kakao",
                        enabled = !uiState.isLoading,
                        onClick = { viewModel.loginWithKakao(activity) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Naver Login
                    SocialLoginButton(
                        text = "네이버로 시작하기",
                        iconText = "N",
                        backgroundColor = SocialBrandColors.NaverBackground,
                        textColor = SocialBrandColors.NaverText,
                        iconBackgroundColor = Color.White.copy(alpha = 0.2f),
                        isLoading = uiState.isLoading && uiState.selectedProvider == "naver",
                        enabled = !uiState.isLoading,
                        onClick = { viewModel.loginWithNaver(activity) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Login
                    SocialLoginButton(
                        text = "Google로 시작하기",
                        iconText = "G",
                        backgroundColor = SocialBrandColors.GoogleBackground,
                        textColor = SocialBrandColors.GoogleText,
                        iconBackgroundColor = Color(0xFF4285F4).copy(alpha = 0.1f),
                        iconTextColor = Color(0xFF4285F4),
                        borderColor = SocialBrandColors.GoogleBorder,
                        isLoading = uiState.isLoading && uiState.selectedProvider == "google",
                        enabled = !uiState.isLoading,
                        onClick = {
                            val signInIntent = viewModel.getGoogleSignInIntent(activity)
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Terms with clickable text
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "로그인 시 ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "이용약관",
                            fontSize = 12.sp,
                            color = Primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { /* TODO: 이용약관 페이지로 이동 */ }
                        )
                        Text(
                            text = " 및 ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "개인정보처리방침",
                            fontSize = 12.sp,
                            color = Primary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable { /* TODO: 개인정보처리방침 페이지로 이동 */ }
                        )
                        Text(
                            text = "에 동의합니다.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // 전체 화면 로딩 오버레이 (더블 클릭 방지)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = false
                    ) { /* 클릭 이벤트 소비 */ }
            )
        }
    }
}

@Composable
private fun SocialLoginButton(
    text: String,
    iconText: String,
    backgroundColor: Color,
    textColor: Color,
    iconBackgroundColor: Color,
    iconTextColor: Color = textColor,
    borderColor: Color? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .then(
                if (borderColor != null) {
                    Modifier
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                } else {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = backgroundColor.copy(alpha = 0.4f),
                        spotColor = backgroundColor.copy(alpha = 0.4f)
                    )
                }
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = textColor.copy(alpha = 0.3f)),
                enabled = enabled && !isLoading
            ) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                // 아이콘 영역
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(iconBackgroundColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconTextColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) textColor else textColor.copy(alpha = 0.6f)
                )
            }
        }
    }
}
