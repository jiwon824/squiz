/**
 * 🎨 Design System Tokens - Central Export
 * 모든 디자인 토큰을 한 곳에서 관리
 * 
 * Usage:
 * import { tokens } from '@/shared/design-system';
 * import { colors, typography } from '@/shared/design-system/tokens';
 */

// Design Tokens
export { colors, colorClasses } from './colors';
export { spacing, componentSpacing } from './spacing';
export { typography, textStyles } from './typography';
export { shadows, borderRadius } from './effects';

// Combined tokens object
export const tokens = {
  colors: {
    white: '#FFFFFF',
    black: '#202124', 
    google: {
      blue: '#4285F4',
      red: '#EA4335', 
      yellow: '#FBBC04',
      green: '#34A853',
    },
    gray: {
      50: '#F8F9FA',
      100: '#E8EAED',
      300: '#DADCE0',
      500: '#5F6368',
      700: '#3C4043',
    }
  },
  spacing: {
    xs: '0.5rem',
    sm: '1rem',
    md: '1.5rem', 
    lg: '2rem',
    xl: '3rem',
  },
  fontSize: {
    sm: '0.875rem',
    base: '1rem',
    lg: '1.125rem',
    xl: '1.25rem',
    '2xl': '1.5rem',
    '3xl': '1.875rem',
  },
  borderRadius: {
    sm: '0.25rem',
    md: '0.5rem', 
    lg: '0.75rem',
    pill: '9999px',
  },
  shadow: {
    sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
    md: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    lg: '0 10px 15px -3px rgb(0 0 0 / 0.1)',
  }
} as const;

/**
 * 🎭 Component Variants Type System
 * 컴포넌트 변형 타입 정의
 */
export type ComponentVariant = 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
export type ComponentSize = 'sm' | 'md' | 'lg' | 'xl';
export type ComponentColor = 'blue' | 'green' | 'red' | 'yellow' | 'gray';

/**
 * 📏 Responsive Breakpoints
 */
export const breakpoints = {
  sm: '640px',
  md: '768px', 
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px',
} as const;