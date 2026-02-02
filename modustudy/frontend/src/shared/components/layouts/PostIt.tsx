// 포스트잇 스타일 래퍼 컴포넌트
// 대시보드 하단 카드 (오늘의 목표, 다가오는 일정, 최근 성취 등)에서 사용

import React from 'react';

// 3M 포스트잇 색상 팔레트
const POSTIT_THEMES = {
    yellow: {
        bg: '#FFF9C4',
        shadow: 'rgba(200,180,50,0.25)',
        fold: 'rgba(230,210,80,0.6)',
        tape: 'rgba(200,200,180,0.5)',
        text: '#5D4E00',
        sub: '#7A6B1A',
    },
    green: {
        bg: '#C8F7DC',
        shadow: 'rgba(60,160,100,0.2)',
        fold: 'rgba(100,200,140,0.6)',
        tape: 'rgba(180,200,180,0.5)',
        text: '#1B4332',
        sub: '#2D6A4F',
    },
    pink: {
        bg: '#F3D9FA',
        shadow: 'rgba(160,80,180,0.2)',
        fold: 'rgba(200,140,220,0.6)',
        tape: 'rgba(200,180,200,0.5)',
        text: '#4A1259',
        sub: '#6B2D80',
    },
    blue: {
        bg: '#BBDEFB',
        shadow: 'rgba(60,120,200,0.2)',
        fold: 'rgba(100,160,230,0.6)',
        tape: 'rgba(180,190,200,0.5)',
        text: '#0D3B66',
        sub: '#1565C0',
    },
} as const;

export type PostItColor = keyof typeof POSTIT_THEMES;

export interface PostItProps {
    /** 포스트잇 색상 */
    color?: PostItColor;
    /** 기울기 (deg 단위, 예: 1.2, -1.5) */
    rotate?: number;
    /** 테이프 기울기 (deg 단위) */
    tapeRotate?: number;
    /** 자식 콘텐츠 */
    children: React.ReactNode;
    /** 추가 className */
    className?: string;
}

/**
 * 포스트잇 색상 테마 객체를 반환하는 유틸
 * 자식 컴포넌트에서 텍스트 색상 등에 접근할 때 사용
 */
export const getPostItTheme = (color: PostItColor = 'yellow') => POSTIT_THEMES[color];

export const PostIt: React.FC<PostItProps> = ({
    color = 'yellow',
    rotate = 0,
    tapeRotate = 1,
    children,
    className = '',
}) => {
    const theme = POSTIT_THEMES[color];

    return (
        <div
            className={`relative p-6 pb-8 min-h-[220px] transition-all duration-200 hover:-translate-y-1 ${className}`}
            style={{
                background: theme.bg,
                transform: `rotate(${rotate}deg)`,
                boxShadow: `2px 3px 12px ${theme.shadow}, inset 0 0 40px rgba(255,255,255,0.3)`,
            }}
        >
            {/* 테이프 효과 */}
            <div
                className="absolute -top-2 left-1/2 -translate-x-1/2 w-16 h-5 rounded-sm"
                style={{
                    background: theme.tape,
                    backdropFilter: 'blur(2px)',
                    transform: `rotate(${tapeRotate}deg)`,
                }}
            />

            {/* 접힌 모서리 */}
            <div
                className="absolute bottom-0 right-0 w-6 h-6"
                style={{
                    background: `linear-gradient(135deg, ${theme.bg} 50%, ${theme.fold} 50%)`,
                    boxShadow: '-1px -1px 3px rgba(0,0,0,0.06)',
                }}
            />

            {children}
        </div>
    );
};

export default PostIt;
