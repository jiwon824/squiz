import React from 'react';
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
}) => {
    return (
        <div className="meeting-room__meta">
            <div className="meeting-room__meta-row">
                <div className="meeting-room__meta-left">
                    <div className="meeting-room__meta-title">
                        <h1>{meetingTitle || '미팅 룸'}</h1>
                        <div className="meeting-room__meta-times">
                            <div className="meeting-room__meta-time-row">
                                <div className="meeting-room__timer">진행 시간: {elapsedLabel}</div>
                                {plannedLabel ? (
                                    <div className="meeting-room__timer">예정 시간: {plannedLabel}</div>
                                ) : null}
                            </div>
                            {timeWarning && (
                                <div className="meeting-room__timer meeting-room__timer--warning">
                                    {timeWarning}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
                <div className="meeting-room__meta-right">
                    <div className="meeting-room__status">
                        <span>{isPresenter ? '발표자 모드' : '참가자 모드'}</span>
                    </div>
                    <button className="meeting-btn ghost" onClick={onGoList}>
                        목록으로
                    </button>
                </div>
            </div>

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
