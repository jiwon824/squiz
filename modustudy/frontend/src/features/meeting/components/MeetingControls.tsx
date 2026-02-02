import React from 'react';
import '../styles/MeetingRoom.css';

interface MeetingControlsProps {
    isPresenter: boolean;
    micEnabled: boolean;
    micDisabled: boolean;
    shareMode: 'camera' | 'screen' | 'mixed' | null;
    canEndMeeting: boolean;
    captureDisabled: boolean;
    canExtendMeeting: boolean;
    extendDisabled: boolean;
    onToggleMic: () => void;
    onShareModeChange: (mode: 'camera' | 'screen' | 'mixed') => void;
    onTogglePresenter: () => void;
    onEndMeeting: () => void;
    onCapture: () => void;
    onExtendMeeting: () => void;
}

const MeetingControls: React.FC<MeetingControlsProps> = ({
    isPresenter,
    micEnabled,
    micDisabled,
    shareMode,
    onToggleMic,
    onShareModeChange,
    onTogglePresenter,
    canEndMeeting,
    onEndMeeting,
    captureDisabled,
    onCapture,
    canExtendMeeting,
    extendDisabled,
    onExtendMeeting,
}) => {
    return (
        <div className="meeting-controls">
            <div className="meeting-controls__group">
                <button
                    className={`meeting-control ${micEnabled ? 'active' : ''}`}
                    onClick={onToggleMic}
                    disabled={micDisabled}
                >
                    {micEnabled ? '마이크 끄기' : '마이크 켜기'}
                </button>
                {isPresenter && (
                    <div className="meeting-controls__share">
                        <button
                            className={`meeting-control ${shareMode === 'camera' ? 'active' : ''}`}
                            onClick={() => onShareModeChange('camera')}
                        >
                            캠
                        </button>
                        <button
                            className={`meeting-control ${shareMode === 'screen' ? 'active' : ''}`}
                            onClick={() => onShareModeChange('screen')}
                        >
                            화면
                        </button>
                        <button
                            className={`meeting-control ${shareMode === 'mixed' ? 'active' : ''}`}
                            onClick={() => onShareModeChange('mixed')}
                        >
                            화면+캠
                        </button>
                    </div>
                )}
            </div>

            <div className="meeting-controls__group">
                <button className="meeting-control accent" onClick={onTogglePresenter}>
                    {isPresenter ? '발표 권한 내리기' : '발표자 되기'}
                </button>
            </div>

            <div className="meeting-controls__group">
                <div className="meeting-control__tooltip-wrapper">
                    <button
                        className="meeting-control"
                        onClick={onCapture}
                        disabled={captureDisabled}
                        aria-label={captureDisabled ? '캡쳐 중입니다.' : '화면 캡쳐'}
                    >
                        화면 캡쳐
                    </button>
                    <span className="meeting-control__tooltip" role="tooltip">
                        {captureDisabled ? '캡쳐 중입니다.' : '발표화면 캡쳐 후 저장, 복사합니다.'}
                    </span>
                </div>
                <button
                    className="meeting-control"
                    onClick={onExtendMeeting}
                    disabled={!canExtendMeeting || extendDisabled}
                    title={
                        canExtendMeeting
                            ? extendDisabled
                                ? '최대 3시간까지 연장할 수 있습니다.'
                                : '미팅 시간을 30분 연장합니다.'
                            : '스터디장만 연장할 수 있습니다.'
                    }
                >
                    미팅 30분 추가
                </button>
                <button
                    className="meeting-control danger"
                    onClick={onEndMeeting}
                    disabled={!canEndMeeting}
                    title={canEndMeeting ? '미팅 종료' : '스터디장만 종료할 수 있습니다.'}
                >
                    미팅 종료
                </button>
            </div>
        </div>
    );
};

export default MeetingControls;
