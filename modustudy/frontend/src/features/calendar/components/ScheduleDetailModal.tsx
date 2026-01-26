import { Modal, Button } from '@/shared/components';
import { UnifiedSchedule } from '../types';
import { useDeleteSchedule } from '../hooks';
import { MapPin, Clock, Calendar as CalendarIcon, Edit2, Trash2 } from 'lucide-react';

interface ScheduleDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    schedule: UnifiedSchedule | null;
    onEdit?: (schedule: UnifiedSchedule) => void;
}

/**
 * 일정 상세 보기 모달
 */
export const ScheduleDetailModal = ({
    isOpen,
    onClose,
    schedule,
    onEdit
}: ScheduleDetailModalProps) => {
    const { deleting, handleDelete } = useDeleteSchedule();

    if (!schedule) return null;

    // 소스별 라벨
    const getSourceLabel = (source: string) => {
        const labels = {
            personal: '개인 일정',
            study: '스터디 세션',
            google: 'Google Calendar'
        };
        return labels[source as keyof typeof labels] || source;
    };

    // 소스별 색상
    const getSourceColor = (source: string) => {
        const colors = {
            personal: 'bg-blue-100 text-blue-700',
            study: 'bg-green-100 text-green-700',
            google: 'bg-red-100 text-red-700'
        };
        return colors[source as keyof typeof colors] || 'bg-gray-100 text-gray-700';
    };

    // 수정 버튼 (개인 일정만)
    const handleEditClick = () => {
        if (schedule.source === 'personal' && onEdit) {
            onEdit(schedule);
            onClose();
        }
    };

    // 삭제 버튼 (개인 일정만)
    const handleDeleteClick = async () => {
        if (schedule.source === 'personal') {
            const success = await handleDelete(schedule.id as number);
            if (success) {
                onClose();
            }
        }
    };

    // 개인 일정인지 확인
    const isPersonalSchedule = schedule.source === 'personal';

    return (
        <Modal isOpen={isOpen} onClose={onClose} maxWidth="lg">
            <div className="p-6">
                {/* 헤더 */}
                <div className="mb-6">
                    <div className="flex items-center gap-2 mb-2">
                        <span className={`text-xs font-semibold px-3 py-1 rounded-full ${getSourceColor(schedule.source)}`}>
                            {getSourceLabel(schedule.source)}
                        </span>
                        {schedule.status && (
                            <span className="text-xs font-semibold px-3 py-1 rounded-full bg-gray-100 text-gray-700">
                                {schedule.status}
                            </span>
                        )}
                    </div>
                    <h2 className="text-2xl font-bold text-gray-900">
                        {schedule.title}
                    </h2>
                </div>

                {/* 내용 */}
                <div className="space-y-4">
                    {/* 설명 */}
                    {schedule.description && (
                        <div>
                            <p className="text-gray-700 whitespace-pre-wrap">
                                {schedule.description}
                            </p>
                        </div>
                    )}

                    {/* 정보 리스트 */}
                    <div className="space-y-3 py-4 border-t">
                        {/* 날짜 */}
                        <div className="flex items-start gap-3">
                            <CalendarIcon size={20} className="text-gray-400 mt-0.5" />
                            <div>
                                <p className="text-sm font-medium text-gray-500">날짜</p>
                                <p className="text-gray-900">
                                    {schedule.startDate}
                                    {schedule.endDate && schedule.endDate !== schedule.startDate &&
                                        ` ~ ${schedule.endDate}`
                                    }
                                </p>
                            </div>
                        </div>

                        {/* 시간 */}
                        {(schedule.startTime || schedule.durationMinutes) && (
                            <div className="flex items-start gap-3">
                                <Clock size={20} className="text-gray-400 mt-0.5" />
                                <div>
                                    <p className="text-sm font-medium text-gray-500">시간</p>
                                    <p className="text-gray-900">
                                        {schedule.startTime && `${schedule.startTime}`}
                                        {schedule.endTime && ` ~ ${schedule.endTime}`}
                                        {schedule.durationMinutes && ` (${schedule.durationMinutes}분)`}
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* 장소 */}
                        {schedule.location && (
                            <div className="flex items-start gap-3">
                                <MapPin size={20} className="text-gray-400 mt-0.5" />
                                <div>
                                    <p className="text-sm font-medium text-gray-500">장소</p>
                                    <p className="text-gray-900">
                                        {schedule.location}
                                        {schedule.isOnline && (
                                            <span className="ml-2 text-sm text-blue-600">(온라인)</span>
                                        )}
                                    </p>
                                </div>
                            </div>
                        )}

                        {/* 스터디 세션 정보 */}
                        {schedule.source === 'study' && schedule.sessionNumber && (
                            <div className="p-4 bg-green-50 rounded-lg">
                                <p className="text-sm text-green-800">
                                    📚 스터디 세션 <strong>{schedule.sessionNumber}회차</strong>
                                </p>
                            </div>
                        )}
                    </div>
                </div>

                {/* 버튼 */}
                <div className="flex items-center justify-end gap-3 mt-6 pt-4 border-t">
                    <Button
                        variant="outline"
                        onClick={onClose}
                    >
                        닫기
                    </Button>
                    {isPersonalSchedule && (
                        <>
                            <Button
                                variant="outline"
                                onClick={handleDeleteClick}
                                isLoading={deleting}
                                leftIcon={<Trash2 size={18} />}
                                className="text-red-600 border-red-300 hover:bg-red-50"
                            >
                                삭제
                            </Button>
                            <Button
                                variant="primary"
                                onClick={handleEditClick}
                                leftIcon={<Edit2 size={18} />}
                            >
                                수정
                            </Button>
                        </>
                    )}
                </div>
            </div>
        </Modal>
    );
};
