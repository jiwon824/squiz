import { useEffect } from 'react';
import { Modal, Button, Input } from '@/shared/components';
import { UnifiedSchedule } from '../types';
import { useScheduleForm } from '../hooks';

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
                <div className="space-y-4">
                    {/* 제목 */}
                    <Input
                        label="제목 *"
                        type="text"
                        value={formData.title}
                        onChange={(e) => handleChange('title', e.target.value)}
                        error={errors.title}
                        placeholder="일정 제목을 입력하세요"
                    />

                    {/* 설명 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            설명
                        </label>
                        <textarea
                            value={formData.description || ''}
                            onChange={(e) => handleChange('description', e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                            rows={3}
                            placeholder="일정 설명을 입력하세요"
                        />
                    </div>

                    {/* 날짜 및 시간 */}
                    <div className="grid grid-cols-2 gap-4">
                        <Input
                            label="시작 날짜 *"
                            type="date"
                            value={formData.startDate}
                            onChange={(e) => handleChange('startDate', e.target.value)}
                            error={errors.startDate}
                        />
                        <Input
                            label="시작 시간"
                            type="time"
                            value={formData.startTime || ''}
                            onChange={(e) => handleChange('startTime', e.target.value)}
                            error={errors.startTime}
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <Input
                            label="종료 날짜"
                            type="date"
                            value={formData.endDate || ''}
                            onChange={(e) => handleChange('endDate', e.target.value)}
                            error={errors.endDate}
                        />
                        <Input
                            label="종료 시간"
                            type="time"
                            value={formData.endTime || ''}
                            onChange={(e) => handleChange('endTime', e.target.value)}
                            error={errors.endTime}
                        />
                    </div>

                    {/* 장소 */}
                    <Input
                        label="장소"
                        type="text"
                        value={formData.location || ''}
                        onChange={(e) => handleChange('location', e.target.value)}
                        placeholder="장소를 입력하세요"
                    />

                    {/* 온라인 여부 */}
                    <div className="flex items-center gap-3">
                        <input
                            type="checkbox"
                            id="isOnline"
                            checked={formData.isOnline}
                            onChange={(e) => handleChange('isOnline', e.target.checked)}
                            className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
                        />
                        <label htmlFor="isOnline" className="text-sm font-medium text-gray-700">
                            온라인 일정
                        </label>
                    </div>

                    {/* 색상 */}
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                            색상
                        </label>
                        <div className="flex items-center gap-3">
                            <input
                                type="color"
                                value={formData.color || '#4285F4'}
                                onChange={(e) => handleChange('color', e.target.value)}
                                className="w-12 h-12 rounded-lg border border-gray-300 cursor-pointer"
                            />
                            <span className="text-sm text-gray-600">
                                {formData.color || '#4285F4'}
                            </span>
                        </div>
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
