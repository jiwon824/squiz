// 페이지 레이아웃 공통 컴포넌트
// STTReportPage, LearningArchivePage, StudyAfterQuiz 등에서 사용

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

// 브레드크럼 아이템 타입
export interface BreadcrumbItem {
    /** 표시할 텍스트 */
    label: string;
    /** 클릭 시 이동할 경로 (없으면 현재 페이지) */
    path?: string;
    /** 클릭 핸들러 (path보다 우선) */
    onClick?: () => void;
}

export interface PageLayoutProps {
    /** 페이지 제목 */
    title: string;
    /** 브레드크럼 아이템 배열 */
    breadcrumbs?: BreadcrumbItem[];
    /** 뒤로가기 버튼 클릭 핸들러 */
    onBack?: () => void;
    /** 제목 옆 뱃지 */
    badge?: {
        text: string;
        color?: 'primary' | 'secondary' | 'accent' | 'warning' | 'error';
    };
    /** 페이지 콘텐츠 */
    children: React.ReactNode;
    /** 컨테이너 없이 children만 렌더링 */
    noContainer?: boolean;
    /** 추가 클래스명 */
    className?: string;
}

// 뱃지 색상 스타일
const BADGE_STYLES = {
    primary: 'bg-primary/10 text-primary',
    secondary: 'bg-secondary/10 text-secondary',
    accent: 'bg-accent/10 text-accent',
    warning: 'bg-warning/10 text-warning',
    error: 'bg-error/10 text-error',
} as const;

export const PageLayout: React.FC<PageLayoutProps> = ({
    title,
    breadcrumbs = [],
    onBack,
    badge,
    children,
    noContainer = false,
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

    const handleBreadcrumbClick = (item: BreadcrumbItem) => {
        if (item.onClick) {
            item.onClick();
        } else if (item.path) {
            navigate(item.path);
        }
    };

    return (
        <div className={cn('py-8', className)}>
            <div className="max-w-[1400px] mx-auto px-8">
                {/* 브레드크럼 + 미니멀 헤더 */}
                <div className="mb-6">
                    {/* 브레드크럼 */}
                    {breadcrumbs.length > 0 && (
                        <nav className="flex items-center gap-1.5 text-sm mb-2">
                            {breadcrumbs.map((item, index) => {
                                const isLast = index === breadcrumbs.length - 1;
                                const isClickable = item.path || item.onClick;

                                return (
                                    <React.Fragment key={index}>
                                        {index > 0 && (
                                            <ChevronRight size={14} className="text-text-tertiary" />
                                        )}
                                        {isClickable && !isLast ? (
                                            <button
                                                onClick={() => handleBreadcrumbClick(item)}
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
                    )}

                    {/* 페이지 타이틀 + 뒤로가기 */}
                    <div className="flex items-center gap-3">
                        <button
                            onClick={handleBack}
                            className="text-text-tertiary hover:text-text-primary transition-colors"
                        >
                            <ChevronLeft size={24} strokeWidth={1.5} />
                        </button>
                        <h1 className="text-2xl font-bold text-text-primary mb-0">
                            {title}
                        </h1>
                        {badge && (
                            <span className={cn(
                                'px-3 py-1 rounded-full text-sm font-medium',
                                BADGE_STYLES[badge.color || 'primary']
                            )}>
                                {badge.text}
                            </span>
                        )}
                    </div>
                </div>

                {/* 콘텐츠 영역 */}
                {noContainer ? (
                    children
                ) : (
                    <div className="bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden">
                        {children}
                    </div>
                )}
            </div>
        </div>
    );
};

export default PageLayout;
