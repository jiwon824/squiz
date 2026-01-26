import { useState } from 'react';
import { Calendar, ScheduleModal, ScheduleDetailModal, GoogleCalendarSync, DateScheduleListModal } from '@/features/calendar/components';
import { useCalendarData } from '@/features/calendar/hooks';
import { UnifiedSchedule } from '@/features/calendar/types';
import { Button } from '@/shared/components';
import { Plus, Settings, ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * 캘린더 메인 페이지
 * - 통합 캘린더 (개인 일정 + 스터디 세션 + Google Calendar)
 */
export const CalendarPage = () => {
    const [currentDate, setCurrentDate] = useState(new Date());

    // 데이터 페칭
    const { schedules, loading, error } = useCalendarData(currentDate);

    // 모달 상태
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isDateListModalOpen, setIsDateListModalOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);

    // 선택된 일정
    const [selectedSchedule, setSelectedSchedule] = useState<UnifiedSchedule | null>(null);
    const [selectedDate, setSelectedDate] = useState<string | undefined>(undefined);

    // 월 변경
    const handlePrevMonth = () => {
        setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
    };

    const handleNextMonth = () => {
        setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
    };

    const handleToday = () => {
        setCurrentDate(new Date());
    };

    // 일정 클릭 핸들러
    const handleScheduleClick = (schedule: UnifiedSchedule) => {
        setSelectedSchedule(schedule);
        setIsDetailModalOpen(true);
    };

    // 날짜 클릭 핸들러 (날짜별 일정 리스트 모달)
    const handleDateClick = (date: string) => {
        setSelectedDate(date);
        setIsDateListModalOpen(true);
    };

    // Quick Add 핸들러 (+ 버튼)
    const handleQuickAdd = (date: string) => {
        setSelectedDate(date);
        setSelectedSchedule(null);
        setIsAddModalOpen(true);
    };

    // 일정 추가 버튼
    const handleAddClick = () => {
        setSelectedDate(undefined);
        setSelectedSchedule(null);
        setIsAddModalOpen(true);
    };

    // 일정 수정 핸들러 (상세 모달에서 호출)
    const handleEditFromDetail = (schedule: UnifiedSchedule) => {
        setSelectedSchedule(schedule);
        setIsDetailModalOpen(false);
        setIsAddModalOpen(true);
    };

    // 모달 닫기 핸들러
    const handleAddModalClose = () => {
        setIsAddModalOpen(false);
        setSelectedSchedule(null);
        setSelectedDate(undefined);
    };

    const handleDetailModalClose = () => {
        setIsDetailModalOpen(false);
        setSelectedSchedule(null);
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* 헤더 */}
            <header className="bg-white border-b sticky top-0 z-10">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex items-center justify-between h-16">
                        <div>
                            <h1 className="text-2xl font-bold text-gray-900">캘린더</h1>
                            <p className="text-sm text-gray-500">
                                모든 일정을 한눈에 관리하세요
                            </p>
                        </div>
                        <div className="flex items-center gap-3">
                            <Button
                                variant="outline"
                                leftIcon={<Settings size={18} />}
                                onClick={() => setIsSettingsOpen(!isSettingsOpen)}
                            >
                                설정
                            </Button>
                            <Button
                                variant="primary"
                                leftIcon={<Plus size={18} />}
                                onClick={handleAddClick}
                            >
                                일정 추가
                            </Button>
                        </div>
                    </div>
                </div>
            </header>

            {/* 메인 컨텐츠 */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                <div className="flex gap-6">
                    {/* 캘린더 영역 */}
                    <div className="flex-1">
                        {error && (
                            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
                                <p className="text-sm text-red-800">
                                    ⚠️ 일정을 불러오는 중 오류가 발생했습니다.
                                </p>
                            </div>
                        )}

                        <div className="bg-white rounded-xl shadow-sm border overflow-hidden">
                            {/* 캘린더 헤더 (월 변경) */}
                            <div className="flex items-center justify-between p-4 border-b">
                                <div className="flex items-center gap-2">
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={handlePrevMonth}
                                        leftIcon={<ChevronLeft size={20} />}
                                    />
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={handleNextMonth}
                                        leftIcon={<ChevronRight size={20} />}
                                    />
                                </div>
                                <h2 className="text-lg font-bold text-gray-900">
                                    {currentDate.getFullYear()}년 {currentDate.getMonth() + 1}월
                                </h2>
                                <Button
                                    variant="google-ghost"
                                    size="sm"
                                    onClick={handleToday}
                                >
                                    오늘
                                </Button>
                            </div>

                            <Calendar
                                currentDate={currentDate}
                                schedules={schedules}
                                onEventClick={handleScheduleClick}
                                onDateClick={handleDateClick}
                                onQuickAdd={handleQuickAdd}
                                loading={loading}
                            />
                        </div>
                    </div>

                    {/* 사이드바 (설정 패널) */}
                    {isSettingsOpen && (
                        <div className="w-80 flex-shrink-0">
                            <GoogleCalendarSync />
                        </div>
                    )}
                </div>
            </main>

            {/* 일정 추가/편집 모달 */}
            <ScheduleModal
                isOpen={isAddModalOpen}
                onClose={handleAddModalClose}
                schedule={selectedSchedule}
                initialDate={selectedDate}
            />

            {/* 일정 상세 모달 */}
            <ScheduleDetailModal
                isOpen={isDetailModalOpen}
                onClose={handleDetailModalClose}
                schedule={selectedSchedule}
                onEdit={handleEditFromDetail}
            />

            {/* 날짜별 일정 리스트 모달 */}
            <DateScheduleListModal
                isOpen={isDateListModalOpen}
                onClose={() => setIsDateListModalOpen(false)}
                date={selectedDate || new Date().toISOString().split('T')[0]}
                schedules={schedules.filter(s => s.startDate === selectedDate)}
                onScheduleClick={handleScheduleClick}
                onAddClick={() => {
                    setIsAddModalOpen(true);
                }}
            />
        </div>
    );
};
