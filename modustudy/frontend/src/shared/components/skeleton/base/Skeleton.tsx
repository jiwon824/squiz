import React from 'react';
import { cn } from '@/shared/utils/cn';

interface SkeletonProps {
    className?: string;
    variant?: 'text' | 'circle' | 'rect';
    width?: string | number;
    height?: string | number;
    count?: number;
}

const styles = {
    base: 'skeleton',
    variants: {
        text: 'w-full h-4 rounded',
        circle: 'rounded-full',
        rect: 'rounded-md',
    },
};

export const Skeleton: React.FC<SkeletonProps> = ({
    className,
    variant = 'rect',
    width,
    height,
    count = 1,
}) => {
    const items = Array.from({ length: count });

    return (
        <>
            {items.map((_, index) => (
                <div
                    key={index}
                    className={cn(
                        styles.base,
                        styles.variants[variant],
                        className
                    )}
                    style={{
                        width: width,
                        height: height,
                        marginBottom: count > 1 && index < count - 1 ? '0.5rem' : undefined,
                    }}
                />
            ))}
        </>
    );
};
