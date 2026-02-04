import React, { useEffect, useRef } from 'react';
import { cn, conditionalClasses } from '@/shared/utils/cn';
import { Crown, UserCircle } from 'lucide-react';

interface VideoTileProps {
    stream: MediaStream | null;
    label: string;
    isPresenter?: boolean;
    isLocal?: boolean;
    isScreenShare?: boolean;
}

// 비디오 타일 컴포넌트
const VideoTile: React.FC<VideoTileProps> = ({ stream, label, isPresenter, isLocal, isScreenShare }) => {
    const videoRef = useRef<HTMLVideoElement | null>(null);

    useEffect(() => {
        if (!videoRef.current) return;
        videoRef.current.muted = true;
        if (stream) {
            videoRef.current.srcObject = stream;
            videoRef.current.play().catch(() => {});
        }
    }, [stream]);

    return (
        <div className={cn(
            'relative rounded-2xl overflow-hidden bg-gray-900 min-h-[200px] w-full h-full',
            isPresenter && 'ring-2 ring-amber-400 ring-offset-2 ring-offset-gray-900'
        )}>
            {stream ? (
                <video
                    ref={videoRef}
                    autoPlay
                    playsInline
                    muted
                    className={cn(
                        'w-full h-full object-contain block',
                        isLocal && !isScreenShare && '-scale-x-100'
                    )}
                />
            ) : (
                <div className="w-full h-full flex items-center justify-center">
                    <UserCircle size={64} className="text-gray-600" />
                </div>
            )}

            {/* 하단 그라데이션 오버레이 */}
            <div className="absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-black/60 to-transparent pointer-events-none" />

            {/* 발표자 배지 (좌상단) */}
            {isPresenter && (
                <div className="absolute top-3 left-3 inline-flex items-center gap-1.5 px-2.5 py-1 bg-amber-500 text-white text-sm font-medium rounded-full shadow-sm">
                    <Crown size={14} />
                    발표자
                </div>
            )}

            {/* 이름 라벨 (좌하단) */}
            <div className="absolute left-3 bottom-3 inline-flex items-center gap-1.5 text-white text-base font-medium drop-shadow-md">
                {label}
            </div>
        </div>
    );
};

interface MeetingVideoStageProps {
    localStream: MediaStream | null;
    localLabel: string;
    localIsPresenter: boolean;
    isScreenSharing?: boolean;
    remoteVideoStreams: Array<{
        id: string;
        stream: MediaStream;
        label: string;
        isPresenter?: boolean;
    }>;
    containerRef?: React.Ref<HTMLDivElement>;
}

const MeetingVideoStage: React.FC<MeetingVideoStageProps> = ({
    localStream,
    localLabel,
    localIsPresenter,
    isScreenSharing,
    remoteVideoStreams,
    containerRef,
}) => {
    const primaryRemote =
        remoteVideoStreams.find((item) => item.isPresenter) || remoteVideoStreams[0] || null;
    const showRemote = !localIsPresenter;
    const hasSingleTile = Boolean((localIsPresenter && localStream) || (showRemote && primaryRemote));

    return (
        <div
            className={cn(
                'grid gap-3 h-full',
                conditionalClasses.state(
                    hasSingleTile,
                    'grid-cols-1',
                    'grid-cols-[repeat(auto-fit,minmax(240px,1fr))]'
                )
            )}
            ref={containerRef}
        >
            {localIsPresenter && localStream && (
                <VideoTile stream={localStream} label={`${localLabel} (나)`} isPresenter isLocal isScreenShare={isScreenSharing} />
            )}
            {showRemote && primaryRemote && (
                <VideoTile
                    key={primaryRemote.id}
                    stream={primaryRemote.stream}
                    label={primaryRemote.label}
                    isPresenter={primaryRemote.isPresenter}
                />
            )}
            {!localStream && remoteVideoStreams.length === 0 && (
                <div className="flex flex-col items-center justify-center gap-3 py-12 text-gray-400">
                    <UserCircle size={56} />
                    <span className="text-base">미디어 스트림이 없습니다.</span>
                </div>
            )}
        </div>
    );
};

export default MeetingVideoStage;
