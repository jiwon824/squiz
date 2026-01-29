import { useState, useEffect, useRef } from 'react';
import { Modal, Button, Input } from '@/shared/components';
import { sessionApi, type StudySessionResponse, type SessionCreateRequest } from '@/api/endpoints/sessionApi';
import { useUIStore } from '@/store/uiStore';
import { cn } from '@/shared/utils/cn';
import {
  Tag,
  AlignLeft,
  Calendar as CalendarIcon,
  Clock,
  MapPin,
  Globe,
  Monitor,
} from 'lucide-react';

/**
 * 세션 모달 전용 네이티브 날짜 선택기
 * Modal의 overflow-hidden 문제를 피하기 위해 네이티브 input 사용
 */
const SessionDatePicker: React.FC<{
  value: string;
  onChange: (date: string) => void;
  disabled?: boolean;
}> = ({ value, onChange, disabled }) => {
  return (
    <div className="relative w-full">
      <input
        type="date"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        className={cn(
          'w-full px-3.5 py-2.5 h-[50px] bg-gray-50 border border-gray-200 rounded-2xl',
          'text-sm font-bold text-gray-800 cursor-pointer',
          'transition-all hover:bg-white hover:border-blue-300',
          'focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 focus:bg-white outline-none',
          'disabled:bg-gray-100 disabled:cursor-not-allowed disabled:opacity-60',
          '[&::-webkit-calendar-picker-indicator]:cursor-pointer',
          '[&::-webkit-calendar-picker-indicator]:text-gray-400',
          '[&::-webkit-calendar-picker-indicator]:hover:text-blue-500'
        )}
      />
    </div>
  );
};

/**
 * 휠 컬럼 컴포넌트 - 스크롤로 숫자 선택
 */
const WheelColumn: React.FC<{
  label: string;
  values: number[];
  selected: number;
  onChange: (value: number) => void;
}> = ({ label, values, selected, onChange }) => {
  const containerRef = useRef<HTMLDivElement>(null);

  // 휠 이벤트 리스너 - passive: false로 등록해야 preventDefault 가능
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const handleWheel = (e: WheelEvent) => {
      e.preventDefault();
      const currentIndex = values.indexOf(selected);
      if (e.deltaY > 0) {
        // 아래로 스크롤 - 다음 값
        const nextIndex = Math.min(currentIndex + 1, values.length - 1);
        onChange(values[nextIndex]);
      } else {
        // 위로 스크롤 - 이전 값
        const prevIndex = Math.max(currentIndex - 1, 0);
        onChange(values[prevIndex]);
      }
    };

    container.addEventListener('wheel', handleWheel, { passive: false });
    return () => container.removeEventListener('wheel', handleWheel);
  }, [values, selected, onChange]);

  return (
    <div className="flex-1 flex flex-col items-center">
      <div className="text-[10px] font-bold text-gray-400 mb-1">{label}</div>
      <div
        ref={containerRef}
        className="relative h-[100px] w-full flex flex-col items-center justify-center cursor-ns-resize select-none"
      >
        {/* 이전 값 (흐리게) */}
        <div className="text-gray-300 text-sm h-7 flex items-center">
          {values[values.indexOf(selected) - 1] !== undefined
            ? String(values[values.indexOf(selected) - 1]).padStart(2, '0')
            : ''}
        </div>

        {/* 현재 선택된 값 */}
        <div className="bg-blue-500 text-white font-bold text-lg rounded-lg px-4 py-1 min-w-[50px] text-center">
          {String(selected).padStart(2, '0')}
        </div>

        {/* 다음 값 (흐리게) */}
        <div className="text-gray-300 text-sm h-7 flex items-center">
          {values[values.indexOf(selected) + 1] !== undefined
            ? String(values[values.indexOf(selected) + 1]).padStart(2, '0')
            : ''}
        </div>
      </div>
    </div>
  );
};

/**
 * 세션 모달 전용 휠 스타일 시간 선택기
 */
