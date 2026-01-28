// 위젯 컨테이너 공통 컴포넌트
// 위젯의 외부 래퍼 (그림자, 테두리, 둥근 모서리)

import React from 'react';
import { cn } from '@/shared/utils/cn';

export interface WidgetContainerProps {
    /** 위젯 콘텐츠 */
    children: React.ReactNode;
    /** 추가 클래스명 */
    className?: string;
    /** 패딩 없이 렌더링 (헤더와 바디를 직접 구성할 때) */
    noPadding?: boolean;
}

export const WidgetContainer: React.FC<WidgetContainerProps> = ({
    children,
    className,
    noPadding = true,
}) => {
    return (
        <div className={cn(
            'bg-white rounded-2xl shadow-md border border-gray-100 overflow-hidden',
            !noPadding && 'p-6',
            className
        )}>
            {children}
        </div>
    );
};

export default WidgetContainer;
