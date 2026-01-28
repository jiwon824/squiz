import { useState } from 'react';
import { UserLayoutV2 } from '@/layouts/UserLayoutV2';
import { Calendar } from './Calendar';
import { ScheduleModal } from './ScheduleModal';
import { ScheduleDetailModal } from './ScheduleDetailModal';
import { DateScheduleListModal } from './DateScheduleListModal';
import { UnifiedSchedule } from '../types';
import { Button } from '@/shared/components';
import { ChevronLeft, ChevronRight } from 'lucide-react';

/**
 * 캘린더 컴포넌트 테스트 페이지
 */
export const CalendarTestPage = () => {
    const [currentDate, setCurrentDate] = useState(new Date());

    // 모달 상태
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isDateListModalOpen, setIsDateListModalOpen] = useState(false);

    // 선택된 데이터
    const [selectedSchedule, setSelectedSchedule] = useState<UnifiedSchedule | null>(null);
    const [selectedDate, setSelectedDate] = useState<string | undefined>(undefined);

    // Mock 데이터
    const mockSchedules: UnifiedSchedule[] = [
        {
            id: 1,
            title: '알고리즘 스터디',
            description: '백준 문제 풀이',
            startDate: '2026-01-26',
            startTime: '14:00',
            durationMinutes: 120,
            location: '온라인',
            isOnline: true,
            source: 'study',
            status: 'SCHEDULED',
            studyId: 1,
            sessionNumber: 5
        },
        {
            id: 2,
            title: '프로젝트 회의',
            startDate: '2026-01-27',
            startTime: '10:00',
            durationMinutes: 60,
            source: 'personal'
        },
        {
            id: 3,
            title: 'React 세미나',
            startDate: '2026-01-28',
            startTime: '15:00',
            source: 'google',
            googleEventId: 'google-123'
        },
        {
            id: 4,
            title: 'TypeScript 강의',
            startDate: '2026-01-26',
            startTime: '09:00',
            source: 'study',
            studyId: 2
        },
        {
            id: 5,
            title: '개인 학습',
            startDate: '2026-01-29',
            source: 'personal'
        }
    ];

    // 월 이동
    const handlePrevMonth = () => {
        setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 1));
    };

    const handleNextMonth = () => {
        setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 1));
    };

    // 오늘로 이동
    const handleToday = () => {
        setCurrentDate(new Date());
    };

    // 날짜 클릭 (날짜별 일정 리스트 모달)
    const handleDateClick = (date: string) => {
        setSelectedDate(date);
        setIsDateListModalOpen(true);
    };

    // 빠른 추가 (+ 버튼 클릭)
    const handleQuickAdd = (date: string) => {
        setSelectedDate(date);
        setSelectedSchedule(null);
        setIsAddModalOpen(true);
    };

    // 일정 클릭 (상세 모달)
    const handleEventClick = (schedule: UnifiedSchedule) => {
        setSelectedSchedule(schedule);
        setIsDetailModalOpen(true);
    };

    // 상세 모달에서 수정 버튼 클릭
    const handleEditFromDetail = (schedule: UnifiedSchedule) => {
        setSelectedSchedule(schedule);
        setIsDetailModalOpen(false);
        setIsAddModalOpen(true);
    };

    // 일정 추가 모달 닫기
    const handleAddModalClose = () => {
        setIsAddModalOpen(false);
        setSelectedSchedule(null);
        setSelectedDate(undefined);
    };

    // 해당 날짜의 일정 필터링
    const getSchedulesForDate = (date: string) => {
        return mockSchedules.filter(s => s.startDate === date);
    };

    return (
        <UserLayoutV2>
            <div className="max-w-7xl mx-auto p-6">
                {/* 헤더 */}
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">
                        캘린더 테스트 페이지
                    </h1>
                    <p className="text-gray-600">
                        새로 구현한 캘린더 컴포넌트를 테스트합니다.
                    </p>
                </div>

                {/* 컨트롤 바 */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-3 md:p-4 mb-4 md:mb-6">
                    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                        {/* 날짜 네비게이션 */}
                        <div className="flex items-center justify-between md:justify-start gap-2 md:gap-4 w-full md:w-auto">
                            <Button
                                variant="primary"
                                size="sm"
                                onClick={handleToday}
                            >
                                오늘
                            </Button>
                            <div className="flex items-center flex-1 md:flex-none justify-center gap-2">
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={handlePrevMonth}
                                    leftIcon={<ChevronLeft size={20} />}
                                />
                                <span className="text-lg md:text-xl font-bold text-gray-900 min-w-[140px] md:min-w-[200px] text-center">
                                    {currentDate.getFullYear()}년 {currentDate.getMonth() + 1}월
                                </span>
                                <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={handleNextMonth}
                                    leftIcon={<ChevronRight size={20} />}
                                />
                            </div>
                        </div>

                        {/* 범례 (Legend) */}
                        <div className="flex items-center justify-end gap-3 md:gap-4 overflow-x-auto pb-1 md:pb-0">
                            <div className="flex items-center gap-1.5 text-xs md:text-sm whitespace-nowrap">
                                <div className="w-2.5 h-2.5 md:w-3 md:h-3 bg-blue-500 rounded-full md:rounded"></div>
                                <span className="text-gray-600">개인</span>
                            </div>
                            <div className="flex items-center gap-1.5 text-xs md:text-sm whitespace-nowrap">
                                <div className="w-2.5 h-2.5 md:w-3 md:h-3 bg-green-500 rounded-full md:rounded"></div>
                                <span className="text-gray-600">스터디</span>
                            </div>
                            <div className="flex items-center gap-1.5 text-xs md:text-sm whitespace-nowrap">
                                <div className="w-2.5 h-2.5 md:w-3 md:h-3 bg-red-500 rounded-full md:rounded"></div>
                                <span className="text-gray-600">Google</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 캘린더 */}
                <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
                    <Calendar
                        currentDate={currentDate}
                        schedules={mockSchedules}
                        onDateClick={handleDateClick}
                        onQuickAdd={handleQuickAdd}
                        onEventClick={handleEventClick}
                        viewMode="monthly"
                    />
                </div>

                {/* Mock 데이터 표시 */}
                <div className="mt-6 bg-gray-50 rounded-2xl p-6">
                    <h3 className="text-sm font-bold text-gray-700 mb-3">
                        테스트 데이터 ({mockSchedules.length}개)
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                        {mockSchedules.map(schedule => (
                            <div
                                key={schedule.id}
                                className="bg-white rounded-lg p-3 border border-gray-200 cursor-pointer hover:bg-gray-50 transition-colors"
                                onClick={() => handleEventClick(schedule)}
                            >
                                <div className="flex items-start gap-2">
                                    <div className={`w-2 h-2 rounded-full mt-1.5 ${schedule.source === 'personal' ? 'bg-blue-500' :
                                        schedule.source === 'study' ? 'bg-green-500' : 'bg-red-500'
                                        }`}></div>
                                    <div>
                                        <p className="font-medium text-sm">{schedule.title}</p>
                                        <p className="text-xs text-gray-500">{schedule.startDate}</p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

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
                onClose={() => setIsDetailModalOpen(false)}
                schedule={selectedSchedule}
                onEdit={handleEditFromDetail}
            />

            {/* 날짜별 일정 리스트 모달 */}
            <DateScheduleListModal
                isOpen={isDateListModalOpen}
                onClose={() => setIsDateListModalOpen(false)}
                date={selectedDate || new Date().toISOString().split('T')[0]}
                schedules={selectedDate ? getSchedulesForDate(selectedDate) : []}
                onScheduleClick={(schedule) => {
                    setIsDateListModalOpen(false);
                    setSelectedSchedule(schedule);
                    setIsDetailModalOpen(true);
                }}
                onAddClick={() => {
                    setIsDateListModalOpen(false);
                    setSelectedSchedule(null);
                    setIsAddModalOpen(true);
                }}
            />
        </UserLayoutV2>
    );
};
