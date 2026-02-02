/**
 * PageSidebar - 페이지 내 좌측 사이드바 공통 컴포넌트
 * 설정, 스터디 관리 등 페이지에서 공통으로 사용
 */

import React from 'react';
import { cn } from '@/shared/utils/cn';

export interface SidebarItem {
    id: string;
    label: string;
    icon: React.ReactNode;
    description?: string;
    badge?: number;
}

export interface PageSidebarProps {
    items: SidebarItem[];
    activeId: string;
    onSelect: (id: string) => void;
    title?: string;
    className?: string;
}

export const PageSidebar: React.FC<PageSidebarProps> = ({
    items,
    activeId,
    onSelect,
    title,
    className,
}) => {
    return (
        <aside className={cn('w-56 flex-shrink-0', className)}>
            <nav className="bg-surface rounded-3xl border border-border-light p-3 shadow-sm sticky top-6">
                {title && (
                    <h2 className="text-lg font-extrabold text-text-primary mb-4 pl-2">
                        {title}
                    </h2>
                )}
                <ul className="space-y-1">
                    {items.map((item) => {
                        const isActive = activeId === item.id;

                        return (
                            <li key={item.id}>
                                <button
                                    type="button"
                                    onClick={() => onSelect(item.id)}
                                    className={cn(
                                        'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all',
                                        isActive
                                            ? 'bg-primary text-white shadow-md'
                                            : 'text-text-secondary hover:bg-background-secondary hover:text-text-primary'
                                    )}
                                >
                                    {/* 아이콘 */}
                                    <span className="flex-shrink-0">{item.icon}</span>

                                    {/* 라벨 + 설명 */}
                                    <span className="flex-1 text-left min-w-0">
                                        <span className="block truncate">{item.label}</span>
                                        {item.description && (
                                            <span
                                                className={cn(
                                                    'block text-xs mt-0.5 truncate',
                                                    isActive ? 'text-white/70' : 'text-text-tertiary'
                                                )}
                                            >
                                                {item.description}
                                            </span>
                                        )}
                                    </span>

                                    {/* 배지 */}
                                    {item.badge !== undefined && item.badge > 0 && (
                                        <span
                                            className={cn(
                                                'px-2 py-0.5 text-xs font-bold rounded-full',
                                                isActive
                                                    ? 'bg-white/20 text-white'
                                                    : 'bg-error/10 text-error'
                                            )}
                                        >
                                            {item.badge}
                                        </span>
                                    )}
                                </button>
                            </li>
                        );
                    })}
                </ul>
            </nav>
        </aside>
    );
};
