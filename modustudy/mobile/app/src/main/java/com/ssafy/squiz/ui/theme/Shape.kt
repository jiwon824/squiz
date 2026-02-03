package com.ssafy.squiz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================
// Premium Shape System
// ============================================================

val Shapes = Shapes(
    // 아주 작은 요소 (칩, 배지)
    extraSmall = RoundedCornerShape(4.dp),

    // 작은 요소 (버튼, 입력 필드)
    small = RoundedCornerShape(8.dp),

    // 중간 요소 (카드, 다이얼로그)
    medium = RoundedCornerShape(16.dp),

    // 큰 요소 (바텀시트, 모달)
    large = RoundedCornerShape(24.dp),

    // 아주 큰 요소 (풀스크린 모달)
    extraLarge = RoundedCornerShape(32.dp)
)

// ============================================================
// Custom Shape Constants
// ============================================================

// 완전한 원형
val CircleShape = RoundedCornerShape(50)

// 상단만 둥근 모서리 (바텀시트용)
val TopRoundedShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// 하단만 둥근 모서리 (앱바용)
val BottomRoundedShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 0.dp,
    bottomStart = 24.dp,
    bottomEnd = 24.dp
)

// 왼쪽만 둥근 모서리
val LeftRoundedShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 0.dp,
    bottomStart = 16.dp,
    bottomEnd = 0.dp
)

// 오른쪽만 둥근 모서리
val RightRoundedShape = RoundedCornerShape(
    topStart = 0.dp,
    topEnd = 16.dp,
    bottomStart = 0.dp,
    bottomEnd = 16.dp
)

// ============================================================
// Component-Specific Shapes
// ============================================================

// 카드 모양
val CardShape = RoundedCornerShape(16.dp)
val CardShapeSmall = RoundedCornerShape(12.dp)
val CardShapeLarge = RoundedCornerShape(20.dp)

// 버튼 모양
val ButtonShape = RoundedCornerShape(12.dp)
val ButtonShapeSmall = RoundedCornerShape(8.dp)
val ButtonShapeLarge = RoundedCornerShape(16.dp)
val ButtonShapePill = RoundedCornerShape(50)

// 입력 필드 모양
val TextFieldShape = RoundedCornerShape(12.dp)
val SearchBarShape = RoundedCornerShape(24.dp)

// FAB 모양
val FabShape = RoundedCornerShape(16.dp)
val FabShapeExtended = RoundedCornerShape(20.dp)

// 바텀 네비게이션
val BottomNavShape = RoundedCornerShape(
    topStart = 24.dp,
    topEnd = 24.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// 바텀시트
val BottomSheetShape = RoundedCornerShape(
    topStart = 28.dp,
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)

// 다이얼로그
val DialogShape = RoundedCornerShape(28.dp)

// 스낵바
val SnackbarShape = RoundedCornerShape(12.dp)

// 칩/태그
val ChipShape = RoundedCornerShape(8.dp)
val ChipShapePill = RoundedCornerShape(50)

// 이미지/아바타
val AvatarShape = RoundedCornerShape(50)
val ImageShape = RoundedCornerShape(12.dp)
val ImageShapeLarge = RoundedCornerShape(16.dp)

// 프로그레스/인디케이터
val ProgressShape = RoundedCornerShape(50)
