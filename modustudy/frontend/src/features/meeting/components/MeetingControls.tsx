import React from 'react';
import '../styles/MeetingRoom.css';

interface MeetingControlsProps {
    isPresenter: boolean;
    micEnabled: boolean;
    shareMode: 'camera' | 'screen' | 'mixed' | null;
    canEndMeeting: boolean;
    pipPosition: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
    onToggleMic: () => void;
    onShareModeChange: (mode: 'camera' | 'screen' | 'mixed') => void;
    onTogglePresenter: () => void;
    onPipPositionChange: (position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right') => void;
    onEndMeeting: () => void;
}

const MeetingControls: React.FC<MeetingControlsProps> = ({
    isPresenter,
    micEnabled,
    shareMode,
    pipPosition,
    onToggleMic,
    onShareModeChange,
    onTogglePresenter,
    canEndMeeting,
    onPipPositionChange,
    onEndMeeting,
}) => {
    return (
        <div className="meeting-controls">
            <div className="meeting-controls__group">
                <button className={`meeting-control ${micEnabled ? 'active' : ''}`} onClick={onToggleMic}>
                    {micEnabled ? '마이크 ON' : '마이크 OFF'}
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
                        {shareMode === 'mixed' && (
                            <div className="meeting-controls__pip">
                                <span>캠 위치</span>
                                <div className="meeting-controls__pip-buttons">
                                    {(['top-left', 'top-right', 'bottom-left', 'bottom-right'] as const).map(
                                        (position) => (
                                            <button
                                                key={position}
                                                className={`meeting-control chip ${pipPosition === position ? 'active' : ''}`}
                                                onClick={() => onPipPositionChange(position)}
                                            >
                                                {position.replace('-', ' ')}
                                            </button>
                                        )
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>

            <div className="meeting-controls__group">
                <button className="meeting-control accent" onClick={onTogglePresenter}>
                    {isPresenter ? '발표 권한 내리기' : '발표자 되기'}
                </button>
            </div>

            <div className="meeting-controls__group">
                <button
                    className="meeting-control danger"
                    onClick={onEndMeeting}
                    disabled={!canEndMeeting}
                    title={canEndMeeting ? '미팅 종료' : '미팅을 시작한 사람만 종료할 수 있습니다.'}
                >
                    미팅 종료
                </button>
            </div>
        </div>
    );
};

export default MeetingControls;