const SessionTimePicker: React.FC<{
  value: string;
  onChange: (time: string) => void;
  disabled?: boolean;
}> = ({ value, onChange, disabled }) => {
  const [isOpen, setIsOpen] = useState(false);

  // 시간과 분 파싱
  const [hour, minute] = value ? value.split(':').map(Number) : [19, 0];

  // 시간/분 배열 생성
  const hours = Array.from({ length: 24 }, (_, i) => i);
  const minutes = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55];

  // 분 값이 배열에 없으면 가장 가까운 값으로
  const closestMinute = minutes.reduce((prev, curr) =>
    Math.abs(curr - minute) < Math.abs(prev - minute) ? curr : prev
  );

  const handleHourChange = (h: number) => {
    const newTime = `${String(h).padStart(2, '0')}:${String(closestMinute).padStart(2, '0')}`;
    onChange(newTime);
  };

  const handleMinuteChange = (m: number) => {
    const newTime = `${String(hour).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    onChange(newTime);
  };

  const formatTime = (h: number, m: number) => {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
  };

  if (disabled) {
    return (
      <div className="w-full px-3.5 py-2.5 h-[50px] bg-gray-100 border border-gray-200 rounded-2xl flex items-center text-sm font-bold text-gray-500 cursor-not-allowed opacity-60">
        <Clock size={16} className="mr-2 text-gray-400" />
        {formatTime(hour, closestMinute)}
      </div>
    );
  }

  return (
    <div className="relative w-full">
      {/* 트리거 버튼 */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className={cn(
          'w-full px-3.5 py-2.5 h-[50px] bg-gray-50 border border-gray-200 rounded-2xl',
          'flex items-center text-sm font-bold text-gray-800 cursor-pointer',
          'transition-all hover:bg-white hover:border-blue-300',
          isOpen && 'ring-2 ring-blue-500/20 border-blue-500 bg-white'
        )}
      >
        <Clock size={16} className={cn('mr-2 transition-colors', isOpen ? 'text-blue-500' : 'text-gray-400')} />
        {formatTime(hour, closestMinute)}
      </button>

      {/* 휠 선택기 드롭다운 */}
      {isOpen && (
        <>
          {/* 배경 클릭시 닫기 */}
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)} />

          {/* 드롭다운 */}
          <div className="absolute top-full left-0 mt-1 z-50 bg-white border border-gray-200 rounded-xl shadow-lg p-3 w-full">
            <div className="flex">
              {/* 시간 휠 */}
              <WheelColumn
                label="시"
                values={hours}
                selected={hour}
                onChange={handleHourChange}
              />

              {/* 구분선 */}
              <div className="w-px bg-gray-200 mx-2" />

              {/* 분 휠 */}
              <WheelColumn
                label="분"
                values={minutes}
                selected={closestMinute}
                onChange={handleMinuteChange}
              />
            </div>

            {/* 확인 버튼 */}
            <button
              type="button"
              onClick={() => setIsOpen(false)}
              className="w-full mt-3 py-2 bg-blue-500 text-white text-sm font-bold rounded-lg hover:bg-blue-600 transition-colors"
            >
              확인
            </button>
          </div>
        </>
      )}
    </div>
  );
};

interface SessionModalProps {
  isOpen: boolean;
  onClose: () => void;
  studyId: number;
  session?: StudySessionResponse | null;
  initialDate?: string;
  onSuccess?: () => void;
  isLeader?: boolean;
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
  isLeader = true,
}) => {
  const showToast = useUIStore((state) => state.showToast);
  const isEditMode = !!session;
  const isReadOnly = isEditMode && !isLeader; // 수정 모드이면서 리더가 아니면 읽기 전용

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
        const today = new Date().toISOString().split('T')[0];
        const selectedDate = initialDate || today;
        setDate(selectedDate);

        // 오늘 날짜면 현재 시간(5분 단위 올림), 다른 날짜면 00:00
        if (selectedDate === today) {
          const now = new Date();
          const minutes = now.getMinutes();
          const roundedMinutes = Math.ceil(minutes / 5) * 5;
          now.setMinutes(roundedMinutes);
          now.setSeconds(0);
          if (roundedMinutes >= 60) {
            now.setHours(now.getHours() + 1);
            now.setMinutes(0);
          }
          const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
          setTime(currentTime);
        } else {
          setTime('00:00');
        }
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
      {/* overflow-visible 스타일로 DatePicker 드롭다운 표시 */}
      <div className="p-4 sm:p-5" style={{ overflow: 'visible' }}>
        {/* 헤더 */}
        <div className="mb-4">
          <h2 className="text-lg font-bold text-gray-900">
            {isReadOnly ? '세션 상세' : isEditMode ? '세션 수정' : '새 세션 추가'}
          </h2>
          <p className="text-xs text-gray-500 mt-0.5">
            {isReadOnly ? '세션 정보를 확인하세요' : '스터디 일정을 설정하세요'}
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
              disabled={isReadOnly}
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
              className="w-full p-2.5 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all outline-none resize-none disabled:bg-gray-100 disabled:cursor-not-allowed"
              rows={2}
              placeholder="세션에 대한 상세 내용을 입력하세요"
              disabled={isReadOnly}
            />
          </div>

          {/* 날짜 및 시간 */}
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
                <CalendarIcon size={14} className="text-blue-500" />
                날짜 *
              </label>
              <SessionDatePicker
                value={date}
                onChange={(d: string) => setDate(d)}
                disabled={isReadOnly}
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
              <SessionTimePicker
                value={time}
                onChange={(t: string) => setTime(t)}
                disabled={isReadOnly}
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
              disabled={isReadOnly}
            />
          </div>

          {/* 온/오프라인 선택 */}
          <div className="space-y-1">
            <label className="flex items-center gap-1.5 text-xs font-semibold text-gray-700">
              <Globe size={14} className="text-blue-500" />
              진행 방식
            </label>
            <div className={cn('flex gap-2', isReadOnly && 'pointer-events-none opacity-60')}>
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
                disabled={isReadOnly}
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
            {isReadOnly ? '닫기' : '취소'}
          </Button>
          {!isReadOnly && (
            <Button
              variant="primary"
              size="sm"
              onClick={handleSave}
              isLoading={submitting}
            >
              {isEditMode ? '수정' : '생성'}
            </Button>
          )}
        </div>
      </div>
    </Modal>
  );
};
