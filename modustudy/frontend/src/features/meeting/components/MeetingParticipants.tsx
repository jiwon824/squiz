import React from 'react';
import { MeetingRoomParticipant } from '../types';
import '../styles/MeetingRoom.css';

interface MeetingParticipantsProps {
    participants: MeetingRoomParticipant[];
    presenterId: number | null;
    presenterName: string | null;
}

const MeetingParticipants: React.FC<MeetingParticipantsProps> = ({ participants, presenterId, presenterName }) => {
    return (
        <section className="meeting-panel">
            <div className="meeting-panel__header">
                <h3>참가자</h3>
                <span className="meeting-panel__count">{participants.length}</span>
            </div>
            <div className="meeting-panel__body">
                {participants.map((participant) => {
                    const isPresenter =
                        (presenterId !== null && participant.id === presenterId) ||
                        (presenterName !== null && participant.displayName === presenterName);
                    const presenceLabel = participant.isPresent === false ? '자리 없음' : '자리 있음';
                    const presenceIcon = participant.isPresent === false ? '🙅' : '👨‍💻';
                    const presenterLabel = '발표 중';
                    const presenterIcon = '🎤';
                    return (
                        <div
                            key={participant.id}
                            className={`meeting-participant ${participant.isSpeaking ? 'speaking' : ''}`}
                        >
                            <div className="meeting-participant__info">
                                <span className="meeting-participant__name">
                                    {participant.displayName}
                                    {isPresenter && <span className="meeting-participant__badge">발표자</span>}
                                </span>
                                {isPresenter ? (
                                    <span className="meeting-participant__presence">
                                        <span>{presenterIcon}</span>
                                        {presenterLabel}
                                    </span>
                                ) : (
                                    <span className="meeting-participant__presence">
                                        <span>{presenceIcon}</span>
                                        {presenceLabel}
                                    </span>
                                )}
                            </div>
                            <div className={`meeting-participant__status ${participant.active ? 'active' : ''}`}>
                                {participant.active ? '접속' : '퇴장'}
                            </div>
                        </div>
                    );
                })}
                {participants.length === 0 && <p className="meeting-panel__empty">참가자가 없습니다.</p>}
            </div>
        </section>
    );
};

export default MeetingParticipants;
