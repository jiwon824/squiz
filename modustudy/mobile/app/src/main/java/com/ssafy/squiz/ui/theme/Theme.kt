package com.ssafy.squiz.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// Premium Light Color Scheme
// ============================================================
private val LightColorScheme = lightColorScheme(
    // Primary
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    inversePrimary = PrimaryLight,

    // Secondary
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = SecondaryVariant,

    // Tertiary
    tertiary = Tertiary,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TertiaryVariant,

    // Error
    error = Error,
    onError = Color.White,
    errorContainer = ErrorContainer,
    onErrorContainer = ErrorDark,

    // Background & Surface
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnBackgroundSecondary,

    // Outline
    outline = CardBorder,
    outlineVariant = Divider,

    // Inverse
    inverseSurface = BackgroundDark,
    inverseOnSurface = OnSurfaceDark,

    // Scrim
    scrim = GlassBlack,

    // Surface Tint
    surfaceTint = Primary
)

// ============================================================
// Premium Dark Color Scheme
// ============================================================
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = PrimaryLight,
    onPrimary = PrimaryDark,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = PrimaryLight,
    inversePrimary = Primary,

    // Secondary
    secondary = SecondaryLight,
    onSecondary = Color(0xFF003544),
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = SecondaryLight,

    // Tertiary
    tertiary = Tertiary,
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = TertiaryVariant,
    onTertiaryContainer = TertiaryContainer,

    // Error
    error = ErrorLight,
    onError = Color(0xFF690005),
    errorContainer = ErrorDark,
    onErrorContainer = ErrorContainer,

    // Background & Surface
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceDarkSecondary,

    // Outline
    outline = CardBorderDark,
    outlineVariant = DividerDark,

    // Inverse
    inverseSurface = Surface,
    inverseOnSurface = OnSurface,

    // Scrim
    scrim = GlassBlack,

    // Surface Tint
    surfaceTint = PrimaryLight
)

// ============================================================
// Theme Composable
// ============================================================
@Composable
fun SquizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color - Android 12+ (비활성화하여 일관된 브랜드 색상 유지)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Edge-to-edge 디스플레이
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // 상태바/네비게이션바 투명
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // 아이콘 색상 자동 조정
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
