package com.ssafy.squiz.ui.screens.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.squiz.data.local.AuthManager
import com.ssafy.squiz.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager.getInstance(context) }
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000),
        label = ""
    )

    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000)

        // 자동로그인 체크
        if (authManager.isAutoLoginAvailable()) {
            // 자동로그인 성공 → 메인으로 이동
            onNavigateToMain()
        } else {
            // 로그인 필요 → 로그인 화면으로 이동
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientStart,
                        GradientEnd
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo Text
            Text(
                text = "Squiz",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "가장 밀도 높은 몰입\n성장을 흡수하는 기쁨",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnPrimary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        // Version info at bottom
        Text(
            text = "v1.0.0",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(alphaAnim.value),
            fontSize = 12.sp,
            color = OnPrimary.copy(alpha = 0.6f)
        )
    }
}
