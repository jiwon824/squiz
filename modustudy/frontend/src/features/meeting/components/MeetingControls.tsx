import React from 'react';
import { cn } from '@/shared/utils/cn';
import { conditionalClasses } from '@/shared/utils/cn';
import { Button } from '@/shared/components/Button';
import {
    Mic,
    MicOff,
    Camera,
    Monitor,
    MonitorPlay,
    Hand,
    Aperture,
    Clock,
    PhoneOff,
} from 'lucide-react';

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
        <div className="flex items-center gap-2 flex-wrap">
            {/* 미디어 그룹 */}
            <div className="flex items-center gap-1.5">
                <Button
                    variant={micEnabled ? 'primary' : 'secondary'}
                    size="xs"
                    leftIcon={micEnabled ? <Mic size={16} /> : <MicOff size={16} />}
                    onClick={onToggleMic}
                    disabled={micDisabled}
                    tooltip={micEnabled ? '마이크 끄기' : '마이크 켜기'}
                >
                    <span className="hidden sm:inline">{micEnabled ? '마이크 끄기' : '마이크 켜기'}</span>
                </Button>

                {isPresenter && (
                    <>
                        <Button
                            variant={shareMode === 'camera' ? 'primary' : 'secondary'}
                            size="xs"
                            leftIcon={<Camera size={16} />}
                            onClick={() => onShareModeChange('camera')}
                            tooltip="카메라 공유"
                        >
                            <span className="hidden sm:inline">캠</span>
                        </Button>
                        <Button
                            variant={shareMode === 'screen' ? 'primary' : 'secondary'}
                            size="xs"
                            leftIcon={<Monitor size={16} />}
                            onClick={() => onShareModeChange('screen')}
                            tooltip="화면 공유"
                        >
                            <span className="hidden sm:inline">화면</span>
                        </Button>
                        <Button
                            variant={shareMode === 'mixed' ? 'primary' : 'secondary'}
                            size="xs"
                            leftIcon={<MonitorPlay size={16} />}
                            onClick={() => onShareModeChange('mixed')}
                            tooltip="화면+카메라 공유"
                        >
                            <span className="hidden sm:inline">화면+캠</span>
                        </Button>
                    </>
                )}
            </div>

            {/* 구분선 */}
            <div className="w-px h-8 bg-gray-200 mx-1 hidden sm:block" />

            {/* 발표자 그룹 */}
            <div className="flex items-center gap-1.5">
                <Button
                    variant={isPresenter ? 'primary' : 'secondary'}
                    size="xs"
                    leftIcon={<Hand size={16} />}
                    onClick={onTogglePresenter}
                    tooltip={isPresenter ? '발표 권한 내리기' : '발표자 되기'}
                    className={cn(
                        conditionalClasses.state(
                            isPresenter,
                            'bg-amber-500 hover:bg-amber-600',
                            ''
                        )
                    )}
                >
                    <span className="hidden sm:inline">{isPresenter ? '발표 권한 내리기' : '발표자 되기'}</span>
                </Button>
            </div>

            {/* 구분선 */}
            <div className="w-px h-8 bg-gray-200 mx-1 hidden sm:block" />

            {/* 유틸리티 그룹 */}
            <div className="flex items-center gap-1.5">
                <Button
                    variant="secondary"
                    size="xs"
                    leftIcon={<Aperture size={16} />}
                    onClick={onCapture}
                    disabled={captureDisabled}
                    tooltip={captureDisabled ? '캡쳐 중입니다.' : '발표화면 캡쳐 후 저장, 복사합니다.'}
                >
                    <span className="hidden sm:inline">캡쳐</span>
                </Button>

                <Button
                    variant="secondary"
                    size="xs"
                    leftIcon={<Clock size={16} />}
                    onClick={onExtendMeeting}
                    disabled={!canExtendMeeting || extendDisabled}
                    tooltip={
                        canExtendMeeting
                            ? extendDisabled
                                ? '최대 3시간까지 연장할 수 있습니다.'
                                : '미팅 시간을 30분 연장합니다.'
                            : '스터디장만 연장할 수 있습니다.'
                    }
                >
                    <span className="hidden sm:inline">+30분</span>
                </Button>

                <Button
                    variant="danger"
                    size="xs"
                    leftIcon={<PhoneOff size={16} />}
                    onClick={onEndMeeting}
                    disabled={!canEndMeeting}
                    tooltip={canEndMeeting ? '미팅 종료' : '스터디장만 종료할 수 있습니다.'}
                >
                    <span className="hidden sm:inline">종료</span>
                </Button>
            </div>
        </div>
    );
};

export default MeetingControls;
