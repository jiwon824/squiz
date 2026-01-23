import React, { useEffect, useRef, useState } from 'react';
import { MeetingRoomChatMessage } from '../types';
import '../styles/MeetingRoom.css';

interface MeetingChatPanelProps {
    messages: MeetingRoomChatMessage[];
    onSend: (text: string) => void;
    onDelete: (message: MeetingRoomChatMessage) => void;
    currentUserId?: number | null;
    currentSender: string;
}

const MeetingChatPanel: React.FC<MeetingChatPanelProps> = ({
    messages,
    onSend,
    onDelete,
    currentUserId,
    currentSender,
}) => {
    const [text, setText] = useState('');
    const messagesRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        if (!messagesRef.current) return;
        messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
    }, [messages.length]);

    const handleSubmit = (event: React.FormEvent) => {
        event.preventDefault();
        const trimmed = text.trim();
        if (!trimmed) return;
        onSend(trimmed);
        setText('');
    };

    return (
        <section className="meeting-panel meeting-panel--chat">
            <div className="meeting-panel__header">
                <h3>채팅</h3>
            </div>
            <div className="meeting-chat">
                <div className="meeting-chat__messages" ref={messagesRef}>
                    {messages.map((message, index) => {
                        const isOwn =
                            (currentUserId !== null &&
                                currentUserId !== undefined &&
                                message.userId !== null &&
                                message.userId === currentUserId) ||
                            message.sender === currentSender;
                        return (
                            <div key={`${message.sentAt}-${index}`} className="meeting-chat__message">
                                <div className="meeting-chat__meta">
                                    <span
                                        className={`meeting-chat__sender ${isOwn ? 'meeting-chat__sender--own' : ''}`}
                                    >
                                        {message.sender}
                                    </span>
                                    <div className="meeting-chat__actions">
                                        <span className="meeting-chat__time">
                                            {new Date(message.sentAt).toLocaleTimeString()}
                                        </span>
                                        {isOwn && (
                                            <button
                                                type="button"
                                                className="meeting-chat__delete"
                                                onClick={() => onDelete(message)}
                                            >
                                                삭제
                                            </button>
                                        )}
                                    </div>
                                </div>
                                <p className="meeting-chat__text">{message.text}</p>
                            </div>
                        );
                    })}
                    {messages.length === 0 && <p className="meeting-panel__empty">아직 채팅이 없습니다.</p>}
                </div>
                <form className="meeting-chat__input" onSubmit={handleSubmit}>
                    <input
                        type="text"
                        value={text}
                        onChange={(event) => setText(event.target.value)}
                        placeholder="메시지를 입력하세요"
                    />
                    <button type="submit">전송</button>
                </form>
            </div>
        </section>
    );
};

export default MeetingChatPanel;
