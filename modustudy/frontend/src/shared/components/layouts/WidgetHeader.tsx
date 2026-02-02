// 위젯 헤더 공통 컴포넌트
// AI 복습 퀴즈, STT 미팅 리포트, 학습 보관함 등에서 사용

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Maximize2, ChevronLeft, LucideIcon } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

// 색상 스타일 상수
const ICON_BG_STYLES = {
    primary: 'bg-primary/10',
    secondary: 'bg-secondary/10',
    accent: 'bg-accent/10',
    warning: 'bg-warning/10',
    error: 'bg-error/10',
    neutral: 'bg-gray-100',
} as const;

const ICON_TEXT_STYLES = {
    primary: 'text-primary',
    secondary: 'text-secondary',
    accent: 'text-accent',
    warning: 'text-warning',
    error: 'text-error',
    neutral: 'text-gray-900',
} as const;

export type WidgetColorType = keyof typeof ICON_BG_STYLES;

export interface WidgetHeaderProps {
    /** 아이콘 컴포넌트 */
    icon: LucideIcon;
    /** 아이콘 배경 및 텍스트 색상 */
    iconColor?: WidgetColorType;
    /** 헤더 제목 */
    title: string;
    /** 헤더 부제목 */
    subtitle?: string;
    /** 최대화 버튼 클릭 시 이동할 경로 */
    maximizePath?: string;
    /** 최대화 시 페이지 전환 애니메이션 사용 여부 */
    useTransitionAnimation?: boolean;
    /** 뒤로가기 버튼 표시 여부 */
    showBackButton?: boolean;
    /** 뒤로가기 버튼 클릭 핸들러 */
    onBack?: () => void;
    /** 우측 추가 액션 영역 */
    rightActions?: React.ReactNode;
    /** 추가 클래스명 */
    className?: string;
}

export const WidgetHeader: React.FC<WidgetHeaderProps> = ({
    icon: Icon,
    iconColor = 'primary',
    title,
    subtitle,
    maximizePath,
    useTransitionAnimation = false,
    showBackButton = false,
    onBack,
    rightActions,
    className,
}) => {
    const navigate = useNavigate();

    const handleMaximize = () => {
        if (!maximizePath) return;

        if (useTransitionAnimation) {
            // 대시보드에서 다른 페이지로 전환 시 애니메이션 사용
            sessionStorage.setItem('fromDashboard', 'true');

            // 퇴장 애니메이션을 위한 이벤트 발생
            window.dispatchEvent(new CustomEvent('dashboardExit'));

            // 애니메이션 완료 후 네비게이션 (500ms)
            setTimeout(() => {
                navigate(maximizePath);
            }, 500);
        } else {
            navigate(maximizePath);
        }
    };

    return (
        <div className={cn('px-6 pt-6 pb-5 flex items-center justify-between', className)}>
            <div className="flex items-center gap-3">
                {/* 뒤로가기 버튼 */}
                {showBackButton && (
                    <button
                        onClick={onBack}
                        className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors -ml-1.5"
                    >
                        <ChevronLeft size={20} className="text-text-secondary" />
                    </button>
                )}

                {/* 아이콘 */}
                <div className={cn(
                    'w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0',
                    ICON_BG_STYLES[iconColor]
                )}>
                    <Icon className={ICON_TEXT_STYLES[iconColor]} size={20} />
                </div>

                {/* 제목 영역 */}
                <div className="h-10 flex flex-col justify-center">
                    <h3 className="text-lg font-bold text-text-primary leading-6 mb-0">
                        {title}
                    </h3>
                    {subtitle && (
                        <p className="text-xs text-text-tertiary leading-4 mb-0">
                            {subtitle}
                        </p>
                    )}
                </div>
            </div>

            {/* 우측 액션 영역 */}
            <div className="flex items-center gap-2">
                {rightActions}
                {maximizePath && (
                    <button
                        onClick={handleMaximize}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                        title="전체 화면으로 보기"
                    >
                        <Maximize2 size={18} className="text-text-secondary" />
                    </button>
                )}
            </div>
        </div>
    );
};

export default WidgetHeader;
