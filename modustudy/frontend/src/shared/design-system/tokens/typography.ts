/**
 * 🔤 Typography Design Tokens  
 * Google Material 기반 타이포그래피 시스템
 */

export const typography = {
  // 📝 폰트 패밀리
  fontFamily: {
    primary: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
    mono: ['Fira Code', 'Consolas', 'monospace'],
  },
  
  // 📏 폰트 사이즈
  fontSize: {
    xs: ['0.75rem', { lineHeight: '1rem' }],      // 12px
    sm: ['0.875rem', { lineHeight: '1.25rem' }],  // 14px
    base: ['1rem', { lineHeight: '1.5rem' }],     // 16px
    lg: ['1.125rem', { lineHeight: '1.75rem' }],  // 18px
    xl: ['1.25rem', { lineHeight: '1.75rem' }],   // 20px
    '2xl': ['1.5rem', { lineHeight: '2rem' }],    // 24px
    '3xl': ['1.875rem', { lineHeight: '2.25rem' }], // 30px
    '4xl': ['2.25rem', { lineHeight: '2.5rem' }], // 36px
    '5xl': ['3rem', { lineHeight: '1' }],         // 48px
  },
  
  // ⚖️ 폰트 웨이트
  fontWeight: {
    light: '300',
    normal: '400',
    medium: '500',
    semibold: '600',
    bold: '700',
  },
  
  // 📐 라인 하이트
  lineHeight: {
    none: '1',
    tight: '1.25',
    normal: '1.5',
    relaxed: '1.75',
    loose: '2',
  },
} as const;

/**
 * 🎭 의미론적 타이포그래피 클래스
 * 컴포넌트에서 바로 사용할 수 있는 조합
 */
export const textStyles = {
  // 🏷️ 제목 스타일
  heading: {
    h1: 'text-4xl font-bold text-black leading-tight',
    h2: 'text-3xl font-semibold text-black leading-tight', 
    h3: 'text-2xl font-semibold text-black leading-normal',
    h4: 'text-xl font-medium text-black leading-normal',
    h5: 'text-lg font-medium text-black leading-normal',
    h6: 'text-base font-medium text-black leading-normal',
  },
  
  // 📄 본문 스타일
  body: {
    large: 'text-lg font-normal text-gray-700 leading-relaxed',
    default: 'text-base font-normal text-gray-700 leading-normal',
    small: 'text-sm font-normal text-gray-500 leading-normal',
  },
  
  // 🏷️ 라벨 및 캡션
  label: {
    default: 'text-sm font-medium text-gray-700',
    small: 'text-xs font-medium text-gray-500',
    required: 'text-sm font-medium text-gray-700 after:content-["*"] after:text-google-red after:ml-1',
  },
  
  // 🔗 링크 스타일
  link: {
    default: 'text-google-blue hover:text-google-blue-dark underline-offset-2 hover:underline transition-colors',
    subtle: 'text-gray-700 hover:text-google-blue transition-colors',
  },
  
  // ⚠️ 상태별 텍스트
  status: {
    success: 'text-google-green font-medium',
    warning: 'text-google-yellow font-medium',
    error: 'text-google-red font-medium',
    info: 'text-google-blue font-medium',
  },
} as const;

export type FontSize = keyof typeof typography.fontSize;
export type FontWeight = keyof typeof typography.fontWeight;
export type TextStyle = typeof textStyles;