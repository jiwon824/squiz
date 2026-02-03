package com.ssafy.squiz.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Premium Brand Colors - Modern & Sophisticated
// ============================================================

// Primary Brand - Deep Blue with Purple Accent
val Primary = Color(0xFF6366F1)          // Indigo 500
val PrimaryVariant = Color(0xFF4F46E5)   // Indigo 600
val PrimaryLight = Color(0xFF818CF8)     // Indigo 400
val PrimaryDark = Color(0xFF3730A3)      // Indigo 800
val PrimaryContainer = Color(0xFFE0E7FF) // Indigo 100
val PrimaryContainerDark = Color(0xFF312E81) // Indigo 900

// Secondary - Vibrant Cyan/Teal
val Secondary = Color(0xFF06B6D4)        // Cyan 500
val SecondaryVariant = Color(0xFF0891B2) // Cyan 600
val SecondaryLight = Color(0xFF22D3EE)   // Cyan 400
val SecondaryContainer = Color(0xFFCFFAFE) // Cyan 100

// Tertiary - Warm Amber Accent
val Tertiary = Color(0xFFF59E0B)         // Amber 500
val TertiaryVariant = Color(0xFFD97706)  // Amber 600
val TertiaryContainer = Color(0xFFFEF3C7) // Amber 100

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
// Study Status Colors
// ============================================================

val StatusRecruiting = Color(0xFF10B981)   // Emerald - 모집중
val StatusInProgress = Color(0xFF6366F1)   // Indigo - 진행중
val StatusCompleted = Color(0xFF64748B)    // Slate - 완료
val StatusPending = Color(0xFFF59E0B)      // Amber - 대기중
val StatusCancelled = Color(0xFFEF4444)    // Red - 취소됨

// ============================================================
// Premium Gradients
// ============================================================

// 메인 그라데이션 (Primary → Secondary)
val GradientPrimaryStart = Color(0xFF6366F1)   // Indigo
val GradientPrimaryEnd = Color(0xFF8B5CF6)     // Violet

// 세컨더리 그라데이션 (Cyan → Blue)
val GradientSecondaryStart = Color(0xFF06B6D4) // Cyan
val GradientSecondaryEnd = Color(0xFF3B82F6)   // Blue

// 성공 그라데이션 (Emerald → Teal)
val GradientSuccessStart = Color(0xFF10B981)   // Emerald
val GradientSuccessEnd = Color(0xFF14B8A6)     // Teal

// 경고 그라데이션 (Amber → Orange)
val GradientWarningStart = Color(0xFFF59E0B)   // Amber
val GradientWarningEnd = Color(0xFFF97316)     // Orange

// 다크 그라데이션 (Slate)
val GradientDarkStart = Color(0xFF1E293B)      // Slate 800
val GradientDarkEnd = Color(0xFF0F172A)        // Slate 900

// 레거시 호환 (기존 코드와의 호환성)
val GradientStart = GradientPrimaryStart
val GradientMiddle = Color(0xFF8B5CF6)
val GradientEnd = GradientPrimaryEnd

// ============================================================
// Glassmorphism & Modern Effects
// ============================================================

val GlassWhite = Color(0x80FFFFFF)         // 50% 투명 흰색
val GlassWhiteLight = Color(0x33FFFFFF)    // 20% 투명 흰색
val GlassBlack = Color(0x40000000)         // 25% 투명 검정
val GlassBlackLight = Color(0x1A000000)    // 10% 투명 검정
val GlassPrimary = Color(0x336366F1)       // 20% Primary

// Blur Overlay
val OverlayLight = Color(0xE6FFFFFF)       // 90% 흰색
val OverlayDark = Color(0xE60F172A)        // 90% 다크

// Glow Effects
val GlowPrimary = Color(0x406366F1)        // 25% Primary
val GlowSecondary = Color(0x4006B6D4)      // 25% Secondary
val GlowSuccess = Color(0x4010B981)        // 25% Success

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
// Chart Colors - Data Visualization
// ============================================================

val ChartPrimary = Color(0xFF6366F1)       // Indigo
val ChartSecondary = Color(0xFF06B6D4)     // Cyan
val ChartTertiary = Color(0xFF8B5CF6)      // Violet
val ChartQuaternary = Color(0xFFF59E0B)    // Amber
val ChartQuinary = Color(0xFF10B981)       // Emerald
val ChartSenary = Color(0xFFEC4899)        // Pink

// ============================================================
// Recording / Audio
// ============================================================

val RecordingActive = Color(0xFFEF4444)    // Red - 녹음중
val RecordingPaused = Color(0xFFF59E0B)    // Amber - 일시정지
val RecordingIdle = Color(0xFF64748B)      // Slate - 대기
val AudioWaveform = Color(0xFF6366F1)      // Primary - 파형
val AudioWaveformBg = Color(0xFFE0E7FF)    // Primary Container

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
