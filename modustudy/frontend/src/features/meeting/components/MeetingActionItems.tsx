import React, { useState } from 'react';
import {
    ActionItemStatus,
    MeetingActionItemRequest,
    MeetingActionItemResponse,
    MeetingParticipantResponse,
} from '../types';
import '../styles/MeetingDetail.css';

interface MeetingActionItemsProps {
    actionItems: MeetingActionItemResponse[];
    participants: MeetingParticipantResponse[];
    onAdd: (payload: MeetingActionItemRequest) => void;
    onUpdate: (id: number, payload: MeetingActionItemRequest) => void;
}

const MeetingActionItems: React.FC<MeetingActionItemsProps> = ({ actionItems, participants, onAdd, onUpdate }) => {
    const [newContent, setNewContent] = useState('');
    const [newAssignee, setNewAssignee] = useState<string>('');

    const handleAdd = () => {
        const trimmed = newContent.trim();
        if (!trimmed) return;
        onAdd({
            content: trimmed,
            assigneeId: newAssignee ? Number(newAssignee) : null,
            status: 'TODO',
        });
        setNewContent('');
        setNewAssignee('');
    };

    const handleUpdate = (item: MeetingActionItemResponse, updates: Partial<MeetingActionItemRequest>) => {
        onUpdate(item.id, {
            content: updates.content ?? item.content,
            assigneeId: updates.assigneeId ?? item.assigneeId,
            status: (updates.status ?? item.status) as ActionItemStatus,
        });
    };

    return (
        <section className="meeting-detail-card">
            <div className="meeting-detail-card__header">
                <h3>액션 아이템</h3>
            </div>
            <div className="meeting-detail-card__body">
                <div className="meeting-action-add">
                    <input
                        type="text"
                        value={newContent}
                        onChange={(event) => setNewContent(event.target.value)}
                        placeholder="해야 할 일을 입력하세요"
                    />
                    <select value={newAssignee} onChange={(event) => setNewAssignee(event.target.value)}>
                        <option value="">담당자 선택</option>
                        {participants.map((participant) => (
                            <option key={participant.userId} value={participant.userId}>
                                {participant.nickname}
                            </option>
                        ))}
                    </select>
                    <button type="button" onClick={handleAdd}>
                        추가
                    </button>
                </div>

                <div className="meeting-action-list">
                    {actionItems.map((item) => (
                        <div key={item.id} className={`meeting-action-item ${item.status === 'DONE' ? 'done' : ''}`}>
                            <input
                                type="text"
                                value={item.content}
                                onChange={(event) => handleUpdate(item, { content: event.target.value })}
                            />
                            <select
                                value={item.assigneeId ?? ''}
                                onChange={(event) =>
                                    handleUpdate(item, {
                                        assigneeId: event.target.value ? Number(event.target.value) : null,
                                    })
                                }
                            >
                                <option value="">담당자 없음</option>
                                {participants.map((participant) => (
                                    <option key={participant.userId} value={participant.userId}>
                                        {participant.nickname}
                                    </option>
                                ))}
                            </select>
                            <select
                                value={item.status}
                                onChange={(event) => handleUpdate(item, { status: event.target.value as ActionItemStatus })}
                            >
                                <option value="TODO">TODO</option>
                                <option value="DONE">DONE</option>
                            </select>
                        </div>
                    ))}
                    {actionItems.length === 0 && <p className="meeting-detail-empty">등록된 액션 아이템이 없습니다.</p>}
                </div>
            </div>
        </section>
    );
};

export default MeetingActionItems;
