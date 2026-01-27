import { useEffect } from 'react';
import { Modal, Button, Input, DatePicker, TimePicker } from '@/shared/components';
import { UnifiedSchedule } from '../types';
import { useScheduleForm } from '../hooks';
import {
    Tag, AlignLeft, Calendar as CalendarIcon, Clock, MapPin
} from 'lucide-react';

interface ScheduleModalProps {
    isOpen: boolean;
    onClose: () => void;
    schedule?: UnifiedSchedule | null;
    initialDate?: string;
}

/**
 * 일정 추가/편집 모달
 */
export const ScheduleModal = ({ isOpen, onClose, schedule, initialDate }: ScheduleModalProps) => {
    const isEditMode = !!schedule;
    const {
        formData,
        errors,
        submitting,
        handleChange,
        handleCreate,
        handleUpdate,
        reset
    } = useScheduleForm(schedule || undefined);

    // 모달 열릴 때 초기 날짜 설정
    useEffect(() => {
        if (isOpen && initialDate && !schedule) {
            handleChange('startDate', initialDate);
        }
    }, [isOpen, initialDate]);

    // 저장 핸들러
    const handleSave = async () => {
        let success = false;
        if (isEditMode && schedule) {
            success = await handleUpdate(schedule.id as number);
        } else {
            success = await handleCreate();
        }

        if (success) {
            reset();
            onClose();
        }
    };

    // 모달 닫기
    const handleClose = () => {
        reset();
        onClose();
    };

    return (
        <Modal isOpen={isOpen} onClose={handleClose} maxWidth="2xl">
            <div className="p-6">
                {/* 헤더 */}
                <div className="mb-6">
                    <h2 className="text-2xl font-bold text-gray-900">
                        {isEditMode ? '일정 수정' : '일정 추가'}
                    </h2>
                </div>

                {/* 폼 */}
                <div className="space-y-6">
                    {/* 제목 */}
                    <div className="space-y-1.5">
                        <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                            <Tag size={16} className="text-blue-500" />
                            제목 *
                        </label>
                        <Input
                            type="text"
                            value={formData.title}
                            onChange={(e) => handleChange('title', e.target.value)}
                            error={errors.title}
                            placeholder="예: 알고리즘 문제 풀이, 프로젝트 회의"
                            className="text-base"
                        />
                    </div>

                    {/* 설명 */}
                    <div className="space-y-1.5">
                        <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                            <AlignLeft size={16} className="text-blue-500" />
                            설명
                        </label>
                        <textarea
                            value={formData.description || ''}
                            onChange={(e) => handleChange('description', e.target.value)}
                            className="w-full p-3.5 bg-gray-50 border border-gray-200 rounded-2xl focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none resize-none min-h-[100px]"
                            rows={3}
                            placeholder="일정에 대한 상세 내용을 입력하세요"
                        />
                    </div>

                    {/* 날짜 및 시간 */}
                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1.5">
                            <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                                <CalendarIcon size={16} className="text-blue-500" />
                                시작 날짜 *
                            </label>
                            <DatePicker
                                value={formData.startDate}
                                onChange={(date) => handleChange('startDate', date)}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                                <Clock size={16} className="text-blue-500" />
                                시작 시간
                            </label>
                            <TimePicker
                                value={formData.startTime || ''}
                                onChange={(time) => handleChange('startTime', time)}
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div className="space-y-1.5">
                            <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                                <CalendarIcon size={16} className="text-gray-400" />
                                종료 날짜
                            </label>
                            <DatePicker
                                value={formData.endDate || ''}
                                onChange={(date) => handleChange('endDate', date)}
                            />
                        </div>
                        <div className="space-y-1.5">
                            <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                                <Clock size={16} className="text-gray-400" />
                                종료 시간
                            </label>
                            <TimePicker
                                value={formData.endTime || ''}
                                onChange={(time) => handleChange('endTime', time)}
                            />
                        </div>
                    </div>

                    {/* 장소 */}
                    <div className="space-y-1.5">
                        <label className="flex items-center gap-2 text-sm font-bold text-gray-700 ml-1">
                            <MapPin size={16} className="text-blue-500" />
                            장소
                        </label>
                        <Input
                            type="text"
                            value={formData.location || ''}
                            onChange={(e) => handleChange('location', e.target.value)}
                            placeholder="오프라인 장소 또는 링크를 입력하세요"
                        />
                    </div>
                </div>

                {/* 버튼 */}
                <div className="flex items-center justify-end gap-3 mt-8 pt-4 border-t">
                    <Button
                        variant="outline"
                        onClick={handleClose}
                        disabled={submitting}
                    >
                        취소
                    </Button>
                    <Button
                        variant="primary"
                        onClick={handleSave}
                        isLoading={submitting}
                    >
                        {isEditMode ? '수정' : '추가'}
                    </Button>
                </div>
            </div>
        </Modal>
    );
};
