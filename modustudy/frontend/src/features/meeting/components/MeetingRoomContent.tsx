import React from 'react';
import MeetingParticipants from './MeetingParticipants';
import MeetingChatPanel from './MeetingChatPanel';
import MeetingVideoStage from './MeetingVideoStage';
import { MeetingRoomChatMessage, MeetingRoomParticipant } from '../types';

interface RemoteVideoStream {
    id: string;
    stream: MediaStream;
    label: string;
    isPresenter: boolean;
}

interface MeetingRoomContentProps {
    localStream: MediaStream | null;
    localLabel: string;
    localIsPresenter: boolean;
    remoteVideoStreams: RemoteVideoStream[];
    videoStageRef: React.RefObject<HTMLDivElement>;
    aiVideoRef: React.RefObject<HTMLVideoElement>;
    participants: MeetingRoomParticipant[];
    presenterId: number | null;
    presenterName: string | null;
    chatMessages: MeetingRoomChatMessage[];
    onSendChat: (content: string) => void;
    onDeleteChat: (messageId: number) => void;
    currentUserId: number | null;
    currentSender: string;
}

const MeetingRoomContent: React.FC<MeetingRoomContentProps> = ({
    localStream,
    localLabel,
    localIsPresenter,
    remoteVideoStreams,
    videoStageRef,
    aiVideoRef,
    participants,
    presenterId,
    presenterName,
    chatMessages,
    onSendChat,
    onDeleteChat,
    currentUserId,
    currentSender,
}) => {
    return (
        <div className="meeting-room__content">
            <div className="meeting-room__stage">
                <MeetingVideoStage
                    localStream={localStream}
                    localLabel={localLabel}
                    localIsPresenter={localIsPresenter}
                    containerRef={videoStageRef}
                    remoteVideoStreams={remoteVideoStreams}
                />
                <video ref={aiVideoRef} className="meeting-room__hidden-video" muted playsInline />
            </div>
            <div className="meeting-room__side">
                <MeetingParticipants
                    participants={participants}
                    presenterId={presenterId}
                    presenterName={presenterName}
                />
                <MeetingChatPanel
                    messages={chatMessages}
                    onSend={onSendChat}
                    onDelete={onDeleteChat}
                    currentUserId={currentUserId}
                    currentSender={currentSender}
                />
            </div>
        </div>
    );
};

export default MeetingRoomContent;
