/**
 * 📏 Spacing Design Tokens
 * 일관된 여백 시스템 - 8px 기반 스케일
 */

export const spacing = {
    // 🔢 기본 스케일 (8px 기준)
    0: '0',
    1: '0.25rem',    // 4px
    2: '0.5rem',     // 8px
    3: '0.75rem',    // 12px
    4: '1rem',       // 16px
    5: '1.25rem',    // 20px
    6: '1.5rem',     // 24px
    8: '2rem',       // 32px
    10: '2.5rem',    // 40px
    12: '3rem',      // 48px
    16: '4rem',      // 64px
    20: '5rem',      // 80px
    24: '6rem',      // 96px

    // 📦 의미론적 스페이싱
    xs: '0.5rem',    // 8px
    sm: '1rem',      // 16px
    md: '1.5rem',    // 24px
    lg: '2rem',      // 32px
    xl: '3rem',      // 48px
    '2xl': '4rem',   // 64px
    '3xl': '6rem',   // 96px
} as const;

/**
 * 🧩 Component-specific spacing
 * 컴포넌트별 권장 여백
 */
export const componentSpacing = {
    button: {
        padding: {
            sm: 'px-3 py-1.5',
            md: 'px-6 py-2.5',
            lg: 'px-8 py-3.5',
        },
        gap: {
            sm: 'gap-1.5',
            md: 'gap-2',
            lg: 'gap-2.5',
        }
    },
    card: {
        padding: {
            sm: 'p-4',
            md: 'p-6',
            lg: 'p-8',
        },
        margin: {
            sm: 'm-2',
            md: 'm-4',
            lg: 'm-6',
        }
    },
    section: {
        margin: {
            sm: 'my-6',
            md: 'my-8',
            lg: 'my-12',
        },
        padding: {
            sm: 'py-4',
            md: 'py-6',
            lg: 'py-8',
        }
    }
} as const;

export type SpacingToken = keyof typeof spacing;