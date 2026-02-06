package com.ssafy.squiz.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Premium Brand Colors - Google Blue Theme
// ============================================================

// Primary Brand - Google Blue
val Primary = Color(0xFF4285F4)          // Google Blue (Main)
val PrimaryVariant = Color(0xFF1A73E8)   // Google Blue Dark
val PrimaryLight = Color(0xFF7BAAF7)     // Google Blue Light
val PrimaryDark = Color(0xFF1557B0)      // Google Blue 700
val PrimaryContainer = Color(0xFFE8F0FE) // Google Blue 50 (배경)
val PrimaryContainerDark = Color(0xFF1A4B8E) // Google Blue 900

// Primary Alpha (투명도)
val Primary10 = Color(0x1A4285F4)        // 10% 투명
val Primary20 = Color(0x334285F4)        // 20% 투명
val Primary30 = Color(0x4D4285F4)        // 30% 투명

// Secondary - Teal/Cyan Accent
val Secondary = Color(0xFF34A853)        // Google Green
val SecondaryVariant = Color(0xFF1E8E3E) // Google Green Dark
val SecondaryLight = Color(0xFF81C995)   // Google Green Light
val SecondaryContainer = Color(0xFFE6F4EA) // Google Green 50

// Tertiary - Warm Amber/Orange Accent
val Tertiary = Color(0xFFFBBC04)         // Google Yellow
val TertiaryVariant = Color(0xFFF9AB00)  // Google Yellow Dark
val TertiaryContainer = Color(0xFFFEF7E0) // Google Yellow 50

// ============================================================
// Surface & Background - Subtle Gradients
// ============================================================

// Light Mode
val Background = Color(0xFFFAFAFC)       // 아주 연한 블루 틴트
val BackgroundSecondary = Color(0xFFF1F5F9) // Slate 100
val Surface = Color(0xFFFFFFFF)
val SurfaceVariant = Color(0xFFF8FAFC)   // Slate 50
val SurfaceElevated = Color(0xFFFFFFFF)

// Dark Mode
val BackgroundDark = Color(0xFF0F172A)   // Slate 900
val BackgroundSecondaryDark = Color(0xFF1E293B) // Slate 800
val SurfaceDark = Color(0xFF1E293B)      // Slate 800
val SurfaceVariantDark = Color(0xFF334155) // Slate 700
val SurfaceElevatedDark = Color(0xFF475569) // Slate 600

// ============================================================
// Text Colors - High Contrast & Readable
// ============================================================

val OnPrimary = Color(0xFFFFFFFF)
val OnSecondary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF1E293B)     // Slate 800
val OnBackgroundSecondary = Color(0xFF64748B) // Slate 500
val OnBackgroundTertiary = Color(0xFF94A3B8) // Slate 400
val OnSurface = Color(0xFF1E293B)
val OnSurfaceSecondary = Color(0xFF64748B)
val OnSurfaceDark = Color(0xFFF1F5F9)    // Slate 100
val OnSurfaceDarkSecondary = Color(0xFF94A3B8)

// ============================================================
// Status Colors - Vibrant & Clear
// ============================================================

val Success = Color(0xFF10B981)          // Emerald 500
val SuccessLight = Color(0xFF34D399)     // Emerald 400
val SuccessContainer = Color(0xFFD1FAE5) // Emerald 100
val SuccessDark = Color(0xFF047857)      // Emerald 700

val Warning = Color(0xFFF59E0B)          // Amber 500
val WarningLight = Color(0xFFFBBF24)     // Amber 400
val WarningContainer = Color(0xFFFEF3C7) // Amber 100
val WarningDark = Color(0xFFB45309)      // Amber 700

val Error = Color(0xFFEF4444)            // Red 500
val ErrorLight = Color(0xFFF87171)       // Red 400
val ErrorContainer = Color(0xFFFEE2E2)   // Red 100
val ErrorDark = Color(0xFFB91C1C)        // Red 700

val Info = Color(0xFF3B82F6)             // Blue 500
val InfoLight = Color(0xFF60A5FA)        // Blue 400
val InfoContainer = Color(0xFFDBEAFE)    // Blue 100
val InfoDark = Color(0xFF1D4ED8)         // Blue 700

// ============================================================
// Attendance Status - Distinct & Clear
// ============================================================

val AttendancePresent = Color(0xFF10B981)  // Emerald - 출석
val AttendanceLate = Color(0xFFF59E0B)     // Amber - 지각
val AttendanceAbsent = Color(0xFFEF4444)   // Red - 결석
val AttendanceExcused = Color(0xFF3B82F6)  // Blue - 사유결석
val AttendancePending = Color(0xFF8B5CF6)  // Violet - 대기

// ============================================================
// Study Status Colors (Google Colors)
// ============================================================

val StatusRecruiting = Color(0xFF34A853)   // Google Green - 모집중
val StatusInProgress = Color(0xFF4285F4)   // Google Blue - 진행중
val StatusCompleted = Color(0xFF5F6368)    // Google Gray - 완료
val StatusPending = Color(0xFFFBBC04)      // Google Yellow - 대기중
val StatusCancelled = Color(0xFFEA4335)    // Google Red - 취소됨

// ============================================================
// Premium Gradients - Google Blue Theme
// ============================================================

// 메인 그라데이션 (Google Blue)
val GradientPrimaryStart = Color(0xFF4285F4)   // Google Blue
val GradientPrimaryEnd = Color(0xFF1A73E8)     // Google Blue Dark

