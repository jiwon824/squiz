import React, { useEffect, useState } from 'react';
import { cn } from '@/shared/utils/cn';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { MeetingRequestPayload, MeetingType } from '../types';
import { Clock } from 'lucide-react';

interface MeetingStartModalProps {
    open: boolean;
    initialTitle?: string;
    onClose: () => void;
    onStart: (payload: MeetingRequestPayload) => void;
}

const durationOptions = [
    { label: '2분', sublabel: '테스트', value: 120 },
    { label: '6분', sublabel: '테스트', value: 360 },
    { label: '1시간', sublabel: '', value: 3600 },
    { label: '1시간 30분', sublabel: '', value: 5400 },
    { label: '2시간', sublabel: '', value: 7200 },
    { label: '2시간 30분', sublabel: '', value: 9000 },
    { label: '3시간', sublabel: '', value: 10800 },
];

const MeetingStartModal: React.FC<MeetingStartModalProps> = ({ open, initialTitle, onClose, onStart }) => {
    const [title, setTitle] = useState(initialTitle ?? '');
    const [plannedDurationSeconds, setPlannedDurationSeconds] = useState(3600);
    const meetingType: MeetingType = 'DAILY';
    const autoShareSummary = false;
    const shareWorkspaceId = null;

    useEffect(() => {
        if (open) {
            setTitle(initialTitle ?? '');
            setPlannedDurationSeconds(3600);
        }
    }, [open, initialTitle]);

    const handleSubmit = () => {
        onStart({
            title: title.trim() || '새 미팅',
            meetingType,
            autoShareSummary,
            shareWorkspaceId,
            plannedDurationSeconds,
        });
    };

    return (
        <Modal isOpen={open} onClose={onClose} title="새 미팅 시작" maxWidth="md">
            <div className="flex flex-col gap-6">
                {/* 미팅 제목 */}
                <div className="flex flex-col gap-2">
                    <label className="text-sm font-medium text-gray-700">미팅 제목</label>
                    <input
                        type="text"
                        className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500/30 focus:border-blue-400 transition-all"
                        value={title}
                        onChange={(event) => setTitle(event.target.value)}
                        placeholder="미팅 제목을 입력하세요"
                    />
                </div>

                {/* 미팅 시간 */}
                <div className="flex flex-col gap-2">
                    <label className="text-sm font-medium text-gray-700 flex items-center gap-1.5">
                        <Clock size={14} className="text-gray-400" />
                        미팅 시간
                    </label>
                    <div className="grid grid-cols-4 gap-2">
                        {durationOptions.map((option) => (
                            <button
                                key={option.value}
                                type="button"
                                className={cn(
                                    'flex flex-col items-center gap-0.5 px-3 py-2.5 rounded-xl text-sm font-medium transition-all cursor-pointer',
                                    plannedDurationSeconds === option.value
                                        ? 'bg-blue-600 text-white shadow-sm'
                                        : 'bg-gray-50 text-gray-600 hover:bg-gray-100 border border-gray-200'
                                )}
                                onClick={() => setPlannedDurationSeconds(option.value)}
                            >
                                <span>{option.label}</span>
                                {option.sublabel && (
                                    <span className={cn(
                                        'text-[10px]',
                                        plannedDurationSeconds === option.value ? 'text-blue-200' : 'text-gray-400'
                                    )}>
                                        {option.sublabel}
                                    </span>
                                )}
                            </button>
                        ))}
                    </div>
                </div>

                {/* 버튼 */}
                <div className="flex justify-end gap-3 pt-2">
                    <Button variant="ghost" size="sm" onClick={onClose}>
                        취소
                    </Button>
                    <Button variant="primary" size="sm" onClick={handleSubmit}>
                        시작하기
                    </Button>
                </div>
            </div>
        </Modal>
    );
};

export default MeetingStartModal;
