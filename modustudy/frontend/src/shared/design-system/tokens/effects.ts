/**
 * 🎯 Shadow Design Tokens
 * Google Material 기반 그림자 시스템
 */

export const shadows = {
  // 📦 기본 그림자 (elevation 기반)
  none: 'none',
  sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
  default: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
  md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
  lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
  xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
  
  // 🎨 컬러 그림자 (브랜드 색상)
  colored: {
    blue: '0 4px 14px 0 rgb(66 133 244 / 0.15)',
    green: '0 4px 14px 0 rgb(52 168 83 / 0.15)',
    red: '0 4px 14px 0 rgb(234 67 53 / 0.15)',
    yellow: '0 4px 14px 0 rgb(251 188 4 / 0.15)',
  },
  
  // 🔘 컴포넌트별 그림자
  component: {
    button: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
    buttonHover: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    card: '0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1)',
    cardHover: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
    modal: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
    dropdown: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
  }
} as const;

/**
 * ⭕ Border Radius Design Tokens
 * 일관된 모서리 반경 시스템
 */
export const borderRadius = {
  none: '0',
  sm: '0.125rem',    // 2px
  default: '0.25rem', // 4px
  md: '0.375rem',    // 6px
  lg: '0.5rem',      // 8px
  xl: '0.75rem',     // 12px
  '2xl': '1rem',     // 16px
  '3xl': '1.5rem',   // 24px
  full: '9999px',    // 완전한 원
  
  // 🎯 Google Material 기반
  google: {
    sm: '0.25rem',   // 4px - small components
    md: '0.5rem',    // 8px - default buttons, cards
    lg: '0.75rem',   // 12px - large cards, modals
    pill: '9999px',  // pill 형태 버튼
  }
} as const;

export type ShadowToken = keyof typeof shadows;
export type BorderRadiusToken = keyof typeof borderRadius;