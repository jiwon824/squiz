// 페이지 네비게이션 헤더 공통 컴포넌트
// 브레드크럼 + 뒤로가기 버튼 + 타이틀 세트
// PageLayout과 달리 페이지를 감싸지 않고 헤더만 독립적으로 사용 가능

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import { Breadcrumb, type BreadcrumbItem } from './Breadcrumb';

export interface PageNavHeaderProps {
    /** 페이지 제목 */
    title: string;
    /** 브레드크럼 아이템 배열 */
    breadcrumbs?: BreadcrumbItem[];
    /** 뒤로가기 버튼 클릭 핸들러 (없으면 navigate(-1)) */
    onBack?: () => void;
    /** 뒤로가기 버튼 숨김 */
    hideBackButton?: boolean;
    /** 제목 옆 뱃지 */
    badge?: {
        text: string;
        className?: string;
    };
    /** 우측 액션 영역 */
    rightActions?: React.ReactNode;
    /** 추가 클래스명 */
    className?: string;
}

export const PageNavHeader: React.FC<PageNavHeaderProps> = ({
    title,
    breadcrumbs = [],
    onBack,
    hideBackButton = false,
    badge,
    rightActions,
    className,
}) => {
    const navigate = useNavigate();

    const handleBack = () => {
        if (onBack) {
            onBack();
        } else {
            navigate(-1);
        }
    };

    return (
        <div className={cn('mb-6', className)}>
            {/* 브레드크럼 */}
            {breadcrumbs.length > 0 && (
                <Breadcrumb items={breadcrumbs} className="mb-2" />
            )}

            {/* 뒤로가기 + 타이틀 + 우측 액션 */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                    {!hideBackButton && (
                        <button
                            onClick={handleBack}
                            className="text-text-tertiary hover:text-text-primary transition-colors"
                            aria-label="뒤로 가기"
                        >
                            <ChevronLeft size={24} strokeWidth={1.5} />
                        </button>
                    )}
                    <h1 className="text-2xl font-bold text-text-primary mb-0">
                        {title}
                    </h1>
                    {badge && (
                        <span className={cn(
                            'px-3 py-1 rounded-full text-sm font-medium',
                            badge.className || 'bg-primary/10 text-primary'
                        )}>
                            {badge.text}
                        </span>
                    )}
                </div>

                {rightActions && (
                    <div className="flex items-center gap-2">
                        {rightActions}
                    </div>
                )}
            </div>
        </div>
    );
};

export default PageNavHeader;
