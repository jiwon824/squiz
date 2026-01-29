// 편집 가능한 대화 기록 컴포넌트
// 화자별 색상 구분 + 화자 변경 + 텍스트 편집

import React, { useCallback } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import type { TranscriptItem } from './types';
import { getSpeakerClasses } from './constants';

interface EditableTranscriptProps {
    /** 대화 기록 배열 */
    transcript: TranscriptItem[];
    /** 참여자 목록 */
    participants: string[];
    /** 대화 기록 변경 콜백 */
    onTranscriptChange: (updatedTranscript: TranscriptItem[]) => void;
    /** 검색어 (하이라이트용) */
    searchQuery?: string;
    /** 추가 클래스명 */
    className?: string;
}

export const EditableTranscript: React.FC<EditableTranscriptProps> = ({
    transcript,
    participants,
    onTranscriptChange,
    searchQuery = '',
    className,
}) => {
    // 화자 변경 핸들러
    const handleSpeakerChange = useCallback((index: number, newSpeaker: string) => {
        const updated = [...transcript];
        updated[index] = { ...updated[index], speaker: newSpeaker };
        onTranscriptChange(updated);
    }, [transcript, onTranscriptChange]);

    // 텍스트 편집 핸들러 (contentEditable → innerText로 XSS 방지)
    const handleTextBlur = useCallback((index: number, e: React.FocusEvent<HTMLParagraphElement>) => {
        const newText = e.currentTarget.innerText;
        if (newText !== transcript[index].text) {
            const updated = [...transcript];
            updated[index] = { ...updated[index], text: newText };
            onTranscriptChange(updated);
        }
    }, [transcript, onTranscriptChange]);

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
                            {/* 화자 이름 (클릭 → 드롭다운) */}
                            <div className="relative inline-block mb-1">
                                <select
                                    value={item.speaker}
                                    onChange={(e) => handleSpeakerChange(idx, e.target.value)}
                                    className={cn(
                                        'text-sm font-semibold cursor-pointer',
                                        'bg-transparent outline-none',
                                        colorClasses.text,
                                        'hover:bg-surface-hover rounded-google',
                                        'pl-1 pr-5 py-0.5 -ml-1'
                                    )}
                                    style={{ appearance: 'none', WebkitAppearance: 'none' }}
                                >
                                    {participants.map((p) => (
                                        <option key={p} value={p}>{p}</option>
                                    ))}
                                </select>
                                {/* 커스텀 화살표 */}
                                <ChevronDown
                                    size={12}
                                    className="absolute right-1 top-1/2 -translate-y-1/2 pointer-events-none text-text-tertiary"
                                />
                            </div>

                            {/* 대화 텍스트 (편집 가능) */}
                            <p
                                contentEditable
                                suppressContentEditableWarning
                                onBlur={(e) => handleTextBlur(idx, e)}
                                className={cn(
                                    'text-sm text-text-secondary leading-relaxed',
                                    'outline-none rounded-google px-1 -ml-1',
                                    'focus:ring-2 focus:ring-primary/20 focus:bg-surface'
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