// 세컨더리 그라데이션 (Blue → Deep Blue)
val GradientSecondaryStart = Color(0xFF7BAAF7) // Google Blue Light
val GradientSecondaryEnd = Color(0xFF4285F4)   // Google Blue

// 성공 그라데이션 (Google Green)
val GradientSuccessStart = Color(0xFF34A853)   // Google Green
val GradientSuccessEnd = Color(0xFF1E8E3E)     // Google Green Dark

// 경고 그라데이션 (Google Yellow → Orange)
val GradientWarningStart = Color(0xFFFBBC04)   // Google Yellow
val GradientWarningEnd = Color(0xFFF9AB00)     // Google Yellow Dark

// 다크 그라데이션 (Slate)
val GradientDarkStart = Color(0xFF1E293B)      // Slate 800
val GradientDarkEnd = Color(0xFF0F172A)        // Slate 900

// 레거시 호환 (기존 코드와의 호환성)
val GradientStart = GradientPrimaryStart
val GradientMiddle = Color(0xFF5A9CF5)         // 중간 톤
val GradientEnd = GradientPrimaryEnd

// ============================================================
// Glassmorphism & Modern Effects
// ============================================================

val GlassWhite = Color(0x80FFFFFF)         // 50% 투명 흰색
val GlassWhiteLight = Color(0x33FFFFFF)    // 20% 투명 흰색
val GlassBlack = Color(0x40000000)         // 25% 투명 검정
val GlassBlackLight = Color(0x1A000000)    // 10% 투명 검정
val GlassPrimary = Color(0x334285F4)       // 20% Google Blue

// Blur Overlay
val OverlayLight = Color(0xE6FFFFFF)       // 90% 흰색
val OverlayDark = Color(0xE60F172A)        // 90% 다크

// Glow Effects
val GlowPrimary = Color(0x404285F4)        // 25% Google Blue
val GlowSecondary = Color(0x4034A853)      // 25% Google Green
val GlowSuccess = Color(0x4034A853)        // 25% Google Green

// ============================================================
// Activity Grass Colors (GitHub Style - Enhanced)
// ============================================================

val GrassLevel0 = Color(0xFFE2E8F0)        // Slate 200
val GrassLevel1 = Color(0xFFA7F3D0)        // Emerald 200
val GrassLevel2 = Color(0xFF6EE7B7)        // Emerald 300
val GrassLevel3 = Color(0xFF34D399)        // Emerald 400
val GrassLevel4 = Color(0xFF10B981)        // Emerald 500

// ============================================================
// Social Login Colors
// ============================================================

val KakaoYellow = Color(0xFFFEE500)
val KakaoBlack = Color(0xFF191919)
val NaverGreen = Color(0xFF03C75A)
val NaverWhite = Color(0xFFFFFFFF)
val GoogleWhite = Color(0xFFFFFFFF)
val GoogleGray = Color(0xFF757575)

// ============================================================
// Card & Border Colors
// ============================================================

val CardBorder = Color(0xFFE2E8F0)         // Slate 200
val CardBorderDark = Color(0xFF475569)     // Slate 600
val CardShadow = Color(0x1A000000)         // 10% 검정

val Divider = Color(0xFFE2E8F0)            // Slate 200
val DividerDark = Color(0xFF334155)        // Slate 700

// ============================================================
// Shimmer / Loading
// ============================================================

val ShimmerBase = Color(0xFFE2E8F0)        // Slate 200
val ShimmerHighlight = Color(0xFFF1F5F9)   // Slate 100
val ShimmerBaseDark = Color(0xFF334155)    // Slate 700
val ShimmerHighlightDark = Color(0xFF475569) // Slate 600

// ============================================================
// Chart Colors - Data Visualization (Google Colors)
// ============================================================

val ChartPrimary = Color(0xFF4285F4)       // Google Blue
val ChartSecondary = Color(0xFF34A853)     // Google Green
val ChartTertiary = Color(0xFFFBBC04)      // Google Yellow
val ChartQuaternary = Color(0xFFEA4335)    // Google Red
val ChartQuinary = Color(0xFF5F6368)       // Google Gray
val ChartSenary = Color(0xFF1A73E8)        // Google Blue Dark

// ============================================================
// Recording / Audio
// ============================================================

val RecordingActive = Color(0xFFEA4335)    // Google Red - 녹음중
val RecordingPaused = Color(0xFFFBBC04)    // Google Yellow - 일시정지
val RecordingIdle = Color(0xFF5F6368)      // Google Gray - 대기
val AudioWaveform = Color(0xFF4285F4)      // Google Blue - 파형
val AudioWaveformBg = Color(0xFFE8F0FE)    // Google Blue 50

// ============================================================
// Quiz / Review (FSRS)
// ============================================================

val FsrsAgain = Color(0xFFEF4444)          // Red - 다시
val FsrsHard = Color(0xFFF59E0B)           // Amber - 어려움
val FsrsGood = Color(0xFF10B981)           // Emerald - 좋음
val FsrsEasy = Color(0xFF06B6D4)           // Cyan - 쉬움

// ============================================================
// Meeting / Summary
// ============================================================

val MeetingRecording = Color(0xFFEF4444)   // Red - 녹음중
val MeetingProcessing = Color(0xFFF59E0B)  // Amber - 처리중
val MeetingCompleted = Color(0xFF10B981)   // Emerald - 완료
val MeetingFailed = Color(0xFF64748B)      // Slate - 실패
