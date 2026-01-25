/**
 * 🛠️ ClassName Utility 
 * Tailwind + clsx 기반 클래스명 조합 유틸리티
 * 
 * Usage:
 * import { cn } from '@/shared/utils/cn';
 * 
 * className={cn(
 *   'base-class',
 *   {
 *     'conditional-class': condition,
 *     'another-class': anotherCondition,
 *   },
 *   additionalClassName
 * )}
 */

import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * 클래스명을 조합하고 Tailwind 충돌을 해결하는 유틸리티
 * 
 * @param inputs - 클래스명 또는 조건부 클래스명 객체
 * @returns 조합된 클래스명 문자열
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * 🎨 디자인 토큰 기반 클래스 빌더
 */
export const classBuilder = {
  /**
   * 버튼 클래스 빌더
   */
  button: (variant: 'primary' | 'secondary' | 'outline' | 'ghost', size: 'sm' | 'md' | 'lg') => {
    const baseClasses = 'inline-flex items-center justify-center font-semibold transition-all duration-200 active:scale-[0.98] disabled:opacity-50';
    
    const variants = {
      primary: 'bg-google-blue hover:bg-google-blue-dark text-white shadow-sm',
      secondary: 'bg-gray-100 hover:bg-gray-200 text-gray-700',
      outline: 'border border-google-blue text-google-blue hover:bg-google-blue-light',
      ghost: 'text-gray-700 hover:bg-gray-100',
    };
    
    const sizes = {
      sm: 'px-3 py-1.5 text-xs rounded-md gap-1.5',
      md: 'px-6 py-2.5 text-sm rounded-lg gap-2',
      lg: 'px-8 py-3.5 text-base rounded-xl gap-2.5',
    };
    
    return cn(baseClasses, variants[variant], sizes[size]);
  },
  
  /**
   * 카드 클래스 빌더
   */
  card: (variant: 'default' | 'outlined' | 'elevated' = 'default') => {
    const baseClasses = 'bg-white rounded-lg';
    
    const variants = {
      default: 'border border-gray-100',
      outlined: 'border-2 border-gray-300',
      elevated: 'shadow-md hover:shadow-lg transition-shadow',
    };
    
    return cn(baseClasses, variants[variant]);
  },
  
  /**
   * 텍스트 클래스 빌더
   */
  text: (variant: 'heading' | 'body' | 'caption', size?: 'sm' | 'md' | 'lg') => {
    const variants = {
      heading: cn(
        'font-semibold text-black',
        {
          'text-lg': size === 'sm',
          'text-xl': size === 'md' || !size,
          'text-2xl': size === 'lg',
        }
      ),
      body: cn(
        'text-gray-700',
        {
          'text-sm': size === 'sm',
          'text-base': size === 'md' || !size,  
          'text-lg': size === 'lg',
        }
      ),
      caption: cn(
        'text-gray-500',
        {
          'text-xs': size === 'sm' || !size,
          'text-sm': size === 'md',
          'text-base': size === 'lg',
        }
      ),
    };
    
    return variants[variant];
  }
};

/**
 * 🎯 조건부 클래스 헬퍼
 */
export const conditionalClasses = {
  /**
   * 상태에 따른 클래스
   */
  state: (isActive: boolean, activeClass: string, inactiveClass: string = '') => 
    cn({ [activeClass]: isActive, [inactiveClass]: !isActive && inactiveClass }),
    
  /**
   * 사이즈별 반응형 클래스
   */
  responsive: (classes: { sm?: string; md?: string; lg?: string; xl?: string }) => 
    cn(
      classes.sm && `sm:${classes.sm}`,
      classes.md && `md:${classes.md}`,
      classes.lg && `lg:${classes.lg}`,
      classes.xl && `xl:${classes.xl}`
    ),
};