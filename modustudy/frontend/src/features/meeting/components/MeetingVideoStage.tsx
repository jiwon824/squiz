import React, { useEffect, useRef } from 'react';
import '../styles/MeetingRoom.css';

interface VideoTileProps {
    stream: MediaStream | null;
    label: string;
    isPresenter?: boolean;
    isLocal?: boolean;
}

const VideoTile: React.FC<VideoTileProps> = ({ stream, label, isPresenter, isLocal }) => {
    const videoRef = useRef<HTMLVideoElement | null>(null);

    useEffect(() => {
        if (!videoRef.current) return;
        if (stream) {
            videoRef.current.srcObject = stream;
            videoRef.current.play().catch(() => {});
        }
    }, [stream]);

    return (
        <div className={`meeting-video-tile ${isPresenter ? 'presenter' : ''}`}>
            <video ref={videoRef} autoPlay playsInline muted={isLocal} />
            <div className="meeting-video-tile__label">
                {label}
                {isPresenter && <span className="meeting-video-tile__badge">발표자</span>}
            </div>
        </div>
    );
};

interface MeetingVideoStageProps {
    localStream: MediaStream | null;
    localLabel: string;
    localIsPresenter: boolean;
    remoteVideoStreams: Array<{
        id: string;
        stream: MediaStream;
        label: string;
        isPresenter?: boolean;
    }>;
}

const MeetingVideoStage: React.FC<MeetingVideoStageProps> = ({
    localStream,
    localLabel,
    localIsPresenter,
    remoteVideoStreams,
}) => {
    return (
        <div className="meeting-video-stage">
            {localStream && (
                <VideoTile stream={localStream} label={`${localLabel} (나)`} isPresenter={localIsPresenter} isLocal />
            )}
            {remoteVideoStreams.map((item) => (
                <VideoTile
                    key={item.id}
                    stream={item.stream}
                    label={item.label}
                    isPresenter={item.isPresenter}
                />
            ))}
            {!localStream && remoteVideoStreams.length === 0 && (
                <div className="meeting-video-empty">미디어 스트림이 없습니다.</div>
            )}
        </div>
    );
};

export default MeetingVideoStage;
