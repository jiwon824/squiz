// 대화 기록 컴포넌트
// 화자별 색상 구분 + 텍스트 표시

import React from 'react';
import { cn } from '@/shared/utils/cn';
import type { TranscriptItem } from './types';
import { getSpeakerClasses } from './constants';

interface EditableTranscriptProps {
    /** 대화 기록 배열 */
    transcript: TranscriptItem[];
    /** 참여자 목록 */
    participants: string[];
    /** 대화 기록 변경 콜백 (하위 호환성 유지) */
    onTranscriptChange?: (updatedTranscript: TranscriptItem[]) => void;
    /** 검색어 (하이라이트용) */
    searchQuery?: string;
    /** 추가 클래스명 */
    className?: string;
}

export const EditableTranscript: React.FC<EditableTranscriptProps> = ({
    transcript,
    participants,
    searchQuery = '',
    className,
}) => {
    // 검색어 하이라이트 여부
    const isHighlighted = (item: TranscriptItem) => {
        if (!searchQuery) return false;
        const q = searchQuery.toLowerCase();
        return item.text.toLowerCase().includes(q) || item.speaker.toLowerCase().includes(q);
    };

    return (
        <div className={cn('space-y-1', className)}>
            {transcript.map((item, idx) => {
                const colorClasses = getSpeakerClasses(item.speaker, participants);
                const highlighted = isHighlighted(item);

                return (
                    <div
                        key={idx}
                        className={cn(
                            'flex gap-4 p-4 rounded-google transition-all',
                            `border-l-4 ${colorClasses.border}`,
                            { 'bg-accent/5': highlighted },
                            'hover:bg-surface-hover'
                        )}
                    >
                        {/* 타임스탬프 */}
                        <div className="flex-shrink-0 w-14 text-xs text-text-tertiary pt-0.5">
                            {item.time}
                        </div>

                        {/* 화자 + 텍스트 */}
                        <div className="flex-1 min-w-0">
                            {/* 화자 이름 */}
                            <span
                                className={cn(
                                    'text-sm font-semibold',
                                    colorClasses.text,
                                    'inline-block mb-1'
                                )}
                            >
                                {item.speaker}
                            </span>

                            {/* 대화 텍스트 */}
                            <p
                                className={cn(
                                    'text-sm text-text-secondary leading-relaxed',
                                    'px-1 -ml-1'
                                )}
                            >
                                {item.text}
                            </p>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};
