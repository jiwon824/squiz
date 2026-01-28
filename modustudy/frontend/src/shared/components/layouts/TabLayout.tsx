// 탭 레이아웃 공통 컴포넌트
// 좌측 탭 네비게이션 + 우측 콘텐츠 영역
// StudyAfterQuiz, STTReportPage, LearningArchivePage 등에서 사용

import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { LucideIcon } from 'lucide-react';
import { cn } from '@/shared/utils/cn';

// 탭 아이템 타입
export interface TabItem<T extends string = string> {
    /** 탭 고유 ID */
    id: T;
    /** 탭 라벨 */
    label: string;
    /** 탭 아이콘 */
    icon: LucideIcon;
    /** 카운트 뱃지 (선택) */
    count?: number;
}

export interface TabLayoutProps<T extends string = string> {
    /** 탭 아이템 배열 */
    tabs: TabItem<T>[];
    /** 현재 활성 탭 ID */
    activeTab: T;
    /** 탭 변경 핸들러 */
    onTabChange: (tabId: T) => void;
    /** 탭별 콘텐츠 렌더 함수 */
    children: React.ReactNode;
    /** 좌측 탭 영역 너비 (기본: w-60) */
    tabWidth?: string;
    /** 애니메이션 layoutId (기본: 'tab-indicator') */
    layoutId?: string;
    /** 추가 클래스명 */
    className?: string;
}

export const TabLayout = <T extends string = string>({
    tabs,
    activeTab,
    onTabChange,
    children,
    tabWidth = 'w-60',
    layoutId = 'tab-indicator',
    className,
}: TabLayoutProps<T>) => {
    return (
        <div className={cn('flex', className)}>
            {/* 좌측 탭 네비게이션 */}
            <div className={cn(tabWidth, 'flex-shrink-0 bg-gray-50/70 relative')}>
                {tabs.map((tab) => {
                    const Icon = tab.icon;
                    const isActive = activeTab === tab.id;

                    return (
                        <button
                            key={tab.id}
                            onClick={() => onTabChange(tab.id)}
                            className={cn(
                                'w-full flex items-center gap-2 px-4 py-4 text-sm font-medium transition-colors relative',
                                isActive
                                    ? 'text-primary bg-white -mr-px z-10'
                                    : 'text-text-secondary hover:text-text-primary hover:bg-gray-100 border-b border-gray-100'
                            )}
                        >
                            {/* 왼쪽 인디케이터 */}
                            {isActive && (
                                <motion.div
                                    layoutId={layoutId}
                                    className="absolute left-0 top-0 bottom-0 w-1 bg-primary rounded-r"
                                />
                            )}
                            <Icon size={18} />
                            <span>{tab.label}</span>
                            {tab.count !== undefined && (
                                <span className={cn(
                                    'ml-auto px-1.5 py-0.5 rounded-full text-xs',
                                    isActive
                                        ? 'bg-primary/10 text-primary'
                                        : 'bg-gray-200 text-text-tertiary'
                                )}>
                                    {tab.count}
                                </span>
                            )}
                        </button>
                    );
                })}
                {/* 우측 border line */}
                <div className="absolute top-0 right-0 bottom-0 w-px bg-gray-200" />
            </div>

            {/* 우측 콘텐츠 영역 */}
            <div className="flex-1 p-8">
                {children}
            </div>
        </div>
    );
};

// 탭 콘텐츠 래퍼 (애니메이션 적용)
export interface TabContentProps {
    /** 고유 키 (탭 ID) */
    tabKey: string;
    /** 탭 콘텐츠 */
    children: React.ReactNode;
}

export const TabContent: React.FC<TabContentProps> = ({ tabKey, children }) => {
    return (
        <motion.div
            key={tabKey}
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -10 }}
        >
            {children}
        </motion.div>
    );
};

// AnimatePresence 래퍼
export const TabContentWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    return (
        <AnimatePresence mode="wait">
            {children}
        </AnimatePresence>
    );
};

export default TabLayout;
