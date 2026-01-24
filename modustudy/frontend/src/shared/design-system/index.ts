/**
 * 🎨 SQuiz Design System
 * Google-style 디자인 시스템 - 중앙 집중식 관리
 * 
 * Architecture:
 * - tokens/ : 디자인 토큰 (색상, 타이포그래피, 스페이싱 등)
 * - primitives/ : 기본 컴포넌트 (Box, Container 등)  
 * - utils/ : 유틸리티 함수들
 * 
 * Usage:
 * import { tokens, Box, cn } from '@/shared/design-system';
 */

// 🎯 Design Tokens
export * from './tokens';

// 🧱 Primitive Components  
export { Box, Container } from './primitives/Box';

// 🛠️ Utilities
export { cn, classBuilder, conditionalClasses } from '../utils/cn';

/**
 * 🎨 Design System Configuration
 * 전역 설정 및 기본값
 */
export const designSystemConfig = {
  // 🎯 기본 설정
  defaultTheme: 'light',
  
  // 🎨 브랜드 색상 (주요 식별자)
  brand: {
    primary: '#4285F4',    // Google Blue
    logo: '#4285F4',       // 로고 색상
  },
  
  // 📱 반응형 브레이크포인트
  breakpoints: {
    mobile: '640px',
    tablet: '768px', 
    desktop: '1024px',
    wide: '1280px',
  },
  
  // 🎭 애니메이션 설정
  animation: {
    duration: {
      fast: '150ms',
      normal: '200ms',
      slow: '300ms',
    },
    easing: {
      ease: 'cubic-bezier(0.4, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
    }
  },
  
  // 🔄 전환 효과
  transitions: {
    colors: 'colors 200ms ease',
    transform: 'transform 200ms ease',
    shadow: 'box-shadow 200ms ease',
    all: 'all 200ms ease',
  }
} as const;

/**
 * 🎯 Component Props Types
 * 모든 컴포넌트에서 공통으로 사용하는 타입들
 */
export interface BaseComponentProps {
  className?: string;
  children?: React.ReactNode;
  testId?: string;
}

export interface InteractiveProps extends BaseComponentProps {
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void;
}

export interface FormComponentProps extends BaseComponentProps {
  name?: string;
  value?: any;
  onChange?: (value: any) => void;
  error?: string;
  required?: boolean;
  placeholder?: string;
}

/**
 * 🚀 Design System 버전 정보
 */
export const DESIGN_SYSTEM_VERSION = '1.0.0';

/**
 * 🎨 테마 타입 정의
 */
export type Theme = 'light' | 'dark';
export type ComponentVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
export type ComponentSize = 'sm' | 'md' | 'lg' | 'xl';
export type ColorScheme = 'blue' | 'green' | 'red' | 'yellow' | 'gray';