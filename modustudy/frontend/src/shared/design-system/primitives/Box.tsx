/**
 * 🧱 Box Primitive Component
 * 모든 레이아웃의 기초가 되는 Box 컴포넌트
 * 
 * Features:
 * - 디자인 토큰 기반 props
 * - Tailwind와 완전 호환
 * - TypeScript 타입 안전성
 */

import React from 'react';
import { cn } from '@/shared/utils/cn';
import { ComponentSize } from '../tokens';

type SpacingValue = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '3xl';
type ColorValue = 'white' | 'gray-50' | 'gray-100' | 'google-blue' | 'google-green' | 'google-red' | 'google-yellow';

interface BoxProps extends React.HTMLAttributes<HTMLDivElement> {
    // 🎨 색상 props
    bg?: ColorValue;
    color?: ColorValue;

    // 📏 스페이싱 props  
    p?: SpacingValue;
    px?: SpacingValue;
    py?: SpacingValue;
    pt?: SpacingValue;
    pb?: SpacingValue;
    pl?: SpacingValue;
    pr?: SpacingValue;

    m?: SpacingValue;
    mx?: SpacingValue;
    my?: SpacingValue;
    mt?: SpacingValue;
    mb?: SpacingValue;
    ml?: SpacingValue;
    mr?: SpacingValue;

    // 🔘 모서리 반경
    rounded?: 'none' | 'sm' | 'md' | 'lg' | 'xl' | 'full';

    // 🎯 그림자
    shadow?: 'none' | 'sm' | 'md' | 'lg' | 'xl';

    // 📐 레이아웃
    display?: 'block' | 'flex' | 'inline' | 'inline-flex' | 'grid' | 'hidden';

    // HTML 요소 타입 변경
    as?: keyof JSX.IntrinsicElements;
}

export const Box: React.FC<BoxProps> = ({
    children,
    className,
    bg,
    color,
    p, px, py, pt, pb, pl, pr,
    m, mx, my, mt, mb, ml, mr,
    rounded = 'none',
    shadow = 'none',
    display = 'block',
    as: Component = 'div',
    ...props
}) => {
    const classes = cn(
        // 기본 디스플레이
        display,

        // 배경색
        bg && `bg-${bg}`,

        // 텍스트 색상
        color && `text-${color}`,

        // 패딩
        p && `p-${p}`,
        px && `px-${px}`,
        py && `py-${py}`,
        pt && `pt-${pt}`,
        pb && `pb-${pb}`,
        pl && `pl-${pl}`,
        pr && `pr-${pr}`,

        // 마진
        m && `m-${m}`,
        mx && `mx-${mx}`,
        my && `my-${my}`,
        mt && `mt-${mt}`,
        mb && `mb-${mb}`,
        ml && `ml-${ml}`,
        mr && `mr-${mr}`,

        // 모서리 반경
        rounded !== 'none' && `rounded-${rounded}`,

        // 그림자
        shadow !== 'none' && `shadow-${shadow}`,

        className
    );

    return (
        <Component className={classes} {...props}>
            {children}
        </Component>
    );
};

/**
 * 📦 Container Primitive
 * 페이지 컨테이너 컴포넌트
 */
interface ContainerProps extends BoxProps {
    size?: 'sm' | 'md' | 'lg' | 'xl' | 'full';
    centered?: boolean;
}

export const Container: React.FC<ContainerProps> = ({
    size = 'xl',
    centered = true,
    className,
    ...props
}) => {
    const containerClasses = cn(
        'w-full',
        {
            'max-w-sm': size === 'sm',      // 384px
            'max-w-md': size === 'md',      // 448px  
            'max-w-4xl': size === 'lg',     // 896px
            'max-w-7xl': size === 'xl',     // 1280px
            'max-w-full': size === 'full',  // 100%
        },
        centered && 'mx-auto',
        className
    );

    return <Box className={containerClasses} {...props} />;
};