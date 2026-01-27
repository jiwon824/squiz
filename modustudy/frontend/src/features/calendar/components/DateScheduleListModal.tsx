import { Modal, Button } from '@/shared/components';
import { UnifiedSchedule } from '../types';
import { Calendar as CalendarIcon, MapPin, Clock } from 'lucide-react';

interface DateScheduleListModalProps {
    isOpen: boolean;
    onClose: () => void;
    date: string;
    schedules: UnifiedSchedule[];
    onScheduleClick: (schedule: UnifiedSchedule) => void;
    onAddClick: () => void;
}

/**
 * 특정 날짜의 일정 리스트 모달
 */
export const DateScheduleListModal = ({
    isOpen,
    onClose,
    date,
    schedules,
    onScheduleClick,
    onAddClick
}: DateScheduleListModalProps) => {
    if (!isOpen) return null;

    // 날짜 포맷팅 (YYYY-MM-DD → 2026년 1월 26일)
    const formatDateDisplay = (dateStr: string) => {
        const d = new Date(dateStr);
        return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
    };

    // 요일
    const getDayOfWeek = (dateStr: string) => {
        const days = ['일', '월', '화', '수', '목', '금', '토'];
        return days[new Date(dateStr).getDay()];
    };

    // 소스별 라벨
    const getSourceLabel = (source: string) => {
        const labels = {
            personal: '개인',
            study: '스터디',
            google: 'Google'
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

    return (
        <Modal isOpen={isOpen} onClose={onClose} maxWidth="md">
            <div className="p-6">
                {/* 헤더 */}
                <div className="mb-6">
                    <h2 className="text-2xl font-bold text-gray-900 mb-1">
                        {formatDateDisplay(date)}
                    </h2>
                    <p className="text-sm text-gray-500">
                        {getDayOfWeek(date)}요일
                    </p>
                </div>

                {/* 일정 추가 버튼 */}
                <Button
                    variant="primary"
                    fullWidth
                    className="mb-4"
                    leftIcon={<CalendarIcon size={18} />}
                    onClick={() => {
                        onAddClick();
                        onClose();
                    }}
                >
                    이 날짜에 일정 추가
                </Button>

                {/* 일정 리스트 */}
                {schedules.length === 0 ? (
                    <div className="py-12 text-center">
                        <CalendarIcon size={48} className="mx-auto text-gray-300 mb-3" />
                        <p className="text-gray-500 mb-2">일정이 없습니다</p>
                        <p className="text-sm text-gray-400">위 버튼을 눌러 일정을 추가해보세요</p>
                    </div>
                ) : (
                    <div className="space-y-3 max-h-[400px] overflow-y-auto">
                        {schedules.map((schedule) => (
                            <div
                                key={schedule.id}
                                onClick={() => {
                                    onScheduleClick(schedule);
                                    onClose();
                                }}
                                className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
                            >
                                <div className="flex items-start justify-between mb-2">
                                    <h3 className="font-semibold text-gray-900">
                                        {schedule.title}
                                    </h3>
                                    <span className={`text-xs font-semibold px-2 py-1 rounded-full ${getSourceColor(schedule.source)}`}>
                                        {getSourceLabel(schedule.source)}
                                    </span>
                                </div>

                                {/* 시간 정보 */}
                                {(schedule.startTime || schedule.endTime) && (
                                    <div className="flex items-center gap-2 text-sm text-gray-600 mb-2">
                                        <Clock size={14} />
                                        <span>
                                            {schedule.startTime}
                                            {schedule.endTime && ` ~ ${schedule.endTime}`}
                                        </span>
                                    </div>
                                )}

                                {/* 장소 정보 */}
                                {schedule.location && (
                                    <div className="flex items-center gap-2 text-sm text-gray-600">
                                        <MapPin size={14} />
                                        <span>{schedule.location}</span>
                                        {schedule.isOnline && (
                                            <span className="text-xs text-blue-600">(온라인)</span>
                                        )}
                                    </div>
                                )}

                                {/* 설명 */}
                                {schedule.description && (
                                    <p className="text-sm text-gray-500 mt-2 line-clamp-2">
                                        {schedule.description}
                                    </p>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {/* 닫기 버튼 */}
                <div className="flex justify-end mt-6 pt-4 border-t">
                    <Button variant="outline" onClick={onClose}>
                        닫기
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
