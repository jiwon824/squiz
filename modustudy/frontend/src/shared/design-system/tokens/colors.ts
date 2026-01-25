/**
 * 🎨 Color Design Tokens
 * Google-style 색상 시스템 - 중앙 집중식 관리
 * 
 * Usage:
 * import { colors } from '@/shared/design-system/tokens/colors';
 * className={`bg-${colors.primary.DEFAULT}`}
 */

export const colors = {
    // 🔹 기본 베이스 (흰색/검정색 메인)
    white: '#FFFFFF',
    black: '#202124',

    // 🔹 그레이 스케일 (Google Material 기준)
    gray: {
        50: '#F8F9FA',    // 페이지 배경
        100: '#E8EAED',   // 경계선, 구분선
        300: '#DADCE0',   // 비활성 요소
        500: '#5F6368',   // 보조 텍스트
        700: '#3C4043',   // 서브 타이틀
    },

    // 🌈 Google 포인트 색상 (다채로운 액센트)
    google: {
        blue: {
            DEFAULT: '#4285F4',   // 주요 액션, 링크
            light: '#E8F0FE',     // 연한 배경
            dark: '#1a73e8',      // 호버 상태
        },
        red: {
            DEFAULT: '#EA4335',   // 오류, 경고, 중요
            light: '#FCE8E6',     // 연한 배경
            dark: '#d93025',      // 호버 상태
        },
        yellow: {
            DEFAULT: '#FBBC04',   // 알림, 주의
            light: '#FEF7E0',     // 연한 배경
            dark: '#f9ab00',      // 호버 상태
        },
        green: {
            DEFAULT: '#34A853',   // 성공, 완료
            light: '#E6F4EA',     // 연한 배경
            dark: '#137333',      // 호버 상태
        },
    },

    // 🎯 의미론적 색상 (Google 색상 매핑)
    semantic: {
        primary: '#4285F4',      // google.blue.DEFAULT
        success: '#34A853',      // google.green.DEFAULT
        warning: '#FBBC04',      // google.yellow.DEFAULT
        error: '#EA4335',        // google.red.DEFAULT
        info: '#4285F4',         // google.blue.DEFAULT
    },

    // 📝 텍스트 색상
    text: {
        primary: '#202124',      // 메인 텍스트
        secondary: '#3C4043',    // 서브 타이틀
        tertiary: '#5F6368',     // 보조 텍스트
        muted: '#DADCE0',        // 비활성 텍스트
        inverse: '#FFFFFF',      // 반전 텍스트
    },

    // 🎨 표면 색상
    surface: {
        primary: '#FFFFFF',      // 카드, 모달 배경
        secondary: '#F8F9FA',    // 페이지 배경
        hover: '#F1F3F4',        // 호버 배경
        disabled: '#E8EAED',     // 비활성 배경
    },

    // 🔒 퀴즈 전용 색상 (점수별)
    quiz: {
        perfect: '#34A853',      // 90-100점 (녹색)
        excellent: '#4285F4',    // 75-89점 (파란색)
        good: '#a855f7',         // 50-74점 (보라색 - 유지)
        fair: '#FBBC04',         // 25-49점 (노란색)
        poor: '#EA4335',         // 0-24점 (빨간색)
    },
} as const;

/**
 * 🎨 Tailwind 클래스명 헬퍼 
 * 타입 안전성을 위한 색상 클래스 생성 유틸리티
 */
export const colorClasses = {
    bg: {
        primary: 'bg-google-blue',
        success: 'bg-google-green',
        warning: 'bg-google-yellow',
        error: 'bg-google-red',
        white: 'bg-white',
        gray: 'bg-gray-50',
    },
    text: {
        primary: 'text-black',
        secondary: 'text-gray-700',
        tertiary: 'text-gray-500',
        inverse: 'text-white',
    },
    border: {
        default: 'border-gray-300',
        primary: 'border-google-blue',
        error: 'border-google-red',
    }
} as const;

export type ColorToken = keyof typeof colors;
export type ColorClass = typeof colorClasses;