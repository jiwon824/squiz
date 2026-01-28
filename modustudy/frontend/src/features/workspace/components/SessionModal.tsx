import { useState, useEffect } from 'react';
import { Modal, Button, Input, DatePicker, TimePicker } from '@/shared/components';
import { sessionApi, type StudySessionResponse, type SessionCreateRequest } from '@/api/endpoints/sessionApi';
import { useUIStore } from '@/store/uiStore';
import {
  Tag,
  AlignLeft,
  Calendar as CalendarIcon,
  Clock,
  MapPin,
  Globe,
  Monitor,
} from 'lucide-react';

interface SessionModalProps {
  isOpen: boolean;
  onClose: () => void;
  studyId: number;
  session?: StudySessionResponse | null;
  initialDate?: string;
  onSuccess?: () => void;
}

/**
 * 스터디 세션 생성/편집 모달
 */
export const SessionModal: React.FC<SessionModalProps> = ({
  isOpen,
  onClose,
  studyId,
  session,
  initialDate,
  onSuccess,
}) => {
  const showToast = useUIStore((state) => state.showToast);
  const isEditMode = !!session;

  // 폼 상태
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [durationMinutes, setDurationMinutes] = useState<number | ''>('');
  const [location, setLocation] = useState('');
  const [isOnline, setIsOnline] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // 모달 열릴 때 초기값 설정
  useEffect(() => {
    if (isOpen) {
      if (session) {
        // 편집 모드: 기존 세션 데이터로 초기화
        setTitle(session.title || '');
        setDescription(session.description || '');
        // scheduledAt에서 날짜와 시간 분리
        if (session.scheduledAt) {
          const dt = new Date(session.scheduledAt);
          setDate(dt.toISOString().split('T')[0]);
          setTime(dt.toTimeString().slice(0, 5));
        }
        setDurationMinutes(session.durationMinutes || '');
        setLocation(session.location || '');
        setIsOnline(session.isOnline ?? true);
      } else {
        // 생성 모드: 초기값
        setTitle('');
        setDescription('');
        setDate(initialDate || new Date().toISOString().split('T')[0]);
        setTime('19:00');
        setDurationMinutes(60);
        setLocation('');
        setIsOnline(true);
      }
      setErrors({});
    }
  }, [isOpen, session, initialDate]);

  // 유효성 검사
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!date) {
      newErrors.date = '날짜를 선택해주세요';
    }
    if (!time) {
      newErrors.time = '시간을 선택해주세요';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // 저장
  const handleSave = async () => {
    if (!validate()) return;

    setSubmitting(true);
    try {
      // ISO DateTime 생성
      const scheduledAt = `${date}T${time}:00`;

      const requestData: SessionCreateRequest = {
        title: title || undefined,
        description: description || undefined,
        scheduledAt,
        durationMinutes: durationMinutes ? Number(durationMinutes) : undefined,
        location: location || undefined,
        isOnline,
      };

      if (isEditMode && session) {
        await sessionApi.updateSession(studyId, session.id, requestData);
        showToast?.('세션이 수정되었습니다.', 'success');
      } else {
        await sessionApi.createSession(studyId, requestData);
        showToast?.('세션이 생성되었습니다.', 'success');
      }

      onSuccess?.();
      onClose();
    } catch (err: any) {
      const errorMessage =
        err?.response?.data?.message || '세션 저장에 실패했습니다.';
      showToast?.(errorMessage, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} maxWidth="sm">
      <div className="p-4 sm:p-5">
        {/* 헤더 */}
        <div className="mb-4">
          <h2 className="text-lg font-bold text-gray-900">
            {isEditMode ? '세션 수정' : '새 세션 추가'}
          </h2>
          <p className="text-xs text-gray-500 mt-0.5">
            스터디 일정을 설정하세요
          </p>
        </div>

        {/* 폼 */}
        <div className="space-y-3">
          {/* 제목 */}
          <div className="space-y-1">
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
              <Tag size={14} className="text-blue-500" />
              제목
            </label>
            <Input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="예: 알고리즘 스터디 1회차"
              className="text-sm"
            />
          </div>

          {/* 설명 */}
          <div className="space-y-1">
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
              <AlignLeft size={14} className="text-blue-500" />
              설명
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full p-2.5 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none resize-none"
              rows={2}
              placeholder="세션에 대한 상세 내용을 입력하세요"
            />
          </div>

          {/* 날짜 및 시간 */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
                <CalendarIcon size={14} className="text-blue-500" />
                날짜 *
              </label>
              <DatePicker
                value={date}
                onChange={(d) => setDate(d)}
              />
              {errors.date && (
                <p className="text-xs text-red-500">{errors.date}</p>
              )}
            </div>
            <div className="space-y-1">
              <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
                <Clock size={14} className="text-blue-500" />
                시간 *
              </label>
              <TimePicker
                value={time}
                onChange={(t) => setTime(t)}
              />
              {errors.time && (
                <p className="text-xs text-red-500">{errors.time}</p>
              )}
            </div>
          </div>

          {/* 진행 시간 */}
          <div className="space-y-1">
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
              <Clock size={14} className="text-gray-400" />
              진행 시간 (분)
            </label>
            <Input
              type="number"
              value={durationMinutes}
              onChange={(e) =>
                setDurationMinutes(e.target.value ? Number(e.target.value) : '')
              }
              placeholder="60"
              min={1}
              className="text-sm"
            />
          </div>

          {/* 온/오프라인 선택 */}
          <div className="space-y-1">
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
              <Globe size={14} className="text-blue-500" />
              진행 방식
            </label>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setIsOnline(true)}
                className={`flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border-2 text-sm transition-all ${
                  isOnline
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                <Monitor size={16} />
                온라인
              </button>
              <button
                type="button"
                onClick={() => setIsOnline(false)}
                className={`flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border-2 text-sm transition-all ${
                  !isOnline
                    ? 'border-blue-500 bg-blue-50 text-blue-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'
                }`}
              >
                <MapPin size={16} />
                오프라인
              </button>
            </div>
          </div>

          {/* 장소 (오프라인일 때) */}
          {!isOnline && (
            <div className="space-y-1">
              <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
                <MapPin size={14} className="text-blue-500" />
                장소
              </label>
              <Input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="모임 장소를 입력하세요"
                className="text-sm"
              />
            </div>
          )}
        </div>

        {/* 버튼 */}
        <div className="flex items-center justify-end gap-2 mt-5 pt-3 border-t">
          <Button
            variant="outline"
            size="sm"
            onClick={onClose}
            disabled={submitting}
          >
            취소
          </Button>
          <Button
            variant="primary"
            size="sm"
            onClick={handleSave}
            isLoading={submitting}
          >
            {isEditMode ? '수정' : '생성'}
          </Button>
        </div>
      </div>
    </Modal>
  );
};
