import { useState } from 'react';
import { useCalendarStore } from '../services/calendarStore';
import { UnifiedSchedule } from '../types';
import { getTodayString, getCurrentTimeString } from '../utils';
import { CreatePersonalScheduleRequest, UpdatePersonalScheduleRequest } from '@/api/endpoints/calendarApi';
import { useUIStore } from '@/store/uiStore';
import { getErrorMessage } from '@/shared/utils/errorUtils';

/**
 * 일정 폼 관리 훅
 */
export const useScheduleForm = (initialSchedule?: UnifiedSchedule) => {
    const { createSchedule, updateSchedule } = useCalendarStore();
    const { showToast } = useUIStore();

    const [formData, setFormData] = useState<CreatePersonalScheduleRequest>({
        title: initialSchedule?.title || '',
        description: initialSchedule?.description || '',
        startDate: initialSchedule?.startDate || getTodayString(),
        startTime: initialSchedule?.startTime || getCurrentTimeString(),
        endDate: initialSchedule?.endDate || initialSchedule?.startDate || getTodayString(),
        endTime: initialSchedule?.endTime || '',
        location: initialSchedule?.location || '',
        isOnline: initialSchedule?.isOnline ?? false,
        color: initialSchedule?.color || '#4285F4',
        syncToGoogle: initialSchedule?.isSyncedWithGoogle ?? true
    });

    const [errors, setErrors] = useState<Record<string, string>>({});
    const [submitting, setSubmitting] = useState(false);

    // 폼 필드 변경
    const handleChange = (field: keyof CreatePersonalScheduleRequest, value: any) => {
        setFormData(prev => ({ ...prev, [field]: value }));
        // 에러 클리어
        if (errors[field]) {
            setErrors(prev => {
                const newErrors = { ...prev };
                delete newErrors[field];
                return newErrors;
            });
        }
    };

    // 폼 검증
    const validate = (): boolean => {
        const newErrors: Record<string, string> = {};

        if (!formData.title.trim()) {
            newErrors.title = '제목을 입력해주세요.';
        } else if (formData.title.length > 200) {
            newErrors.title = '제목은 200자 이내로 입력해주세요.';
        }

        if (!formData.startDate) {
            newErrors.startDate = '시작 날짜를 선택해주세요.';
        }

        // 종료일이 시작일보다 빠른 경우
        if (formData.endDate && formData.startDate > formData.endDate) {
            newErrors.endDate = '종료일은 시작일 이후여야 합니다.';
        }

        // 시간 형식 검증 (HH:mm)
        const timeRegex = /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/;
        if (formData.startTime && !timeRegex.test(formData.startTime)) {
            newErrors.startTime = '올바른 시간 형식이 아닙니다. (HH:mm)';
        }
        if (formData.endTime && !timeRegex.test(formData.endTime)) {
            newErrors.endTime = '올바른 시간 형식이 아닙니다. (HH:mm)';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    // 생성
    const handleCreate = async () => {
        if (!validate()) return false;

        setSubmitting(true);
        try {
            await createSchedule(formData);
            showToast('일정이 생성되었습니다.', 'success');
            return true;
        } catch (error: unknown) {
            showToast(getErrorMessage(error, '일정 생성에 실패했습니다.'), 'error');
            return false;
        } finally {
            setSubmitting(false);
        }
    };

    // 수정
    const handleUpdate = async (scheduleId: number) => {
        if (!validate()) return false;

        setSubmitting(true);
        try {
            const updateData: UpdatePersonalScheduleRequest = { ...formData };
            await updateSchedule(scheduleId, updateData);
            showToast('일정이 수정되었습니다.', 'success');
            return true;
        } catch (error: unknown) {
            showToast(getErrorMessage(error, '일정 수정에 실패했습니다.'), 'error');
            return false;
        } finally {
            setSubmitting(false);
        }
    };

    // 폼 리셋
    const reset = () => {
        setFormData({
            title: '',
            description: '',
            startDate: getTodayString(),
            startTime: getCurrentTimeString(),
            endDate: getTodayString(),
            endTime: '',
            location: '',
            isOnline: false,
            color: '#4285F4',
            syncToGoogle: true
        });
        setErrors({});
    };

    return {
        formData,
        errors,
        submitting,
        handleChange,
        handleCreate,
        handleUpdate,
        reset
    };
};

/**
 * 일정 삭제 훅
 */
export const useDeleteSchedule = () => {
    const { deleteSchedule } = useCalendarStore();
    const { showToast } = useUIStore();
    const [deleting, setDeleting] = useState(false);

    const handleDelete = async (scheduleId: number) => {
        if (!confirm('정말 삭제하시겠습니까?')) return false;

        setDeleting(true);
        try {
            await deleteSchedule(scheduleId);
            showToast('일정이 삭제되었습니다.', 'success');
            return true;
        } catch (error: unknown) {
            showToast(getErrorMessage(error, '일정 삭제에 실패했습니다.'), 'error');
            return false;
        } finally {
            setDeleting(false);
        }
    };

    return {
        deleting,
        handleDelete
    };
};

/**
 * 빠른 일정 추가 훅 (날짜 클릭 시)
 */
export const useQuickAddSchedule = () => {
    const { createSchedule } = useCalendarStore();
    const { showToast } = useUIStore();

    const quickAdd = async (date: string, title: string = '새 일정') => {
        try {
            await createSchedule({
                title,
                startDate: date,
                isOnline: false
            });
            showToast('일정이 추가되었습니다.', 'success');
            return true;
        } catch (error: unknown) {
            showToast(getErrorMessage(error, '일정 추가에 실패했습니다.'), 'error');
            return false;
        }
    };

    return { quickAdd };
};
