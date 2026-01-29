// 브레드크럼 공통 컴포넌트
// 페이지 상단 경로 표시용 (대시보드 > STT 미팅 리포트 등)

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

export interface BreadcrumbItem {
    /** 표시할 텍스트 */
    label: string;
    /** 클릭 시 이동할 경로 (없으면 현재 페이지) */
    path?: string;
    /** 클릭 핸들러 (path보다 우선) */
    onClick?: () => void;
}

export interface BreadcrumbProps {
    /** 브레드크럼 아이템 배열 (마지막 아이템이 현재 페이지) */
    items: BreadcrumbItem[];
    /** 추가 클래스명 */
    className?: string;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ items, className }) => {
    const navigate = useNavigate();

    const handleClick = (item: BreadcrumbItem) => {
        if (item.onClick) {
            item.onClick();
        } else if (item.path) {
            navigate(item.path);
        }
    };

    if (items.length === 0) return null;

    return (
        <nav className={cn('flex items-center gap-1.5 text-sm', className)}>
            {items.map((item, index) => {
                const isLast = index === items.length - 1;
                const isClickable = item.path || item.onClick;

                return (
                    <React.Fragment key={index}>
                        {index > 0 && (
                            <ChevronRight size={14} className="text-text-tertiary" />
                        )}
                        {isClickable && !isLast ? (
                            <button
                                onClick={() => handleClick(item)}
                                className="text-text-tertiary hover:text-primary transition-colors"
                            >
                                {item.label}
                            </button>
                        ) : (
                            <span className={cn(
                                isLast
                                    ? 'text-text-primary font-medium'
                                    : 'text-text-tertiary'
                            )}>
                                {item.label}
                            </span>
                        )}
                    </React.Fragment>
                );
            })}
        </nav>
    );
};

export default Breadcrumb;
