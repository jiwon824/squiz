import { useState, useEffect, useRef } from 'react';
import { SpeechSegment } from '../types';
import { cn } from '@/shared/utils/cn';

interface RealtimeCaptionsProps {
    segments: SpeechSegment[];
    isEnabled: boolean;
    onToggle: () => void;
    maxDisplayCount?: number;  // 표시할 최대 자막 수
    autoHideDelay?: number;    // 자막 자동 숨김 지연 (ms)
}

/**
 * 실시간 자막 컴포넌트
 * 미팅 중 발화 내용을 실시간으로 화면 하단에 표시
 */
export const RealtimeCaptions = ({
    segments,
    isEnabled,
    onToggle,
    maxDisplayCount = 3,
    autoHideDelay = 10000,
}: RealtimeCaptionsProps) => {
    const [visibleSegments, setVisibleSegments] = useState<SpeechSegment[]>([]);
    const containerRef = useRef<HTMLDivElement>(null);

    // 새 세그먼트가 추가되면 표시 목록 업데이트
    useEffect(() => {
        if (!isEnabled || segments.length === 0) {
            setVisibleSegments([]);
            return;
        }

        // 최근 N개만 표시
        const recentSegments = segments.slice(-maxDisplayCount);
        setVisibleSegments(recentSegments);

        // 자동 스크롤
        if (containerRef.current) {
            containerRef.current.scrollTop = containerRef.current.scrollHeight;
        }
    }, [segments, isEnabled, maxDisplayCount]);

    // 자동 숨김 타이머
    useEffect(() => {
        if (!isEnabled || visibleSegments.length === 0) return;

        const timer = setTimeout(() => {
            // 마지막 세그먼트가 autoHideDelay 이상 지났으면 점점 페이드 아웃
            const lastSegment = visibleSegments[visibleSegments.length - 1];
            const elapsed = Date.now() - lastSegment.timestamp;
            if (elapsed > autoHideDelay) {
                setVisibleSegments((prev) => prev.slice(1));
            }
        }, autoHideDelay);

        return () => clearTimeout(timer);
    }, [visibleSegments, isEnabled, autoHideDelay]);

    if (!isEnabled) {
        return (
            <button
                onClick={onToggle}
                className={cn(
                    'fixed bottom-20 left-1/2 -translate-x-1/2 z-40',
                    'px-4 py-2 rounded-full',
                    'bg-gray-800/80 text-white text-sm',
                    'hover:bg-gray-700/80 transition-colors',
                    'flex items-center gap-2'
                )}
            >
                <span className="text-lg">CC</span>
                <span>자막 켜기</span>
            </button>
        );
    }

    return (
        <div className="fixed bottom-20 left-0 right-0 z-40 pointer-events-none">
            <div className="max-w-3xl mx-auto px-4">
                {/* 자막 토글 버튼 */}
                <div className="flex justify-center mb-2 pointer-events-auto">
                    <button
                        onClick={onToggle}
                        className={cn(
                            'px-3 py-1 rounded-full text-xs',
                            'bg-blue-600/90 text-white',
                            'hover:bg-blue-500/90 transition-colors'
                        )}
                    >
                        자막 끄기
                    </button>
                </div>

                {/* 자막 표시 영역 */}
                <div
                    ref={containerRef}
                    className={cn(
                        'bg-black/75 rounded-lg p-3',
                        'max-h-32 overflow-y-auto',
                        'pointer-events-auto'
                    )}
                >
                    {visibleSegments.length === 0 ? (
                        <p className="text-gray-400 text-center text-sm">
                            발화를 기다리는 중...
                        </p>
                    ) : (
                        <div className="space-y-2">
                            {visibleSegments.map((segment, index) => (
                                <CaptionItem
                                    key={`${segment.timestamp}-${index}`}
                                    segment={segment}
                                    isLatest={index === visibleSegments.length - 1}
                                />
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

interface CaptionItemProps {
    segment: SpeechSegment;
    isLatest: boolean;
}

const CaptionItem = ({ segment, isLatest }: CaptionItemProps) => {
    const formatTime = (timestamp: number) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('ko-KR', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
        });
    };

    return (
        <div
            className={cn(
                'flex items-start gap-2 text-white',
                'animate-fadeIn',
                isLatest && 'font-medium'
            )}
        >
            <span className="text-blue-400 text-sm whitespace-nowrap flex-shrink-0">
                {segment.speakerName}:
            </span>
            <span className={cn('text-sm', isLatest ? 'text-white' : 'text-gray-300')}>
                {segment.text}
            </span>
            <span className="text-gray-500 text-xs whitespace-nowrap flex-shrink-0 ml-auto">
                {formatTime(segment.timestamp)}
            </span>
        </div>
    );
};

export default RealtimeCaptions;
