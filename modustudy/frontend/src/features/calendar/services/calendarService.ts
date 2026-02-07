import { 
    calendarApi, 
    PersonalScheduleDTO, 
    StudySessionDTO, 
    GoogleCalendarEventDTO,
    CreatePersonalScheduleRequest,
    UpdatePersonalScheduleRequest
} from '@/api/endpoints/calendarApi';
import { UnifiedSchedule, ScheduleSource } from '../types';
import { formatDate } from '../utils';

// Google Calendar API 상태 타입
interface GoogleCalendarStatus {
    connected: boolean;
    email?: string;
    hasValidToken?: boolean;
    calendarId?: string;
    tokenExpiresAt?: string;
}

/**
 * 캘린더 서비스
 * - 여러 소스(개인, 스터디, Google)의 데이터를 통합하여 UnifiedSchedule로 변환
 * - 비즈니스 로직 처리
 */
class CalendarService {
    /**
     * 개인 일정 DTO를 UnifiedSchedule로 변환
     */
    private mapPersonalScheduleToUnified(dto: PersonalScheduleDTO): UnifiedSchedule {
        return {
            id: dto.id,
            title: dto.title,
            description: dto.description,
            startDate: dto.startDate,
            startTime: dto.startTime,
            endDate: dto.endDate,
            endTime: dto.endTime,
            location: dto.location,
            isOnline: dto.isOnline,
            source: 'personal',
            color: dto.color,
            createdAt: dto.createdAt,
            updatedAt: dto.updatedAt
        };
    }

    /**
     * 스터디 세션 DTO를 UnifiedSchedule로 변환
     */
    private mapStudySessionToUnified(dto: StudySessionDTO): UnifiedSchedule {
        const scheduledDate = new Date(dto.scheduledAt);
        const startDate = formatDate(scheduledDate);
        const startTime = scheduledDate.toTimeString().substring(0, 5); // HH:mm

        return {
            id: `study-${dto.id}`,
            title: dto.title,
            description: dto.description,
            startDate: startDate,
            startTime: startTime,
            durationMinutes: dto.durationMinutes,
            location: dto.location,
            isOnline: dto.isOnline,
            source: 'study',
            status: dto.status,
            studyId: dto.studyId,
            sessionNumber: dto.sessionNumber,
            createdAt: dto.createdAt
        };
    }

    /**
     * Google Calendar 이벤트를 UnifiedSchedule로 변환
     */
    private mapGoogleEventToUnified(dto: GoogleCalendarEventDTO): UnifiedSchedule {
        // Google Calendar는 종일 이벤트(date) 또는 시간 지정 이벤트(dateTime) 구분
        const startDate = dto.start.date || dto.start.dateTime?.split('T')[0] || '';
        const startTime = dto.start.dateTime ? new Date(dto.start.dateTime).toTimeString().substring(0, 5) : undefined;
        const endDate = dto.end.date || dto.end.dateTime?.split('T')[0];
        const endTime = dto.end.dateTime ? new Date(dto.end.dateTime).toTimeString().substring(0, 5) : undefined;

        return {
            id: `google-${dto.id}`,
            title: dto.summary,
            description: dto.description,
            startDate: startDate,
            startTime: startTime,
            endDate: endDate,
            endTime: endTime,
            location: dto.location,
            source: 'google',
            googleEventId: dto.id,
            color: '#EA4335' // Google 빨강
        };
    }

    /**
     * 날짜 범위의 모든 일정 조회 (통합)
     */
    async getAllSchedules(startDate: string, endDate: string): Promise<UnifiedSchedule[]> {
        try {
            // 백엔드가 통합 API를 제공하는 경우
            const data = await calendarApi.getAllSchedules(startDate, endDate);

            const personalSchedules = data.personal.map(dto => this.mapPersonalScheduleToUnified(dto));
            const studySessions = data.studySessions.map(dto => this.mapStudySessionToUnified(dto));
            const googleEvents = data.googleEvents.map(dto => this.mapGoogleEventToUnified(dto));

            return [...personalSchedules, ...studySessions, ...googleEvents];
        } catch (error) {
            // 통합 API가 없으면 개별 호출
            return this.getAllSchedulesSeparately(startDate, endDate);
        }
    }

