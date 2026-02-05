import React from 'react';
import { cn, classBuilder, conditionalClasses } from '@/shared/utils/cn';
import { ArrowLeft, Presentation, Users } from 'lucide-react';
import MeetingControls from './MeetingControls';

interface MeetingRoomHeaderProps {
    meetingTitle: string;
    elapsedLabel: string;
    plannedLabel: string | null;
    timeWarning: string | null;
    isPresenter: boolean;
    onGoList: () => void;
    micEnabled: boolean;
    micDisabled: boolean;
    shareMode: 'camera' | 'screen' | 'mixed' | null;
    onToggleMic: () => void;
    onShareModeChange: (mode: 'camera' | 'screen' | 'mixed') => void;
    onTogglePresenter: () => void;
    onEndMeeting: () => void;
    onCapture: () => void;
    captureDisabled: boolean;
    canEndMeeting: boolean;
    canExtendMeeting: boolean;
    extendDisabled: boolean;
    onExtendMeeting: () => void;
    elapsedSeconds?: number;
    plannedDurationSeconds?: number | null;
}

const MeetingRoomHeader: React.FC<MeetingRoomHeaderProps> = ({
    meetingTitle,
    elapsedLabel,
    plannedLabel,
    timeWarning,
    isPresenter,
    onGoList,
    micEnabled,
    micDisabled,
    shareMode,
    onToggleMic,
    onShareModeChange,
    onTogglePresenter,
    onEndMeeting,
    onCapture,
    captureDisabled,
    canEndMeeting,
    canExtendMeeting,
    extendDisabled,
    onExtendMeeting,
    elapsedSeconds = 0,
    plannedDurationSeconds,
}) => {
    // 프로그레스 바 비율 계산
    const progressPercent = plannedDurationSeconds
        ? Math.min((elapsedSeconds / plannedDurationSeconds) * 100, 100)
        : 0;

    return (
        <div className={cn(classBuilder.card('elevated'), 'flex flex-col gap-3 p-4 shrink-0')}>
            {/* 상단: 제목 + 타이머 + 상태 */}
            <div className="flex items-center justify-between gap-4 flex-wrap">
                <div className="flex items-center gap-4 min-w-0 flex-1">
                    <h1 className="text-xl font-bold text-gray-900 truncate max-w-[180px] sm:max-w-[280px] lg:max-w-[400px]">
                        {meetingTitle || '미팅 룸'}
                    </h1>

                    {/* 타이머 */}
                    <div className="flex items-center gap-3 shrink-0">
                        <span className="font-mono text-lg font-semibold text-gray-700 tabular-nums">
                            {elapsedLabel}
                        </span>
                        {plannedLabel && (
                            <span className="text-sm text-gray-400">
                                / {plannedLabel}
                            </span>
                        )}
                    </div>

                    {/* 시간 경고 */}
                    {timeWarning && (
                        <span className="text-sm font-semibold text-red-500 animate-pulse shrink-0">
                            {timeWarning}
                        </span>
                    )}
                </div>

                <div className="flex items-center gap-3 shrink-0">
                    {/* 모드 배지 */}
                    <div className={cn(
                        'inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full text-sm font-medium',
                        conditionalClasses.state(
                            isPresenter,
                            'bg-amber-50 text-amber-700',
                            'bg-blue-50 text-blue-700'
                        )
                    )}>
                        {isPresenter ? <Presentation size={16} /> : <Users size={16} />}
                        {isPresenter ? '발표자 모드' : '참가자 모드'}
                    </div>

                    <button
                        className="inline-flex items-center gap-2 px-4 py-2 rounded-full text-base text-gray-500 hover:bg-gray-100 transition-colors cursor-pointer active:scale-95"
                        onClick={onGoList}
                    >
                        <ArrowLeft size={18} />
                        목록으로
                    </button>
                </div>
            </div>

            {/* 프로그레스 바 (예정 시간이 있을 때만) */}
            {plannedDurationSeconds && plannedDurationSeconds > 0 && (
                <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
                    <div
                        className={cn(
                            'h-full rounded-full transition-all duration-1000 ease-linear',
                            progressPercent >= 90 ? 'bg-red-500' :
                            progressPercent >= 75 ? 'bg-amber-500' :
                            'bg-blue-500'
                        )}
                        style={{ width: `${progressPercent}%` }}
                    />
                </div>
            )}

            {/* 컨트롤 바 */}
            <MeetingControls
                isPresenter={isPresenter}
                micEnabled={micEnabled}
                micDisabled={micDisabled}
                shareMode={shareMode}
                onToggleMic={onToggleMic}
                onShareModeChange={onShareModeChange}
                onTogglePresenter={onTogglePresenter}
                onEndMeeting={onEndMeeting}
                canEndMeeting={canEndMeeting}
                captureDisabled={captureDisabled}
                onCapture={onCapture}
                canExtendMeeting={canExtendMeeting}
                extendDisabled={extendDisabled}
                onExtendMeeting={onExtendMeeting}
            />
        </div>
    );
};

export default MeetingRoomHeader;
