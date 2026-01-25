import React from 'react';
import { cn } from '@/shared/utils/cn';

interface FeatureCardLayoutProps {
    onClick?: () => void;
    headerLeft?: React.ReactNode;
    headerRight?: React.ReactNode;
    title: string;
    description: string;
    body?: React.ReactNode;
    footerLeft?: React.ReactNode;
    footerRight?: React.ReactNode;
    statusOverlay?: React.ReactNode;
    isCompleted?: boolean;
    className?: string;
}

export const FeatureCardLayout: React.FC<FeatureCardLayoutProps> = ({
    onClick,
    headerLeft,
    headerRight,
    title,
    description,
    body,
    footerLeft,
    footerRight,
    statusOverlay,
    isCompleted,
    className
}) => {
    return (
        <div
            onClick={onClick}
            className={cn(
                "group relative bg-white border border-border-light rounded-[40px] px-8 py-10 transition-all duration-700 hover:shadow-[0_30px_70px_rgba(0,0,0,0.08)] hover:-translate-y-3 cursor-pointer overflow-hidden flex flex-col h-[480px]",
                isCompleted && "opacity-60 grayscale-[0.6]",
                className
            )}
        >
            {/* Background Decoration */}
            <div className="absolute top-0 right-0 w-32 h-32 bg-primary/5 rounded-full -mr-16 -mt-16 blur-2xl group-hover:bg-primary/10 transition-colors duration-700" />

            {/* Header: Badges & Actions */}
            <div className="flex justify-between items-start mb-8 relative z-10">
                <div className="flex-1 min-w-0">
                    {headerLeft}
                </div>
                <div className="flex-shrink-0 ml-4">
                    {headerRight}
                </div>
            </div>

            {/* Content: Title & Description */}
            <div className="flex-1 flex flex-col min-w-0 relative z-10 mb-8">
                <h3 className="text-[26px] font-black text-text-primary mb-4 leading-tight tracking-tight group-hover:text-primary transition-colors h-[64px] line-clamp-2">
                    {title}
                </h3>
                <p className="text-[14px] text-text-secondary leading-relaxed font-medium opacity-70 h-[63px] line-clamp-3">
                    {description}
                </p>
            </div>

            {/* Optional Body Slot */}
            {body && <div className="relative z-10">{body}</div>}

            {/* Footer Slot */}
            {(footerLeft || footerRight) && (
                <div className="flex items-center justify-between pt-8 border-t border-border-light/40 mt-auto relative z-10">
                    <div className="flex items-center gap-4 min-w-0 flex-1">
                        {footerLeft}
                    </div>
                    {footerRight && (
                        <div className="flex-shrink-0 ml-4">
                            {footerRight}
                        </div>
                    )}
                </div>
            )}

            {/* Status Overlay */}
            {statusOverlay && (
                <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-20">
                    {statusOverlay}
                </div>
            )}
        </div>
    );
};