    /**
     * 개별 API 호출로 일정 조회 (통합 API가 없을 경우)
     */
    private async getAllSchedulesSeparately(startDate: string, endDate: string): Promise<UnifiedSchedule[]> {
        const [personal, studySessions] = await Promise.allSettled([
            calendarApi.getPersonalSchedules(startDate, endDate),
            calendarApi.getMyStudySessions(startDate, endDate)
        ]);

        const schedules: UnifiedSchedule[] = [];

        if (personal.status === 'fulfilled') {
            schedules.push(...personal.value.map(dto => this.mapPersonalScheduleToUnified(dto)));
        }

        if (studySessions.status === 'fulfilled') {
            schedules.push(...studySessions.value.map((dto: StudySessionDTO) => this.mapStudySessionToUnified(dto)));
        }

        // Google Calendar 이벤트 조회 (연동된 경우에만)
        try {
            const googleStatus = await calendarApi.getGoogleCalendarStatus();
            if (googleStatus.connected) {
                // 날짜를 ISO DateTime 형식으로 변환
                const startTime = new Date(startDate + 'T00:00:00').toISOString();
                const endTime = new Date(endDate + 'T23:59:59').toISOString();
                const googleEvents = await calendarApi.getGoogleCalendarEvents(startTime, endTime);
                schedules.push(...googleEvents.map(dto => this.mapGoogleEventToUnified(dto)));
            }
        } catch (error) {
        }

        return schedules;
    }

    /**
     * 개인 일정 생성
     */
    async createPersonalSchedule(request: CreatePersonalScheduleRequest): Promise<UnifiedSchedule> {
        const dto = await calendarApi.createPersonalSchedule(request);
        return this.mapPersonalScheduleToUnified(dto);
    }

    /**
     * 개인 일정 수정
     */
    async updatePersonalSchedule(scheduleId: number, request: UpdatePersonalScheduleRequest): Promise<UnifiedSchedule> {
        const dto = await calendarApi.updatePersonalSchedule(scheduleId, request);
        return this.mapPersonalScheduleToUnified(dto);
    }

    /**
     * 개인 일정 삭제
     */
    async deletePersonalSchedule(scheduleId: number): Promise<void> {
        await calendarApi.deletePersonalSchedule(scheduleId);
    }

    /**
     * 소스별 일정 필터링
     */
    filterBySource(schedules: UnifiedSchedule[], sources: ScheduleSource[]): UnifiedSchedule[] {
        return schedules.filter(schedule => sources.includes(schedule.source));
    }

    /**
     * 날짜별 일정 그룹화
     */
    groupByDate(schedules: UnifiedSchedule[]): Record<string, UnifiedSchedule[]> {
        return schedules.reduce((acc, schedule) => {
            const date = schedule.startDate;
            if (!acc[date]) {
                acc[date] = [];
            }
            acc[date].push(schedule);
            return acc;
        }, {} as Record<string, UnifiedSchedule[]>);
    }

    /**
     * 특정 날짜의 일정 조회
     */
    getSchedulesForDate(schedules: UnifiedSchedule[], date: string): UnifiedSchedule[] {
        return schedules.filter(schedule => schedule.startDate === date);
    }

    /**
     * Google Calendar 상태 조회
     */
    async getGoogleCalendarStatus(): Promise<GoogleCalendarStatus> {
        return await calendarApi.getGoogleCalendarStatus();
    }

    /**
     * Google Calendar 연동
     */
    async connectGoogleCalendar(): Promise<string> {
        return await calendarApi.getGoogleAuthUrl();
    }

    /**
     * Google Calendar 연동 해제
     */
    async disconnectGoogleCalendar(): Promise<void> {
        await calendarApi.disconnectGoogleCalendar();
    }

    /**
     * Google Calendar 동기화 트리거 (Watch 등록)
     */
    async triggerGoogleSync(): Promise<void> {
        await calendarApi.syncGoogleCalendar();
    }

    /**
     * Google Calendar 이벤트 조회
     */
    async getGoogleCalendarEvents(startDate: string, endDate: string): Promise<UnifiedSchedule[]> {
        const startTime = new Date(startDate + 'T00:00:00').toISOString();
        const endTime = new Date(endDate + 'T23:59:59').toISOString();
        const events = await calendarApi.getGoogleCalendarEvents(startTime, endTime);
        return events.map(dto => this.mapGoogleEventToUnified(dto));
    }

    /**
     * 월별 일정 개수 조회
     */
    getMonthlyScheduleCount(schedules: UnifiedSchedule[], year: number, month: number): number {
        const monthStr = `${year}-${String(month + 1).padStart(2, '0')}`;
        return schedules.filter(s => s.startDate.startsWith(monthStr)).length;
    }
}

export const calendarService = new CalendarService();
export default calendarService;
