// 전체 기록 뷰: 검색 + EditableTranscript + 다운로드/재생

import React, { useState, useCallback } from 'react';
import { Search, Play } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import type { MeetingReport, TranscriptItem } from './types';
import { EditableTranscript } from './EditableTranscript';

interface TranscriptViewProps {
    /** 미팅 리포트 데이터 */
    report: MeetingReport;
    /** 대화 기록 변경 콜백 */
    onTranscriptChange?: (updatedTranscript: TranscriptItem[]) => void;
    /** 추가 클래스명 */
    className?: string;
}

export const TranscriptView: React.FC<TranscriptViewProps> = ({
    report,
    onTranscriptChange,
    className,
}) => {
    const [searchQuery, setSearchQuery] = useState('');

    // 검색 필터링
    const filteredTranscript = report.transcript.filter(item =>
        searchQuery === '' ||
        item.text.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.speaker.toLowerCase().includes(searchQuery.toLowerCase())
    );

    // 대화 기록 변경 핸들러
    const handleTranscriptChange = useCallback((updated: TranscriptItem[]) => {
        onTranscriptChange?.(updated);
    }, [onTranscriptChange]);

    return (
        <div className={cn('space-y-4', className)}>
            {/* 검색 */}
            <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-text-tertiary" size={16} />
                <input
                    type="text"
                    placeholder="대화 내용 검색..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className={cn(
                        'w-full pl-9 pr-4 py-2.5 text-sm',
                        'bg-background border border-border rounded-google-lg',
                        'focus:border-primary focus:ring-2 focus:ring-primary/20 focus:outline-none',
                        'transition-colors'
                    )}
                />
            </div>

            {/* 대화 기록 */}
            <div className="rounded-xl border border-border overflow-hidden">
                <div className={cn(
                    'px-5 py-4 border-b border-border',
                    'bg-background/50'
                )}>
                    <h3 className="font-semibold text-text-primary mb-0">대화 기록</h3>
                </div>
                <div className="p-4 max-h-[400px] overflow-y-auto">
                    <EditableTranscript
                        transcript={filteredTranscript}
                        participants={report.participants}
                        onTranscriptChange={handleTranscriptChange}
                        searchQuery={searchQuery}
                    />
                </div>
            </div>

            {/* 재생 버튼 */}
            <div className="flex justify-center pt-4">
                <button className={cn(
                    'inline-flex items-center gap-2 px-6 py-3 font-medium rounded-xl',
                    'bg-primary text-white hover:bg-primary-dark transition-colors'
                )}>
                    <Play size={18} />
                    음성 재생
                </button>
            </div>
        </div>
    );
};
