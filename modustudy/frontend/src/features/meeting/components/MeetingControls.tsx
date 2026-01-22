import React from 'react';
import '../styles/MeetingRoom.css';

interface MeetingControlsProps {
    isPresenter: boolean;
    micEnabled: boolean;
    cameraEnabled: boolean;
    screenSharing: boolean;
    canEndMeeting: boolean;
    pipPosition: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right';
    onToggleMic: () => void;
    onToggleCamera: () => void;
    onToggleScreenShare: () => void;
    onTogglePresenter: () => void;
    onPipPositionChange: (position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right') => void;
    onEndMeeting: () => void;
}

const MeetingControls: React.FC<MeetingControlsProps> = ({
    isPresenter,
    micEnabled,
    cameraEnabled,
    screenSharing,
    pipPosition,
    onToggleMic,
    onToggleCamera,
    onToggleScreenShare,
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
                    <>
                        <button
                            className={`meeting-control ${cameraEnabled ? 'active' : ''}`}
                            onClick={onToggleCamera}
                        >
                            {cameraEnabled ? '카메라 ON' : '카메라 OFF'}
                        </button>
                        <button
                            className={`meeting-control ${screenSharing ? 'active' : ''}`}
                            onClick={onToggleScreenShare}
                        >
                            {screenSharing ? '화면 공유 중' : '화면 공유'}
                        </button>
                    </>
                )}
            </div>

            <div className="meeting-controls__group">
                <button className="meeting-control accent" onClick={onTogglePresenter}>
                    {isPresenter ? '발표 권한 내리기' : '발표자 되기'}
                </button>
                {isPresenter && (
                    <div className="meeting-controls__pip">
                        <span>캠 위치</span>
                        <div className="meeting-controls__pip-buttons">
                            {(['top-left', 'top-right', 'bottom-left', 'bottom-right'] as const).map((position) => (
                                <button
                                    key={position}
                                    className={`meeting-control chip ${pipPosition === position ? 'active' : ''}`}
                                    onClick={() => onPipPositionChange(position)}
                                    disabled={!screenSharing || !cameraEnabled}
                                >
                                    {position.replace('-', ' ')}
                                </button>
                            ))}
                        </div>
                    </div>
                )}
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
