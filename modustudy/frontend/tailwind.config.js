/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // 🎯 CSS 변수 기반 색상 시스템 (index.css와 연동)
        primary: {
          DEFAULT: 'var(--color-primary)',      // #80A1BA
          light: 'var(--color-primary-light)',  // #9BB5CA  
          dark: 'var(--color-primary-dark)',    // #6A8AA3
          50: 'var(--color-primary-alpha-10)',
          100: 'var(--color-primary-alpha-20)',
          300: 'var(--color-primary-alpha-30)',
        },
        secondary: {
          DEFAULT: 'var(--color-secondary)',     // #91C4C3
          light: 'var(--color-secondary-light)', // #A8D4D3
          dark: 'var(--color-secondary-dark)',   // #7AB3B2  
          50: 'var(--color-secondary-alpha-10)',
          100: 'var(--color-secondary-alpha-20)',
          300: 'var(--color-secondary-alpha-30)',
        },
        accent: {
          DEFAULT: 'var(--color-accent)',       // #B4DEBD
          light: 'var(--color-accent-light)',  // #C7E8CE
          dark: 'var(--color-accent-dark)',    // #9FD1A9
          50: 'var(--color-accent-alpha-10)',
          100: 'var(--color-accent-alpha-20)',
          300: 'var(--color-accent-alpha-30)',
        },
        background: {
          DEFAULT: 'var(--color-background)',           // #FFF7DD
          secondary: 'var(--color-background-secondary)', // #FFF9E6
          tertiary: 'var(--color-background-tertiary)',   // #FFFBF0
        },
        surface: {
          DEFAULT: 'var(--color-surface)',      // #FFFFFF
          hover: 'var(--color-surface-hover)',  // #FFFEF8
        },
        text: {
          primary: 'var(--color-text-primary)',     // #454A4F
          secondary: 'var(--color-text-secondary)', // #5A6C7D
          tertiary: 'var(--color-text-tertiary)',   // #8A9BA8
          inverse: 'var(--color-text-inverse)',     // #FFFFFF
        },
        // 상태 색상 (의미론적)
        success: 'var(--color-success)',    // #B4DEBD (accent와 동일)
        warning: 'var(--color-warning)',    // #FFD88D
        error: 'var(--color-error)',        // #F4A5A5
        info: 'var(--color-info)',          // #91C4C3 (secondary와 동일)

        // 퀴즈 전용 색상 (점수별)
        quiz: {
          success: 'var(--color-google-green)',   // 90-100점
          info: 'var(--color-google-blue)',       // 75-89점  
          focus: '#a855f7',                       // 50-74점 (보라색 유지)
          warning: 'var(--color-google-yellow)',  // 25-49점
          danger: 'var(--color-google-red)',      // 0-24점
        },

        // 호환성 유지 (단계적 제거 예정)
        'study-bg': 'var(--color-gray-50)',
        'study-blue': 'var(--color-google-blue)',
        'study-teal': 'var(--color-google-green)',
        'study-green': 'var(--color-google-green)',
        'study-text': 'var(--color-text-primary)',
        'study-text-dark': 'var(--color-text-primary)',
      },
      borderRadius: {
        'google': '8px',      // Google Material 기본
        'google-lg': '12px',  // 대형 카드
        'pill': '50px',       // 버튼용 pill 형태
      },
      fontFamily: {
        'sans': ['Pretendard', 'var(--font-primary)', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
